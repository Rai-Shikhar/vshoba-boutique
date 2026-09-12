package com.vshoba.boutique.service;

import com.vshoba.boutique.model.Product;
import com.vshoba.boutique.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Startup seed: puts the opening "Collections" onto the shop floor.
 *
 * Runs on every boot, but each product is inserted ONLY if a product with
 * the same name does not already exist - so local H2, the Render Postgres,
 * admin-created products and repeated redeploys never create duplicates.
 */
@Component
public class ProductSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductSeeder.class);

    private final ProductRepository productRepository;

    public ProductSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        seed("Banarasi Silk Saree",
                "Handwoven Banarasi silk saree with a rich gold zari border and a flowing pallu.",
                "8499.00", "Sarees", 12);
        seed("Kanchipuram Silk Saree",
                "Temple-town classic - crisp Kanchipuram silk with a contrasting korvai border.",
                "9900.00", "Sarees", 8);
        seed("Chikankari Cotton Kurti",
                "Feather-light Lucknowi chikankari kurti, perfect for everyday comfort.",
                "1299.00", "Kurtis", 25);
        seed("Ajrakh Block-Print Kurta",
                "Eco-printed Ajrakh kurta in earthy indigo and rust tones.",
                "1899.00", "Kurtis", 15);
        seed("Organza Festive Lehenga",
                "Sheer organza lehenga for festive evenings, pre-stitched with a matching dupatta.",
                "6499.00", "Lehengas", 5);
        seed("Kota Doria Dupatta",
                "Breezy Kota doria dupatta with a soft woven chequered finish.",
                "899.00", "Accessories", 30);
        seed("Cotton Anarkali Set",
                "Flared cotton anarkali with churidar - an easy festive two-piece.",
                "2199.00", "Kurtis", 10);
    }

    private void seed(String name, String description, String price, String category, int stock) {
        if (productRepository.existsByNameIgnoreCase(name)) {
            return;
        }
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setCategory(category);
        product.setStockQuantity(stock);
        product.setActive(true);
        productRepository.save(product);
        log.info("Seeded product: {} ({} ₹{})", name, category, price);
    }
}