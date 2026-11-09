package com.craftora.craftora.Service;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class ProductPaymentSuccessAspect {

    private final ProductPaymentReceiptService productPaymentReceiptService;

    public ProductPaymentSuccessAspect(
            ProductPaymentReceiptService productPaymentReceiptService) {

        this.productPaymentReceiptService =
                productPaymentReceiptService;
    }

    // =====================================================
    // HANDLE PRODUCT PAYMENT SUCCESS
    // =====================================================

    @AfterReturning(
            pointcut =
                    "execution(* com.craftora.craftora.Controller.OrderPaymentController.paymentSuccess(..))",
            returning = "result"
    )
    public void afterPaymentSuccess(
            JoinPoint joinPoint,
            Object result) {

        try {

            Object[] args = joinPoint.getArgs();

            if (args == null || args.length == 0) {
                return;
            }

            String payHereOrderId = null;

            // Find the String argument.
            // The current paymentSuccess() method receives
            // the PayHere order_id as a String.
            for (Object arg : args) {

                if (arg instanceof String) {

                    payHereOrderId =
                            (String) arg;

                    break;
                }
            }

            if (payHereOrderId == null
                    || payHereOrderId.trim().isEmpty()) {

                return;
            }

            // PayHere may sometimes send duplicate/comma-separated
            // order_id values.
            if (payHereOrderId.contains(",")) {

                payHereOrderId =
                        payHereOrderId
                                .split(",")[0]
                                .trim();
            }

            // Generate PDF receipt and send email.
            productPaymentReceiptService
                    .generateAndSendReceipt(
                            payHereOrderId
                    );

        } catch (Exception e) {

            // Email/receipt failure must not
            // undo a completed payment.
            e.printStackTrace();
        }
    }
}