package com.vshoba.boutique.controller;

import com.vshoba.boutique.model.Product;
import com.vshoba.boutique.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

/**
 * The boutique's product REST API.
 *
 * Every URL below starts with /api/products. "GET" = read, "POST" = create,
 * "PUT" = update, "PATCH" = partial update, "DELETE" = remove.
 *
 * The controller is deliberately thin: it only translates HTTP requests
 * into service calls. All rules live in ProductService.
 *
 * NOTE on security: today any caller can create/update/delete. A later
 * security phase will protect the write endpoints (POST/PUT/PATCH/DELETE)
 * so only admins can use them. The read endpoints (GET) stay public for
 * the shop.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ---------- Public storefront (anyone) ----------

    /**
     * GET /api/products - everything shoppers can buy, newest first.
     */
    @GetMapping
    public List<Product> getActiveProducts() {
        return productService.getActiveProducts();
    }

    /**
     * GET /api/products/{id} - one product. 404 if missing or hidden.
     */
    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getActiveProductById(id);
    }

    /**
     * GET /api/products/category/{category} - e.g. /category/Sarees.
     */
    @GetMapping("/category/{category}")
    public List<Product> getByCategory(@PathVariable String category) {
        return productService.getProductsByCategory(category);
    }

    /**
     * GET /api/products/search?q=silk - searches name and description.
     */
    @GetMapping("/search")
    public List<Product> search(@RequestParam("q") String keyword) {
        return productService.searchProducts(keyword);
    }

    /**
     * GET /api/products/price-range?min=500&max=2000
     */
    @GetMapping("/price-range")
    public List<Product> getByPriceRange(@RequestParam BigDecimal min,
                                         @RequestParam BigDecimal max) {
        return productService.getProductsInPriceRange(min, max);
    }

    /**
     * GET /api/products/categories - the category dropdown list.
     */
    @GetMapping("/categories")
    public List<String> getCategories() {
        return productService.getCategories();
    }

    // ---------- Inventory (admin) ----------

    /**
     * GET /api/products/all - ALL products, including hidden ones.
     * The full inventory view for the admin panel. Admin-only.
     */
    @GetMapping("/all")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    /**
     * GET /api/products/low-stock?threshold=5 - what needs reordering.
     */
    @GetMapping("/low-stock")
    public List<Product> getLowStock(@RequestParam(defaultValue = "5") int threshold) {
        return productService.getLowStockProducts(threshold);
    }

    /**
     * POST /api/products - add a new product. Body is the product JSON.
     * Example body:
     * {
     *   "name": "Blue Kanchipuram Saree",
     *   "description": "Handwoven silk saree with gold zari border",
     *   "price": 8500.00,
     *   "stockQuantity": 10,
     *   "category": "Sarees",
     *   "imageUrl": "https://example.com/saree1.jpg"
     * }
     *
     * @Valid rejects bad JSON (missing name, negative price, ...)
     * before the service is even called.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@Valid @RequestBody Product product) {
        return productService.createProduct(product);
    }

    /**
     * PUT /api/products/{id} - update a product.
     * Partial updates work: send only the fields you want to change.
     * (No @Valid here on purpose: partial bodies are allowed to omit
     * optional fields, and ProductService re-validates the merged result.)
     */
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }

    /**
     * PATCH /api/products/{id}/stock?quantity=25 - adjust stock level.
     */
    @PatchMapping("/{id}/stock")
    public Product updateStock(@PathVariable Long id, @RequestParam int quantity) {
        return productService.updateStock(id, quantity);
    }

    /**
     * DELETE /api/products/{id} - permanently remove a product.
     * Fails with 400 if the product still has stock.
     * Prefer deactivation (PUT active=false) over deletion!
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}
