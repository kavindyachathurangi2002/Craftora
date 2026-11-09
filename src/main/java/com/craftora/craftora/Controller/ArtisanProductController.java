package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.Product;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.ProductRepository;
import com.craftora.craftora.Repository.UserRepository;
import com.craftora.craftora.Service.EmailService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class ArtisanProductController {

    private final ProductRepository productRepository;
    private final ArtisanRepository artisanRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ArtisanProductController(
            ProductRepository productRepository,
            ArtisanRepository artisanRepository,
            UserRepository userRepository,
            EmailService emailService) {

        this.productRepository = productRepository;
        this.artisanRepository = artisanRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // =========================
    // MY PRODUCTS
    // =========================
    @GetMapping("/artisan/products")
    public String products(
            HttpSession session,
            Model model) {

        Long userId = getLoggedInUserId(session);

        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return "redirect:/login";
        }

        Artisan artisan = artisanRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }

        model.addAttribute("user", user);
        model.addAttribute("artisan", artisan);
        model.addAttribute(
                "products",
                productRepository.findByArtisan(artisan)
        );

        return "artisan-products";
    }

    // =========================
    // ADD PRODUCT PAGE
    // =========================
    @GetMapping("/artisan/products/add")
    public String addProductPage(
            HttpSession session,
            Model model) {

        Long userId = getLoggedInUserId(session);

        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return "redirect:/login";
        }

        Artisan artisan = artisanRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }

        model.addAttribute("user", user);
        model.addAttribute("artisan", artisan);

        return "add-product";
    }

    // =========================
    // SAVE NEW PRODUCT
    // =========================
    @PostMapping("/artisan/products/add/save")
    public String saveProduct(
            @RequestParam("productName") String productName,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam("price") java.math.BigDecimal price,
            @RequestParam("stockQuantity") Integer stockQuantity,
            @RequestParam("productImage") MultipartFile productImage,
            HttpSession session) throws IOException {

        Long userId = getLoggedInUserId(session);

        if (userId == null) {
            return "redirect:/login";
        }

        Artisan artisan = artisanRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }

        // Product image is mandatory
        if (productImage == null || productImage.isEmpty()) {
            return "redirect:/artisan/products/add?error=imageRequired";
        }

        Product product = new Product();

        product.setArtisan(artisan);
        product.setProductName(productName.trim());
        product.setCategory(category.trim());
        product.setDescription(description.trim());
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);

        /*
         * IMPORTANT:
         * New products do NOT send low-stock email,
         * even if stock is 1, 2, 3, 4 or 5.
         */
        product.setLowStockAlertSent(false);

        String imagePath = saveProductImage(productImage);

        product.setProductImage(imagePath);

        productRepository.save(product);

        return "redirect:/artisan/products?success=added";
    }

    // =========================
    // EDIT PRODUCT PAGE
    // =========================
    @GetMapping("/artisan/products/edit/{id}")
    public String editProductPage(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        Long userId = getLoggedInUserId(session);

        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return "redirect:/login";
        }

        Artisan artisan = artisanRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }

        Product product = productRepository
                .findById(id)
                .orElse(null);

        // Security check
        // Only the product owner can edit the product
        if (product == null
                || product.getArtisan() == null
                || product.getArtisan().getArtisanId() == null
                || !product.getArtisan()
                        .getArtisanId()
                        .equals(artisan.getArtisanId())) {

            return "redirect:/artisan/products";
        }

        model.addAttribute("user", user);
        model.addAttribute("artisan", artisan);
        model.addAttribute("product", product);

        return "edit-product";
    }

    // =========================
    // UPDATE PRODUCT
    // =========================
    @PostMapping("/artisan/products/edit/save")
    public String updateProduct(
            @RequestParam("productId") Long productId,
            @RequestParam("productName") String productName,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam("price") java.math.BigDecimal price,
            @RequestParam("stockQuantity") Integer stockQuantity,
            @RequestParam(
                    value = "productImage",
                    required = false
            ) MultipartFile productImage,
            HttpSession session) throws IOException {

        Long userId = getLoggedInUserId(session);

        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return "redirect:/login";
        }

        Artisan artisan = artisanRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }

        Product product = productRepository
                .findById(productId)
                .orElse(null);

        // =========================
        // SECURITY CHECK
        // =========================
        if (product == null
                || product.getArtisan() == null
                || product.getArtisan().getArtisanId() == null
                || !product.getArtisan()
                        .getArtisanId()
                        .equals(artisan.getArtisanId())) {

            return "redirect:/artisan/products";
        }

        // =========================
        // OLD STOCK
        // =========================
        Integer oldStock = product.getStockQuantity();

        System.out.println("----------------------------------------");
        System.out.println("PRODUCT STOCK UPDATE");
        System.out.println("Product: " + product.getProductName());
        System.out.println("Old Stock: " + oldStock);
        System.out.println("New Stock: " + stockQuantity);
        System.out.println(
                "Low Stock Alert Already Sent: "
                + product.isLowStockAlertSent()
        );
        System.out.println(
                "Artisan Email: "
                + user.getEmail()
        );
        System.out.println("----------------------------------------");

        // =========================
        // UPDATE PRODUCT DETAILS
        // =========================
        product.setProductName(productName.trim());
        product.setCategory(category.trim());
        product.setDescription(description.trim());
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);

        // =================================================
        // LOW STOCK ALERT LOGIC
        // =================================================

        /*
         * If artisan increases the stock above 5,
         * reset the alert.
         *
         * Example:
         *
         * 5 -> 10
         * 4 -> 10
         *
         * After this, if stock later becomes 5,
         * another email can be sent.
         */
        if (stockQuantity > 5) {

            product.setLowStockAlertSent(false);

            System.out.println(
                    "Stock is above 5."
            );

            System.out.println(
                    "Low Stock Alert has been RESET."
            );
        }

        /*
         * Send Low Stock Email when:
         *
         * OLD stock was greater than 5
         * AND
         * NEW stock is 5 or below
         * AND
         * alert has not already been sent.
         *
         * Examples:
         *
         * 15 -> 5  = EMAIL
         * 10 -> 5  = EMAIL
         * 6  -> 5  = EMAIL
         * 15 -> 4  = EMAIL
         *
         * But:
         *
         * 5 -> 4 = NO EMAIL
         * 4 -> 3 = NO EMAIL
         * 3 -> 2 = NO EMAIL
         * 2 -> 1 = NO EMAIL
         */
        if (oldStock != null
                && oldStock > 5
                && stockQuantity <= 5
                && !product.isLowStockAlertSent()) {

            System.out.println(
                    ">>> LOW STOCK CONDITION MATCHED <<<"
            );

            System.out.println(
                    ">>> SENDING LOW STOCK EMAIL <<<"
            );

            System.out.println(
                    "Email To: "
                    + user.getEmail()
            );

            emailService.sendLowStockAlertEmail(
                    user.getEmail(),
                    user.getFullName(),
                    product.getProductName(),
                    stockQuantity
            );

            product.setLowStockAlertSent(true);

            System.out.println(
                    ">>> LOW STOCK EMAIL SENT <<<"
            );
        } else {

            System.out.println(
                    ">>> LOW STOCK EMAIL NOT SENT <<<"
            );
        }

        // =========================
        // IMAGE UPDATE
        // =========================

        /*
         * Product image is optional during edit.
         * If no new image is selected,
         * old image remains.
         */
        if (productImage != null
                && !productImage.isEmpty()) {

            String oldImage =
                    product.getProductImage();

            String newImage =
                    saveProductImage(productImage);

            product.setProductImage(newImage);

            deleteProductImage(oldImage);
        }

        // =========================
        // SAVE PRODUCT
        // =========================
        productRepository.save(product);

        return "redirect:/artisan/products?success=updated";
    }

    // =========================
    // DELETE PRODUCT
    // =========================
    @PostMapping("/artisan/products/delete/{id}")
    public String deleteProduct(
            @PathVariable Long id,
            HttpSession session) {

        Long userId = getLoggedInUserId(session);

        if (userId == null) {
            return "redirect:/login";
        }

        Artisan artisan = artisanRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }

        Product product = productRepository
                .findById(id)
                .orElse(null);

        // Security check
        // Only product owner can delete
        if (product == null
                || product.getArtisan() == null
                || product.getArtisan().getArtisanId() == null
                || !product.getArtisan()
                        .getArtisanId()
                        .equals(artisan.getArtisanId())) {

            return "redirect:/artisan/products";
        }

        String imagePath =
                product.getProductImage();

        productRepository.delete(product);

        // Delete product image
        deleteProductImage(imagePath);

        return "redirect:/artisan/products?success=deleted";
    }

    // =========================
    // HELPER: LOGGED USER ID
    // =========================
    private Long getLoggedInUserId(
            HttpSession session) {

        Object userIdObject =
                session.getAttribute(
                        "loggedInUserId"
                );

        if (userIdObject instanceof Long) {

            return (Long) userIdObject;
        }

        if (userIdObject instanceof Integer) {

            return ((Integer) userIdObject)
                    .longValue();
        }

        if (userIdObject instanceof String) {

            try {

                return Long.parseLong(
                        (String) userIdObject
                );

            } catch (NumberFormatException ignored) {

                return null;
            }
        }

        return null;
    }

    // =========================
    // HELPER: SAVE PRODUCT IMAGE
    // =========================
    private String saveProductImage(
            MultipartFile image)
            throws IOException {

        Path uploadDirectory =
                Paths.get("uploads/products");

        if (!Files.exists(uploadDirectory)) {

            Files.createDirectories(
                    uploadDirectory
            );
        }

        String originalName =
                image.getOriginalFilename();

        String extension = "";

        if (originalName != null
                && originalName.contains(".")) {

            extension =
                    originalName.substring(
                            originalName.lastIndexOf(".")
                    );
        }

        String fileName =
                UUID.randomUUID()
                + extension;

        Path filePath =
                uploadDirectory.resolve(
                        fileName
                );

        Files.copy(
                image.getInputStream(),
                filePath
        );

        return "/uploads/products/"
                + fileName;
    }

    // =========================
    // HELPER: DELETE PRODUCT IMAGE
    // =========================
    private void deleteProductImage(
            String imagePath) {

        if (imagePath == null
                || imagePath.isBlank()) {

            return;
        }

        try {

            String cleanPath =
                    imagePath;

            if (cleanPath.startsWith("/")) {

                cleanPath =
                        cleanPath.substring(1);
            }

            if (!cleanPath.startsWith(
                    "uploads/products/"
            )) {

                return;
            }

            Path filePath =
                    Paths.get(cleanPath);

            Files.deleteIfExists(
                    filePath
            );

        } catch (IOException ignored) {

            // Product update/delete should still
            // complete even if old image cannot
            // be deleted.
        }
    }
}