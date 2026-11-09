package com.craftora.craftora.Entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;


    @ManyToOne
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private Customer customer;


    @Column(
            name = "order_date",
            nullable = false
    )
    private LocalDateTime orderDate;


    @Column(
            name = "delivery_name",
            nullable = false
    )
    private String deliveryName;


    @Column(
            name = "delivery_phone",
            nullable = false
    )
    private String deliveryPhone;


    @Column(
            name = "delivery_city",
            nullable = false
    )
    private String deliveryCity;


    @Column(
            name = "delivery_address",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String deliveryAddress;


    @Column(
            name = "total_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalAmount;


    @Column(
            name = "payment_status",
            nullable = false
    )
    private String paymentStatus;


    @Column(
            name = "order_status",
            nullable = false
    )
    private String orderStatus;


    @PrePersist
    protected void onCreate() {

        if (orderDate == null) {

            orderDate =
                    LocalDateTime.now();
        }

        if (paymentStatus == null
                || paymentStatus.isBlank()) {

            paymentStatus =
                    "PENDING";
        }

        if (orderStatus == null
                || orderStatus.isBlank()) {

            orderStatus =
                    "PENDING";
        }
    }


    public Long getOrderId() {

        return orderId;
    }


    public void setOrderId(
            Long orderId) {

        this.orderId =
                orderId;
    }


    public Customer getCustomer() {

        return customer;
    }


    public void setCustomer(
            Customer customer) {

        this.customer =
                customer;
    }


    public LocalDateTime getOrderDate() {

        return orderDate;
    }


    public void setOrderDate(
            LocalDateTime orderDate) {

        this.orderDate =
                orderDate;
    }


    public String getDeliveryName() {

        return deliveryName;
    }


    public void setDeliveryName(
            String deliveryName) {

        this.deliveryName =
                deliveryName;
    }


    public String getDeliveryPhone() {

        return deliveryPhone;
    }


    public void setDeliveryPhone(
            String deliveryPhone) {

        this.deliveryPhone =
                deliveryPhone;
    }


    public String getDeliveryCity() {

        return deliveryCity;
    }


    public void setDeliveryCity(
            String deliveryCity) {

        this.deliveryCity =
                deliveryCity;
    }


    public String getDeliveryAddress() {

        return deliveryAddress;
    }


    public void setDeliveryAddress(
            String deliveryAddress) {

        this.deliveryAddress =
                deliveryAddress;
    }


    public BigDecimal getTotalAmount() {

        return totalAmount;
    }


    public void setTotalAmount(
            BigDecimal totalAmount) {

        this.totalAmount =
                totalAmount;
    }


    public String getPaymentStatus() {

        return paymentStatus;
    }


    public void setPaymentStatus(
            String paymentStatus) {

        this.paymentStatus =
                paymentStatus;
    }


    public String getOrderStatus() {

        return orderStatus;
    }


    public void setOrderStatus(
            String orderStatus) {

        this.orderStatus =
                orderStatus;
    }
}