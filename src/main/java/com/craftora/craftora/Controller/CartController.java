package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.CartItem;
import com.craftora.craftora.Entity.Product;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.CartItemRepository;
import com.craftora.craftora.Repository.ProductRepository;
import com.craftora.craftora.Repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class CartController {

    private final ProductRepository productRepository;
    private final ArtisanRepository artisanRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    public CartController(
            ProductRepository productRepository,
            ArtisanRepository artisanRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository) {

        this.productRepository = productRepository;
        this.artisanRepository = artisanRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
    }

    // =========================================================
    // CART PAGE
    // =========================================================

    @GetMapping("/cart")
    public String cart(
            HttpSession session,
            Model model) {

        if (!canUseCart(session)) {
            return "redirect:/marketplace?auth=login";
        }

        /*
         * Load the logged-in user's cart from the DATABASE.
         * This is what makes the cart available again after
         * logout -> login.
         */
        Map<Long, Integer> cartItems = loadCartFromDatabase(session);

        List<CartItemView> items = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cartItems.entrySet()) {

            Product product = productRepository
                    .findById(entry.getKey())
                    .orElse(null);

            if (product == null) {
                continue;
            }

            /*
             * Artisan cannot keep his/her own product
             * in the cart.
             */
            if (isOwnProduct(session, product)) {
                continue;
            }

            int quantity = entry.getValue();

            if (product.getStockQuantity() == null
                    || product.getStockQuantity() <= 0) {
                continue;
            }

            if (quantity > product.getStockQuantity()) {
                quantity = product.getStockQuantity();
                entry.setValue(quantity);
            }

            BigDecimal subtotal =
                    product.getPrice()
                            .multiply(
                                    BigDecimal.valueOf(quantity)
                            );

            items.add(
                    new CartItemView(
                            product,
                            quantity,
                            subtotal
                    )
            );

            total = total.add(subtotal);
        }

        // Keep the session cart synchronized for CheckoutController.
        session.setAttribute("cartItems", cartItems);

        model.addAttribute("cartItems", items);
        model.addAttribute("cartTotal", total);

        return "cart";
    }

    // =========================================================
    // ADD TO CART
    // =========================================================

    @GetMapping("/cart/add/{id}")
    public String addToCart(
            @PathVariable("id") Long id,
            HttpSession session) {

        if (!canUseCart(session)) {
            return "redirect:/marketplace?auth=login";
        }

        Product product =
                productRepository
                        .findById(id)
                        .orElse(null);

        if (product == null) {
            return "redirect:/marketplace";
        }

        /*
         * Artisan cannot add his/her own product.
         */
        if (isOwnProduct(session, product)) {
            return "redirect:/marketplace?ownProduct=true";
        }

        if (product.getStockQuantity() == null
                || product.getStockQuantity() <= 0) {

            return "redirect:/marketplace";
        }

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        Optional<CartItem> existing =
                cartItemRepository.findByUserAndProduct(user, product);

        if (existing.isPresent()) {

            CartItem cartItem = existing.get();

            int newQuantity =
                    cartItem.getQuantity() + 1;

            if (newQuantity > product.getStockQuantity()) {
                newQuantity = product.getStockQuantity();
            }

            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);

        } else {

            CartItem cartItem = new CartItem();

            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(1);

            cartItemRepository.save(cartItem);
        }

        // Refresh session cart from database.
        loadCartFromDatabase(session);

        return "redirect:/cart";
    }

    // =========================================================
    // BUY NOW
    // =========================================================

    @GetMapping("/cart/buy-now/{id}")
    public String buyNow(
            @PathVariable("id") Long id,
            HttpSession session) {

        if (!canUseCart(session)) {
            return "redirect:/marketplace?auth=login";
        }

        /*
         * Both CUSTOMER and ARTISAN can use Buy Now.
         * An ARTISAN can purchase another artisan's product,
         * but cannot purchase his/her own product.
         */
        if (!canUseCart(session)) {
            return "redirect:/marketplace?auth=login";
        }

        Product product =
                productRepository
                        .findById(id)
                        .orElse(null);

        if (product == null) {
            return "redirect:/marketplace";
        }

        /*
         * Artisan cannot buy his/her own product.
         */
        if (isOwnProduct(session, product)) {
            return "redirect:/marketplace?ownProduct=true";
        }

        if (product.getStockQuantity() == null
                || product.getStockQuantity() <= 0) {

            return "redirect:/marketplace";
        }

        // Buy Now is a direct purchase.
        // It should NOT add the product to the normal cart.
        session.setAttribute(
                "buyNowProductId",
                id
        );

        return "redirect:/checkout";
    }

    // =========================================================
    // INCREASE QUANTITY
    // =========================================================

    @GetMapping("/cart/increase/{id}")
    public String increaseQuantity(
            @PathVariable("id") Long id,
            HttpSession session) {

        if (!canUseCart(session)) {
            return "redirect:/marketplace?auth=login";
        }

        Product product =
                productRepository
                        .findById(id)
                        .orElse(null);

        if (product == null) {
            return "redirect:/cart";
        }

        /*
         * Protect against an artisan keeping his own product
         * in an old/session/database cart.
         */
        if (isOwnProduct(session, product)) {

            removeProductFromDatabaseCart(session, product);

            loadCartFromDatabase(session);

            return "redirect:/cart";
        }

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        Optional<CartItem> existing =
                cartItemRepository.findByUserAndProduct(user, product);

        if (existing.isPresent()) {

            CartItem cartItem = existing.get();

            int quantity = cartItem.getQuantity();

            if (product.getStockQuantity() != null
                    && quantity < product.getStockQuantity()) {

                cartItem.setQuantity(quantity + 1);
                cartItemRepository.save(cartItem);
            }
        }

        loadCartFromDatabase(session);

        return "redirect:/cart";
    }

    // =========================================================
    // DECREASE QUANTITY
    // =========================================================

    @GetMapping("/cart/decrease/{id}")
    public String decreaseQuantity(
            @PathVariable("id") Long id,
            HttpSession session) {

        if (!canUseCart(session)) {
            return "redirect:/marketplace?auth=login";
        }

        Product product =
                productRepository
                        .findById(id)
                        .orElse(null);

        if (product == null) {
            return "redirect:/cart";
        }

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        Optional<CartItem> existing =
                cartItemRepository.findByUserAndProduct(user, product);

        if (existing.isPresent()) {

            CartItem cartItem = existing.get();

            int quantity = cartItem.getQuantity();

            if (quantity <= 1) {
                cartItemRepository.delete(cartItem);
            } else {
                cartItem.setQuantity(quantity - 1);
                cartItemRepository.save(cartItem);
            }
        }

        loadCartFromDatabase(session);

        return "redirect:/cart";
    }

    // =========================================================
    // REMOVE FROM CART
    // =========================================================

    @GetMapping("/cart/remove/{id}")
    public String removeFromCart(
            @PathVariable("id") Long id,
            HttpSession session) {

        if (!canUseCart(session)) {
            return "redirect:/marketplace?auth=login";
        }

        Product product =
                productRepository
                        .findById(id)
                        .orElse(null);

        if (product != null) {

            User user = getLoggedInUser(session);

            if (user != null) {

                Optional<CartItem> existing =
                        cartItemRepository.findByUserAndProduct(
                                user,
                                product
                        );

                existing.ifPresent(cartItemRepository::delete);
            }
        }

        loadCartFromDatabase(session);

        return "redirect:/cart";
    }

    // =========================================================
    // CLEAR CART
    // =========================================================

    @GetMapping("/cart/clear")
    public String clearCart(
            HttpSession session) {

        if (!canUseCart(session)) {
            return "redirect:/marketplace?auth=login";
        }

        User user = getLoggedInUser(session);

        if (user != null) {
            cartItemRepository.deleteByUser(user);
        }

        session.removeAttribute("cartItems");

        return "redirect:/cart";
    }

    // =========================================================
    // CHECK WHETHER USER CAN USE CART
    // =========================================================

    private boolean canUseCart(
            HttpSession session) {

        Object userId =
                session.getAttribute(
                        "loggedInUserId"
                );

        if (userId == null) {
            return false;
        }

        Object rolesObject =
                session.getAttribute(
                        "userRoles"
                );

        if (!(rolesObject instanceof List<?>)) {
            return false;
        }

        for (Object role :
                (List<?>) rolesObject) {

            if (!(role instanceof String)) {
                continue;
            }

            String roleName =
                    ((String) role).trim();

            /*
             * Both CUSTOMER and ARTISAN
             * can use the cart.
             */
            if ("CUSTOMER".equalsIgnoreCase(roleName)
                    || "ARTISAN".equalsIgnoreCase(roleName)) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser(
            HttpSession session) {

        Object userIdObject =
                session.getAttribute(
                        "loggedInUserId"
                );

        if (userIdObject == null) {
            return null;
        }

        Long userId;

        try {
            userId =
                    Long.valueOf(
                            userIdObject.toString()
                    );
        } catch (NumberFormatException e) {
            return null;
        }

        return userRepository
                .findById(userId)
                .orElse(null);
    }

    // =========================================================
    // LOAD CART FROM DATABASE
    // =========================================================

    private Map<Long, Integer> loadCartFromDatabase(
            HttpSession session) {

        Map<Long, Integer> cartItems =
                new HashMap<>();

        User user = getLoggedInUser(session);

        if (user == null) {
            session.setAttribute(
                    "cartItems",
                    cartItems
            );
            return cartItems;
        }

        List<CartItem> databaseItems =
                cartItemRepository
                        .findByUserOrderByCreatedAtAsc(user);

        for (CartItem cartItem : databaseItems) {

            if (cartItem.getProduct() == null
                    || cartItem.getProduct().getProductId() == null) {
                continue;
            }

            if (cartItem.getQuantity() == null
                    || cartItem.getQuantity() <= 0) {
                continue;
            }

            cartItems.put(
                    cartItem.getProduct().getProductId(),
                    cartItem.getQuantity()
            );
        }

        session.setAttribute(
                "cartItems",
                cartItems
        );

        return cartItems;
    }

    // =========================================================
    // REMOVE ONE PRODUCT FROM DATABASE CART
    // =========================================================

    private void removeProductFromDatabaseCart(
            HttpSession session,
            Product product) {

        User user = getLoggedInUser(session);

        if (user == null || product == null) {
            return;
        }

        Optional<CartItem> existing =
                cartItemRepository.findByUserAndProduct(
                        user,
                        product
                );

        existing.ifPresent(cartItemRepository::delete);
    }

    // =========================================================
    // CHECK WHETHER PRODUCT BELONGS TO LOGGED-IN ARTISAN
    // =========================================================

    private boolean isOwnProduct(
            HttpSession session,
            Product product) {

        Object userIdObject =
                session.getAttribute(
                        "loggedInUserId"
                );

        if (userIdObject == null) {
            return false;
        }

        if (product == null
                || product.getArtisan() == null) {

            return false;
        }

        Long userId =
                Long.valueOf(
                        userIdObject.toString()
                );

        /*
         * If the logged-in user is an artisan,
         * find his/her artisan record.
         */
        Artisan loggedInArtisan =
                artisanRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (loggedInArtisan == null
                || loggedInArtisan.getArtisanId() == null) {

            return false;
        }

        if (product.getArtisan().getArtisanId() == null) {
            return false;
        }

        return product.getArtisan()
                .getArtisanId()
                .equals(
                        loggedInArtisan.getArtisanId()
                );
    }

    // =========================================================
    // CART ITEM VIEW
    // =========================================================

    public static class CartItemView {

        private final Product product;

        private final int quantity;

        private final BigDecimal subtotal;

        public CartItemView(
                Product product,
                int quantity,
                BigDecimal subtotal) {

            this.product = product;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        public Product getProduct() {
            return product;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }
    }
}
