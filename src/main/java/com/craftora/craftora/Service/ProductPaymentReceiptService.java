package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Entity.OrderPayment;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Repository.OrderPaymentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductPaymentReceiptService {

    private final java.util.Set<String> processedTransactions =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Autowired
    private OrderPaymentRepository orderPaymentRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private com.craftora.craftora.Repository.OrderRepository orderRepository;

    @Autowired
    private ProductPaymentReceiptPdfService pdfService;

    @Autowired
    private ProductPaymentEmailService emailService;

    // =====================================================
    // GENERATE AND SEND PRODUCT PAYMENT RECEIPT
    // =====================================================

    public void generateAndSendReceipt(
            String transactionReference) throws Exception {

        if (transactionReference == null
                || transactionReference.trim().isEmpty()) {
            return;
        }

        OrderPayment payment =
                orderPaymentRepository
                        .findByTransactionId(transactionReference)
                        .orElse(null);

        if (payment == null) {
            return;
        }

        // Prevent duplicate receipt emails for the same transaction
        // while the application is running.
        if (!processedTransactions.add(transactionReference)) {
            return;
        }

        // Only send a receipt for a completed product payment.
        if (!"PAID".equalsIgnoreCase(
                payment.getPaymentStatus())) {
            processedTransactions.remove(transactionReference);
            return;
        }

        Order order = null;

        List<OrderItem> orderItems = null;

        // The order_id is stored in OrderPayment.
        // The controller has already completed the payment,
        // so the order can be retrieved from the payment record.
        final Long orderId = payment.getOrderId();

        if (orderId == null) {
            return;
        }

        // Find the order using the repository through the helper below.
        order = findOrder(orderId);

        if (order == null) {
            return;
        }

        orderItems =
                orderItemRepository.findByOrder(order);

        String customerEmail = null;
        String customerName = null;

        if (order.getCustomer() != null
                && order.getCustomer().getUser() != null) {

            customerEmail =
                    order.getCustomer()
                            .getUser()
                            .getEmail();

            customerName =
                    order.getCustomer()
                            .getUser()
                            .getFullName();
        }

        if (customerEmail == null
                || customerEmail.trim().isEmpty()) {
            return;
        }

        LocalDateTime paidAt =
                payment.getPaidAt();

        if (paidAt == null) {
            paidAt = LocalDateTime.now();
        }

        byte[] pdfReceipt =
                pdfService.generateOrderReceipt(
                        transactionReference,
                        order,
                        orderItems,
                        paidAt
                );

        emailService.sendOrderPaymentReceipt(
                customerEmail,
                customerName,
                order.getOrderId(),
                order.getTotalAmount(),
                paidAt,
                pdfReceipt
        );
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
}
