package com.craftora.craftora.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EventPayHereService {

    @Value("${payhere.merchant-id}")
    private String merchantId;

    @Value("${payhere.merchant-secret}")
    private String merchantSecret;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final String PAYHERE_SANDBOX_URL =
            "https://sandbox.payhere.lk/pay/checkout";

    // =====================================================
    // PAYHERE CHECKOUT URL
    // =====================================================

    public String getCheckoutUrl() {
        return PAYHERE_SANDBOX_URL;
    }

    // =====================================================
    // CREATE EVENT CHECKOUT DATA
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

        String formattedAmount =
                amount.setScale(
                        2,
                        java.math.RoundingMode.HALF_UP
                ).toPlainString();

        String hash =
                generateHash(
                        merchantId,
                        orderId,
                        formattedAmount,
                        "LKR",
                        merchantSecret
                );

        data.put(
                "merchant_id",
                merchantId
        );

        data.put(
                "return_url",
                baseUrl
                        + "/event-payment/success?order_id="
                        + orderId
        );

        data.put(
                "cancel_url",
                baseUrl
                        + "/event-payment/cancel?order_id="
                        + orderId
        );

        data.put(
                "notify_url",
                baseUrl
                        + "/event-payment/notify"
        );

        data.put(
                "first_name",
                safe(firstName)
        );

        data.put(
                "last_name",
                safe(lastName)
        );

        data.put(
                "email",
                safe(email)
        );

        data.put(
                "phone",
                safe(phone)
        );

        data.put(
                "address",
                safe(address)
        );

        data.put(
                "city",
                safe(city)
        );

        data.put(
                "country",
                "Sri Lanka"
        );

        data.put(
                "order_id",
                orderId
        );

        data.put(
                "items",
                safe(items)
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
    // GENERATE PAYHERE HASH
    // =====================================================

    private String generateHash(
            String merchantId,
            String orderId,
            String amount,
            String currency,
            String merchantSecret) {

        String hashedSecret =
                md5(merchantSecret)
                        .toUpperCase();

        String input =
                merchantId
                        + orderId
                        + amount
                        + currency
                        + hashedSecret;

        return md5(input)
                .toUpperCase();
    }

    // =====================================================
    // MD5
    // =====================================================

    private String md5(String input) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("MD5");

            byte[] digest =
                    md.digest(
                            input.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hexString =
                    new StringBuilder();

            for (byte b : digest) {

                String hex =
                        Integer.toHexString(
                                0xff & b
                        );

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate PayHere hash.",
                    e
            );
        }
    }

    // =====================================================
    // SAFE VALUE
    // =====================================================

    private String safe(String value) {

        if (value == null) {
            return "";
        }

        return value;
    }
}