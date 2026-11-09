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
public class PayHereService {

    // =====================================================
    // PAYHERE CONFIGURATION
    // =====================================================

    @Value("${payhere.merchant-id}")
    private String merchantId;

    @Value("${payhere.merchant-secret}")
    private String merchantSecret;

    // =====================================================
    // CRAFTORA BASE URL
    // =====================================================

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
    // CREATE PAYHERE CHECKOUT DATA
    // =====================================================

    public Map<String, String> createCheckoutData(
            String orderId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            String city,
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
        // GENERATE PAYHERE CHECKOUT HASH
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
        // PAYHERE PARAMETERS
        // -------------------------------------------------

        data.put(
                "merchant_id",
                merchantId
        );

        // -------------------------------------------------
        // RETURN URL
        // IMPORTANT: order_id is included
        // -------------------------------------------------

        data.put(
                "return_url",
                baseUrl
                        + "/artisan-payment/success?order_id="
                        + orderId
        );

        // -------------------------------------------------
        // CANCEL URL
        // -------------------------------------------------

        data.put(
                "cancel_url",
                baseUrl
                        + "/artisan-payment/cancel"
        );

        // -------------------------------------------------
        // NOTIFY URL
        // Currently not used for our email flow
        // -------------------------------------------------

        data.put(
                "notify_url",
                baseUrl
                        + "/artisan-payment/notify"
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
                "Craftora Artisan Registration Fee"
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
    // GENERATE PAYHERE NOTIFICATION MD5
    // =====================================================

    public String generateNotificationMd5(
            String merchantId,
            String orderId,
            String payhereAmount,
            String payhereCurrency,
            String statusCode) {

        String secretHash =
                md5(merchantSecret)
                        .toUpperCase();

        String hashString =
                merchantId
                        + orderId
                        + payhereAmount
                        + payhereCurrency
                        + statusCode
                        + secretHash;

        return md5(hashString)
                .toUpperCase();
    }

    // =====================================================
    // VERIFY MERCHANT ID
    // =====================================================

    public boolean isValidMerchantId(
            String receivedMerchantId) {

        if (receivedMerchantId == null
                || merchantId == null) {

            return false;
        }

        return merchantId.equals(
                receivedMerchantId
        );
    }

    // =====================================================
    // MD5 METHOD
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