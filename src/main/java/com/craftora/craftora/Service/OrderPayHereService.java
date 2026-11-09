package com.craftora.craftora.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrderPayHereService {

    @Value("${payhere.merchant-id}")
    private String merchantId;

    @Value("${payhere.merchant-secret}")
    private String merchantSecret;

    @Value("${app.base-url}")
    private String baseUrl;


    // =====================================================
    // PAYHERE SANDBOX CHECKOUT URL
    // =====================================================

    private static final String PAYHERE_SANDBOX_URL =
            "https://sandbox.payhere.lk/pay/checkout";


    // =====================================================
    // GET PAYHERE CHECKOUT URL
    // =====================================================

    public String getCheckoutUrl() {
        return PAYHERE_SANDBOX_URL;
    }


    // =====================================================
    // CREATE CUSTOMER PRODUCT PAYMENT DATA
    // =====================================================

    public Map<String, String> createCheckoutData(
            String orderId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            String city,
            String items,
            BigDecimal amount) {

        Map<String, String> data =
                new LinkedHashMap<>();


        // -------------------------------------------------
        // FORMAT AMOUNT
        // -------------------------------------------------

        String formattedAmount =
                amount
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                        .toPlainString();


        // -------------------------------------------------
        // GENERATE PAYHERE HASH
        // -------------------------------------------------

        String hash =
                generateHash(
                        merchantId,
                        orderId,
                        formattedAmount,
                        "LKR",
                        merchantSecret
                );


        // -------------------------------------------------
        // PAYHERE BASIC DETAILS
        // -------------------------------------------------

        data.put(
                "merchant_id",
                merchantId
        );


        // -------------------------------------------------
        // RETURN URL
        // -------------------------------------------------

        data.put(
                "return_url",
                baseUrl
                        + "/order-payment/success?order_id="
                        + orderId
        );


        // -------------------------------------------------
        // CANCEL URL
        // -------------------------------------------------

        data.put(
                "cancel_url",
                baseUrl
                        + "/order-payment/cancel?order_id="
                        + orderId
        );


        // -------------------------------------------------
        // NOTIFY URL
        // -------------------------------------------------

        data.put(
                "notify_url",
                baseUrl
                        + "/order-payment/notify"
        );


        // -------------------------------------------------
        // CUSTOMER DETAILS
        // -------------------------------------------------

        data.put(
                "first_name",
                firstName
        );

        data.put(
                "last_name",
                lastName
        );

        data.put(
                "email",
                email
        );

        data.put(
                "phone",
                phone
        );

        data.put(
                "address",
                address
        );

        data.put(
                "city",
                city
        );

        data.put(
                "country",
                "Sri Lanka"
        );


        // -------------------------------------------------
        // ORDER DETAILS
        // -------------------------------------------------

        data.put(
                "order_id",
                orderId
        );

        data.put(
                "items",
                items
        );

        data.put(
                "currency",
                "LKR"
        );

        data.put(
                "amount",
                formattedAmount
        );

        data.put(
                "hash",
                hash
        );


        return data;
    }


    // =====================================================
    // GENERATE PAYHERE CHECKOUT HASH
    // =====================================================

    private String generateHash(
            String merchantId,
            String orderId,
            String amount,
            String currency,
            String merchantSecret) {

        String secretHash =
                md5(merchantSecret)
                        .toUpperCase();

        String hashString =
                merchantId
                        + orderId
                        + amount
                        + currency
                        + secretHash;

        return md5(hashString)
                .toUpperCase();
    }


    // =====================================================
    // MD5
    // =====================================================

    private String md5(String input) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("MD5");

            byte[] messageDigest =
                    md.digest(
                            input.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hexString =
                    new StringBuilder();

            for (byte b : messageDigest) {

                String hex =
                        Integer.toHexString(
                                0xff & b
                        );

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString
                    .toString()
                    .toUpperCase();

        } catch (NoSuchAlgorithmException e) {

            throw new RuntimeException(
                    "MD5 algorithm not available",
                    e
            );
        }
    }
}