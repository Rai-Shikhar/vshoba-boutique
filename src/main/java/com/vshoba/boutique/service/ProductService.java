package com.vshoba.boutique.service;

import com.vshoba.boutique.exception.BusinessRuleException;
import com.vshoba.boutique.exception.ResourceNotFoundException;
import com.vshoba.boutique.model.Product;
import com.vshoba.boutique.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for inventory. Every rule grandma cares about lives here:
 *   - stock can never be negative
 *   - prices can never be negative
 *   - a product with stock left cannot be deleted (it must be sold or
 *     deactivated instead)
 *
 * @Transactional means: if ANYTHING inside the method throws, the whole
 * method rolls back - the database is never left half-changed.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ---------- Reads (storefront + inventory) ----------

    /**
     * Every product including hidden ones, newest first - for the
     * admin's full inventory view.
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "id"));
    }

    /**
     * Everything shoppers can see.
     */
    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrueOrderByIdDesc();
    }

    /**
     * Any product by id (even hidden ones) - for the admin's inventory view.
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Like getProductById, but refuses to return hidden/deleted products.
     * For the shopper-facing pages.
     */
    public Product getActiveProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " does not exist"));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ResourceNotFoundException("Product with id " + id + " is not available for sale");
        }
        return product;
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCaseAndActiveTrueOrderByIdDesc(category);
    }

    public List<Product> getProductsInPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null || maxPrice == null) {
            throw new BusinessRuleException("Both a minimum and a maximum price are required");
        }
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessRuleException("Minimum price cannot be greater than maximum price");
        }
        return productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice);
    }

    public List<Product> searchProducts(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessRuleException("Search keyword cannot be empty");
        }
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(trimmed, trimmed);
    }

    /**
     * Stock warning: every product at or below the threshold, worst first.
     */
    public List<Product> getLowStockProducts(int threshold) {
        if (threshold < 0) {
            throw new BusinessRuleException("Stock threshold cannot be negative");
        }
        return productRepository.findByStockQuantityLessThanEqualOrderByStockQuantityAsc(threshold);
    }

    /**
     * All categories, alphabetically - powers the shop's category dropdown.
     */
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    // ---------- Writes (admin only in Phase 4) ----------

    @Transactional
    public Product createProduct(Product product) {
        // Build a fresh entity: whatever id the request contained is ignored,
        // and active defaults to true unless explicitly set.
        Product newProduct = new Product();
        newProduct.setName(product.getName());
        newProduct.setDescription(product.getDescription());
        newProduct.setPrice(product.getPrice());
        newProduct.setStockQuantity(product.getStockQuantity() == null ? 0 : product.getStockQuantity());
        newProduct.setCategory(product.getCategory());
        newProduct.setImageUrl(product.getImageUrl());
        newProduct.setActive(Boolean.TRUE.equals(product.getActive()));

        validateProduct(newProduct);
        return productRepository.save(newProduct);
    }

    /**
     * Partial update: only the fields that are NOT null get changed,
     * so the caller can send just {price: 2500} to change the price.
     */
    @Transactional
    public Product updateProduct(Long id, Product updates) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " does not exist"));

        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getPrice() != null) {
            existing.setPrice(updates.getPrice());
        }
        if (updates.getStockQuantity() != null) {
            existing.setStockQuantity(updates.getStockQuantity());
        }
        if (updates.getCategory() != null) {
            existing.setCategory(updates.getCategory());
        }
        if (updates.getImageUrl() != null) {
            existing.setImageUrl(updates.getImageUrl());
        }
        if (updates.getActive() != null) {
            existing.setActive(updates.getActive());
        }

        validateProduct(existing);
        return productRepository.save(existing);
    }

    /**
     * Explicit stock adjustment - the method inventory managers call
     * when boxes arrive. Enforces the "never negative" rule.
     */
    @Transactional
    public Product updateStock(Long id, int newQuantity) {
        if (newQuantity < 0) {
            throw new BusinessRuleException("Stock cannot be negative (requested " + newQuantity + ")");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " does not exist"));
        product.setStockQuantity(newQuantity);
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " does not exist"));
        if (product.getStockQuantity() > 0) {
            throw new BusinessRuleException("Cannot delete a product that still has " + product.getStockQuantity()
                    + " unit(s) in stock. Set stock to 0 or deactivate it instead.");
        }
        productRepository.delete(product);
    }

    // ---------- Internal ----------

    /**
     * Field checks run inside the service, so the rules hold even if
     * someone calls the service directly (not just through the API).
     */
    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new BusinessRuleException("Product name is required");
        }
        if (product.getName().trim().length() > 150) {
            throw new BusinessRuleException("Product name cannot exceed 150 characters");
        }
        if (product.getDescription() == null || product.getDescription().trim().isEmpty()) {
            throw new BusinessRuleException("Product description is required");
        }
        if (product.getPrice() == null) {
            throw new BusinessRuleException("Product price is required");
        }
        if (product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Product price cannot be negative");
        }
        if (product.getStockQuantity() == null || product.getStockQuantity() < 0) {
            throw new BusinessRuleException("Product stock cannot be negative");
        }
    }
}
