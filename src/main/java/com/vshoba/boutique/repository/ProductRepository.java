package com.vshoba.boutique.repository;

import com.vshoba.boutique.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Database access for products.
 *
 * JpaRepository<Product, Long> already gives us, for free:
 *   save(), findById(), findAll(), deleteById(), count(), existsById() ...
 *
 * Every method below is a "derived query": Spring Data JPA reads the method
 * name, translates it into SQL, and writes the implementation at startup.
 * If a name is misspelled or the pattern is invalid, the application
 * FAILS TO START - a built-in safety net that catches bugs early.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Everything a shopper can see: only active products, newest first.
     */
    List<Product> findByActiveTrueOrderByIdDesc();

    /**
     * All products in one category (e.g. "Sarees"), regardless of status.
     * Category matching is case-insensitive so "sarees" and "Sarees" both work.
     */
    List<Product> findByCategoryIgnoreCase(String category);

    /**
     * Active products only, in one category, newest first.
     * Used by the storefront "shop by category" page.
     */
    List<Product> findByCategoryIgnoreCaseAndActiveTrueOrderByIdDesc(String category);

    /**
     * Products within a price range (inclusive). Used by the price filter.
     */
    List<Product> findByPriceBetweenAndActiveTrue(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Products whose name or description contains the given text.
     * "Containing" compiles to LIKE '%text%', so this is the search box.
     */
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String namePart, String descriptionPart);

    /**
     * Low-stock warning for grandma: everything at or below the threshold,
     * ordered by stock ascending so the most critical item is first.
     */
    List<Product> findByStockQuantityLessThanEqualOrderByStockQuantityAsc(int threshold);

    /**
     * True if a product with this exact name (case-insensitive) already exists.
     * Used by the startup seeder so a redeploy never creates duplicates.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * All distinct categories, alphabetically. Powers the category dropdown.
     *
     * This one cannot be expressed as a derived query method name,
     * so we write explicit JPQL (querying the entity, not the table).
     */
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findDistinctCategories();
}
