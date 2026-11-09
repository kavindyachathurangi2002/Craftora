package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Customer;
import com.craftora.craftora.Entity.Product;
import com.craftora.craftora.Repository.CustomerRepository;
import com.craftora.craftora.Repository.ProductRepository;
import com.craftora.craftora.Service.RatingService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class MarketplaceController {

    private final ProductRepository productRepository;
    private final RatingService ratingService;
    private final CustomerRepository customerRepository;

    public MarketplaceController(
            ProductRepository productRepository,
            RatingService ratingService,
            CustomerRepository customerRepository) {

        this.productRepository = productRepository;
        this.ratingService = ratingService;
        this.customerRepository = customerRepository;
    }

    // =========================================================
    // MARKETPLACE
    // =========================================================

    @GetMapping("/marketplace")
    public String marketplace(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String category,
            @RequestParam(required = false, defaultValue = "") String availability,
            @RequestParam(required = false, defaultValue = "recommended") String sort,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "") String auth,
            Model model) {

        List<Product> products = new ArrayList<>(
                productRepository.findAll()
        );

        String searchKeyword = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        String selectedCategory = category == null
                ? ""
                : category.trim();

        String selectedAvailability = availability == null
                ? ""
                : availability.trim();

        // =====================================================
        // SEARCH
        // Product Name + Artisan Name + Category
        // =====================================================

        if (!searchKeyword.isEmpty()) {

            products.removeIf(product -> {

                String productName = "";

                if (product.getProductName() != null) {
                    productName = product.getProductName()
                            .toLowerCase(Locale.ROOT);
                }

                String productCategory = "";

                if (product.getCategory() != null) {
                    productCategory = product.getCategory()
                            .toLowerCase(Locale.ROOT);
                }

                String artisanName = "";

                if (product.getArtisan() != null
                        && product.getArtisan().getUser() != null
                        && product.getArtisan().getUser().getFullName() != null) {

                    artisanName = product.getArtisan()
                            .getUser()
                            .getFullName()
                            .toLowerCase(Locale.ROOT);
                }

                return !(productName.contains(searchKeyword)
                        || productCategory.contains(searchKeyword)
                        || artisanName.contains(searchKeyword));
            });
        }

        // =====================================================
        // CATEGORY FILTER
        // =====================================================

        if (!selectedCategory.isEmpty()) {

            products.removeIf(product -> {

                if (product.getCategory() == null) {
                    return true;
                }

                return !product.getCategory()
                        .equalsIgnoreCase(selectedCategory);
            });
        }

        // =====================================================
        // MINIMUM PRICE
        // =====================================================

        if (minPrice != null) {

            products.removeIf(product -> {

                if (product.getPrice() == null) {
                    return true;
                }

                return product.getPrice()
                        .compareTo(minPrice) < 0;
            });
        }

        // =====================================================
        // MAXIMUM PRICE
        // =====================================================

        if (maxPrice != null) {

            products.removeIf(product -> {

                if (product.getPrice() == null) {
                    return true;
                }

                return product.getPrice()
                        .compareTo(maxPrice) > 0;
            });
        }

        // =====================================================
        // AVAILABILITY
        // =====================================================

        if ("in-stock".equalsIgnoreCase(selectedAvailability)) {

            products.removeIf(product ->
                    product.getStockQuantity() == null
                            || product.getStockQuantity() <= 0
            );

        } else if ("out-of-stock".equalsIgnoreCase(selectedAvailability)) {

            products.removeIf(product ->
                    product.getStockQuantity() != null
                            && product.getStockQuantity() > 0
            );
        }

        // =====================================================
        // SORT
        // =====================================================

        if ("newest".equalsIgnoreCase(sort)) {

            products.sort(
                    Comparator.comparing(
                            Product::getCreatedAt,
                            Comparator.nullsLast(
                                    Comparator.reverseOrder()
                            )
                    )
            );

        } else if ("price-low".equalsIgnoreCase(sort)) {

            products.sort(
                    Comparator.comparing(
                            Product::getPrice,
                            Comparator.nullsLast(
                                    Comparator.naturalOrder()
                            )
                    )
            );

        } else if ("price-high".equalsIgnoreCase(sort)) {

            products.sort(
                    Comparator.comparing(
                            Product::getPrice,
                            Comparator.nullsLast(
                                    Comparator.naturalOrder()
                            )
                    ).reversed()
            );

        } else if ("name-az".equalsIgnoreCase(sort)) {

            products.sort(
                    Comparator.comparing(
                            Product::getProductName,
                            Comparator.nullsLast(
                                    String.CASE_INSENSITIVE_ORDER
                            )
                    )
            );

        } else if ("name-za".equalsIgnoreCase(sort)) {

            products.sort(
                    Comparator.comparing(
                            Product::getProductName,
                            Comparator.nullsLast(
                                    String.CASE_INSENSITIVE_ORDER
                            )
                    ).reversed()
            );

        } else {

            // =================================================
            // RECOMMENDED
            // Mix products from different artisans
            // =================================================

            products = mixProductsByArtisan(products);
        }

        // =====================================================
        // SEND DATA TO HTML
        // =====================================================

        model.addAttribute("products", products);

        model.addAttribute(
                "keyword",
                keyword == null ? "" : keyword
        );

        model.addAttribute(
                "category",
                selectedCategory
        );

        model.addAttribute(
                "availability",
                selectedAvailability
        );

        model.addAttribute(
                "sort",
                sort
        );

        model.addAttribute(
                "minPrice",
                minPrice
        );

        model.addAttribute(
                "maxPrice",
                maxPrice
        );

        // =====================================================
        // LOGIN / CUSTOMER ACCESS MESSAGE
        // =====================================================

        if ("login".equalsIgnoreCase(auth)) {

            model.addAttribute(
                    "authMessage",
                    "Please sign in or register as a Customer to add products to your cart or buy products."
            );

        }

        return "marketplace";
    }

    // =========================================================
    // ADD TO CART - LOGIN CHECK
    // =========================================================

    @GetMapping("/marketplace/cart/add/{id}")
    public String addToCart(
            @PathVariable("id") Long id,
            HttpSession session) {

        Product product =
                productRepository.findById(id).orElse(null);

        if (product == null) {
            return "redirect:/marketplace";
        }

        // Product must be available
        if (product.getStockQuantity() == null
                || product.getStockQuantity() <= 0) {

            return "redirect:/marketplace";
        }

        // Check whether a user is logged in
        Object userId = session.getAttribute("loggedInUserId");

        if (userId == null) {
            return "redirect:/marketplace?auth=login";
        }

        // Check whether the logged-in user has CUSTOMER role
        Object rolesObject = session.getAttribute("userRoles");

        if (!(rolesObject instanceof List<?>)) {
            return "redirect:/marketplace?auth=login";
        }

        boolean isCustomer = false;

        for (Object role : (List<?>) rolesObject) {

            if (role instanceof String
                    && "CUSTOMER".equalsIgnoreCase((String) role)) {

                isCustomer = true;
                break;
            }
        }

        if (!isCustomer) {
            return "redirect:/marketplace?auth=login";
        }

        return "redirect:/cart/add/" + id;
    }

    // =========================================================
    // BUY NOW - LOGIN CHECK
    // =========================================================

    @GetMapping("/marketplace/buy-now/{id}")
    public String buyNow(
            @PathVariable("id") Long id,
            HttpSession session) {

        Product product =
                productRepository.findById(id).orElse(null);

        if (product == null) {
            return "redirect:/marketplace";
        }

        // Product must be available
        if (product.getStockQuantity() == null
                || product.getStockQuantity() <= 0) {

            return "redirect:/marketplace";
        }

        // Check whether a user is logged in
        Object userId = session.getAttribute("loggedInUserId");

        if (userId == null) {
            return "redirect:/marketplace?auth=login";
        }

        // Check whether the logged-in user has CUSTOMER role
        Object rolesObject = session.getAttribute("userRoles");

        if (!(rolesObject instanceof List<?>)) {
            return "redirect:/marketplace?auth=login";
        }

        boolean isCustomer = false;

        for (Object role : (List<?>) rolesObject) {

            if (role instanceof String
                    && "CUSTOMER".equalsIgnoreCase((String) role)) {

                isCustomer = true;
                break;
            }
        }

        if (!isCustomer) {
            return "redirect:/marketplace?auth=login";
        }

        return "redirect:/cart/buy-now/" + id;
    }

    // =========================================================
    // MIX PRODUCTS BY ARTISAN
    // =========================================================

    private List<Product> mixProductsByArtisan(
            List<Product> products) {

        if (products.size() <= 1) {
            return products;
        }

        /*
         * Keep products separated by artisan.
         *
         * Example:
         *
         * Artisan A -> A1, A2, A3
         * Artisan B -> B1, B2
         * Artisan C -> C1, C2
         *
         * Result:
         *
         * A1, B1, C1, A2, B2, C2, A3
         */

        Map<Long, List<Product>> artisanProducts =
                new LinkedHashMap<>();

        for (Product product : products) {

            Long artisanId = null;

            if (product.getArtisan() != null) {
                artisanId =
                        product.getArtisan().getArtisanId();
            }

            /*
             * If artisan ID is unavailable,
             * use product ID as a unique group.
             */

            if (artisanId == null) {
                artisanId = -product.getProductId();
            }

            artisanProducts
                    .computeIfAbsent(
                            artisanId,
                            key -> new ArrayList<>()
                    )
                    .add(product);
        }

        List<Product> mixedProducts =
                new ArrayList<>();

        boolean added;

        do {

            added = false;

            for (List<Product> artisanList
                    : artisanProducts.values()) {

                if (!artisanList.isEmpty()) {

                    mixedProducts.add(
                            artisanList.remove(0)
                    );

                    added = true;
                }
            }

        } while (added);

        return mixedProducts;
    }

    // =========================================================
    // PRODUCT DETAILS
    // =========================================================

    @GetMapping("/marketplace/product/{id}")
    public String productDetails(
            @PathVariable("id") Long id,
            Model model,
            HttpSession session) {

        Product product =
                productRepository.findById(id).orElse(null);

        if (product == null) {
            return "redirect:/marketplace";
        }

        model.addAttribute("product", product);

        // =====================================================
        // EXISTING RATING INFORMATION
        // =====================================================

        model.addAttribute(
                "ratings",
                ratingService.getRatingsByProduct(id)
        );

        model.addAttribute(
                "reviewCount",
                ratingService.getReviewCount(id)
        );

        model.addAttribute(
                "averageRating",
                ratingService.getAverageRating(id)
        );

        // =====================================================
        // CUSTOMER REVIEW ELIGIBILITY
        // =====================================================

        boolean loggedIn = false;
        boolean canReview = false;
        boolean alreadyReviewed = false;

        Object loggedInUserIdObject =
                session.getAttribute("loggedInUserId");

        if (loggedInUserIdObject != null) {

            loggedIn = true;

            try {

                Long userId =
                        Long.valueOf(
                                loggedInUserIdObject.toString()
                        );

                Customer customer =
                        customerRepository
                                .findByUserUserId(userId)
                                .orElse(null);

                if (customer != null) {

                    alreadyReviewed =
                            ratingService
                                    .getCustomerProductRating(
                                            customer.getCustomerId(),
                                            id
                                    )
                                    .isPresent();

                    boolean purchased =
                            ratingService
                                    .getPurchasedOrderItem(
                                            customer.getCustomerId(),
                                            id
                                    )
                                    .isPresent();

                    canReview =
                            purchased && !alreadyReviewed;
                }

            } catch (Exception ignored) {
                // Keep existing product details page working normally
            }
        }

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("canReview", canReview);
        model.addAttribute("alreadyReviewed", alreadyReviewed);

        return "product-details";
    }
}