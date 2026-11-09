package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProductPaymentReceiptPdfService {

    public byte[] generateOrderReceipt(
            String transactionReference,
            Order order,
            List<OrderItem> orderItems,
            LocalDateTime paidAt) {

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(
                    PageSize.A4,
                    45,
                    45,
                    45,
                    45
            );

            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(
                    Font.HELVETICA,
                    17,
                    Font.BOLD
            );

            Font sectionFont = new Font(
                    Font.HELVETICA,
                    12,
                    Font.BOLD
            );

            Font labelFont = new Font(
                    Font.HELVETICA,
                    9,
                    Font.BOLD
            );

            Font normalFont = new Font(
                    Font.HELVETICA,
                    9,
                    Font.NORMAL
            );

            Font amountFont = new Font(
                    Font.HELVETICA,
                    18,
                    Font.BOLD
            );

            Font statusFont = new Font(
                    Font.HELVETICA,
                    11,
                    Font.BOLD
            );

            Font smallFont = new Font(
                    Font.HELVETICA,
                    8,
                    Font.NORMAL
            );

            // =================================================
            // CRAFTORA LOGO
            // =================================================

            InputStream logoStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                            "static/images/craftora-logo.png"
                    );

            if (logoStream != null) {
                Image logo = Image.getInstance(
                        logoStream.readAllBytes()
                );

                logo.scaleToFit(150, 70);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);

                logoStream.close();
            } else {
                Font brandFont = new Font(
                        Font.HELVETICA,
                        25,
                        Font.BOLD
                );

                Paragraph brand = new Paragraph(
                        "CRAFTORA",
                        brandFont
                );

                brand.setAlignment(Element.ALIGN_CENTER);
                document.add(brand);
            }

            // =================================================
            // RECEIPT TITLE
            // =================================================

            Paragraph receiptTitle = new Paragraph(
                    "PRODUCT PAYMENT RECEIPT",
                    titleFont
            );

            receiptTitle.setAlignment(Element.ALIGN_CENTER);
            receiptTitle.setSpacingBefore(8);
            receiptTitle.setSpacingAfter(18);
            document.add(receiptTitle);

            // =================================================
            // PAYMENT STATUS
            // =================================================

            PdfPTable statusTable = new PdfPTable(1);
            statusTable.setWidthPercentage(100);

            PdfPCell statusCell = new PdfPCell();
            statusCell.setBorder(Rectangle.BOX);
            statusCell.setPaddingTop(10);
            statusCell.setPaddingBottom(10);
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            statusCell.addElement(
                    new Phrase(
                            "PAYMENT STATUS : PAID",
                            statusFont
                    )
            );

            statusTable.addCell(statusCell);
            document.add(statusTable);
            document.add(new Paragraph(" "));

            // =================================================
            // PAYMENT INFORMATION
            // =================================================

            Paragraph paymentHeading = new Paragraph(
                    "PAYMENT INFORMATION",
                    sectionFont
            );

            paymentHeading.setSpacingBefore(8);
            paymentHeading.setSpacingAfter(8);
            document.add(paymentHeading);

            PdfPTable paymentTable = new PdfPTable(2);
            paymentTable.setWidthPercentage(100);
            paymentTable.setWidths(new float[]{35f, 65f});

            addDetailRow(
                    paymentTable,
                    "Order ID",
                    order != null
                            ? String.valueOf(order.getOrderId())
                            : "-",
                    labelFont,
                    normalFont
            );

            addDetailRow(
                    paymentTable,
                    "Transaction Reference",
                    safeValue(transactionReference),
                    labelFont,
                    normalFont
            );

            addDetailRow(
                    paymentTable,
                    "Payment Method",
                    "PayHere",
                    labelFont,
                    normalFont
            );

            addDetailRow(
                    paymentTable,
                    "Payment Date",
                    formatDate(paidAt),
                    labelFont,
                    normalFont
            );

            document.add(paymentTable);
            document.add(new Paragraph(" "));

            // =================================================
            // CUSTOMER INFORMATION
            // =================================================

            Paragraph customerHeading = new Paragraph(
                    "CUSTOMER INFORMATION",
                    sectionFont
            );

            customerHeading.setSpacingBefore(8);
            customerHeading.setSpacingAfter(8);
            document.add(customerHeading);

            PdfPTable customerTable = new PdfPTable(2);
            customerTable.setWidthPercentage(100);
            customerTable.setWidths(new float[]{35f, 65f});

            String customerName = "-";
            String customerEmail = "-";

            if (order != null
                    && order.getCustomer() != null
                    && order.getCustomer().getUser() != null) {

                customerName = safeValue(
                        order.getCustomer()
                                .getUser()
                                .getFullName()
                );

                customerEmail = safeValue(
                        order.getCustomer()
                                .getUser()
                                .getEmail()
                );
            }

            addDetailRow(
                    customerTable,
                    "Customer Name",
                    customerName,
                    labelFont,
                    normalFont
            );

            addDetailRow(
                    customerTable,
                    "Email Address",
                    customerEmail,
                    labelFont,
                    normalFont
            );

            document.add(customerTable);
            document.add(new Paragraph(" "));

            // =================================================
            // DELIVERY INFORMATION
            // =================================================

            Paragraph deliveryHeading = new Paragraph(
                    "DELIVERY INFORMATION",
                    sectionFont
            );

            deliveryHeading.setSpacingBefore(8);
            deliveryHeading.setSpacingAfter(8);
            document.add(deliveryHeading);

            PdfPTable deliveryTable = new PdfPTable(2);
            deliveryTable.setWidthPercentage(100);
            deliveryTable.setWidths(new float[]{35f, 65f});

            if (order != null) {
                addDetailRow(
                        deliveryTable,
                        "Delivery Name",
                        safeValue(order.getDeliveryName()),
                        labelFont,
                        normalFont
                );

                addDetailRow(
                        deliveryTable,
                        "Phone",
                        safeValue(order.getDeliveryPhone()),
                        labelFont,
                        normalFont
                );

                addDetailRow(
                        deliveryTable,
                        "City",
                        safeValue(order.getDeliveryCity()),
                        labelFont,
                        normalFont
                );

                addDetailRow(
                        deliveryTable,
                        "Address",
                        safeValue(order.getDeliveryAddress()),
                        labelFont,
                        normalFont
                );
            }

            document.add(deliveryTable);
            document.add(new Paragraph(" "));

            // =================================================
            // ORDER ITEMS
            // =================================================

            Paragraph itemsHeading = new Paragraph(
                    "ORDER ITEMS",
                    sectionFont
            );

            itemsHeading.setSpacingBefore(8);
            itemsHeading.setSpacingAfter(8);
            document.add(itemsHeading);

            PdfPTable itemTable = new PdfPTable(5);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(
                    new float[]{28f, 22f, 10f, 18f, 22f}
            );

            addHeaderCell(itemTable, "Product", labelFont);
            addHeaderCell(itemTable, "Artisan", labelFont);
            addHeaderCell(itemTable, "Qty", labelFont);
            addHeaderCell(itemTable, "Unit Price", labelFont);
            addHeaderCell(itemTable, "Subtotal", labelFont);

            if (orderItems != null && !orderItems.isEmpty()) {
                for (OrderItem item : orderItems) {
                    addBodyCell(
                            itemTable,
                            item != null
                                    ? safeValue(item.getProductName())
                                    : "-",
                            normalFont
                    );

                    String artisanName = "-";

                    if (item != null
                            && item.getArtisan() != null
                            && item.getArtisan().getUser() != null) {

                        artisanName = safeValue(
                                item.getArtisan()
                                        .getUser()
                                        .getFullName()
                        );
                    }

                    addBodyCell(
                            itemTable,
                            artisanName,
                            normalFont
                    );

                    addBodyCell(
                            itemTable,
                            item != null && item.getQuantity() != null
                                    ? String.valueOf(item.getQuantity())
                                    : "0",
                            normalFont
                    );

                    addBodyCell(
                            itemTable,
                            item != null
                                    ? "LKR "
                                    + formatAmount(item.getUnitPrice())
                                    : "LKR 0.00",
                            normalFont
                    );

                    addBodyCell(
                            itemTable,
                            item != null
                                    ? "LKR "
                                    + formatAmount(item.getSubtotal())
                                    : "LKR 0.00",
                            normalFont
                    );
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(
                        new Phrase(
                                "No order items found.",
                                normalFont
                        )
                );
                emptyCell.setColspan(5);
                emptyCell.setPadding(8);
                itemTable.addCell(emptyCell);
            }

            document.add(itemTable);
            document.add(new Paragraph(" "));

            // =================================================
            // TOTAL
            // =================================================

            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{55f, 45f});

            PdfPCell totalLabelCell = new PdfPCell(
                    new Phrase(
                            "TOTAL PAID",
                            labelFont
                    )
            );

            totalLabelCell.setBorder(Rectangle.BOX);
            totalLabelCell.setPaddingTop(13);
            totalLabelCell.setPaddingBottom(13);

            PdfPCell totalValueCell = new PdfPCell(
                    new Phrase(
                            "LKR "
                                    + formatAmount(
                                    order != null
                                            ? order.getTotalAmount()
                                            : BigDecimal.ZERO
                            ),
                            amountFont
                    )
            );

            totalValueCell.setBorder(Rectangle.BOX);
            totalValueCell.setPaddingTop(10);
            totalValueCell.setPaddingBottom(10);
            totalValueCell.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );

            totalTable.addCell(totalLabelCell);
            totalTable.addCell(totalValueCell);

            document.add(totalTable);
            document.add(new Paragraph(" "));

            // =================================================
            // DELIVERY NOTE
            // =================================================

            Paragraph deliveryNote = new Paragraph(
                    "Delivery: FREE DELIVERY",
                    sectionFont
            );

            deliveryNote.setAlignment(Element.ALIGN_CENTER);
            deliveryNote.setSpacingBefore(8);
            deliveryNote.setSpacingAfter(6);
            document.add(deliveryNote);

            Paragraph deliveryMessage = new Paragraph(
                    "Your order will be delivered directly by the relevant artisan. "
                            + "If your order contains products from multiple artisans, "
                            + "you may receive separate deliveries.",
                    normalFont
            );

            deliveryMessage.setAlignment(Element.ALIGN_CENTER);
            deliveryMessage.setSpacingAfter(6);
            document.add(deliveryMessage);

            Paragraph expectedMessage = new Paragraph(
                    "Expected delivery: within 2 days.",
                    normalFont
            );

            expectedMessage.setAlignment(Element.ALIGN_CENTER);
            expectedMessage.setSpacingAfter(18);
            document.add(expectedMessage);

            // =================================================
            // SUCCESS MESSAGE
            // =================================================

            Paragraph completedMessage = new Paragraph(
                    "Payment Completed Successfully",
                    sectionFont
            );

            completedMessage.setAlignment(Element.ALIGN_CENTER);
            completedMessage.setSpacingBefore(8);
            completedMessage.setSpacingAfter(6);
            document.add(completedMessage);

            Paragraph thankYou = new Paragraph(
                    "Thank you for shopping with Craftora.",
                    normalFont
            );

            thankYou.setAlignment(Element.ALIGN_CENTER);
            thankYou.setSpacingAfter(22);
            document.add(thankYou);

            // =================================================
            // FOOTER
            // =================================================

            PdfPTable footerLine = new PdfPTable(1);
            footerLine.setWidthPercentage(100);

            PdfPCell lineCell = new PdfPCell(
                    new Phrase("")
            );

            lineCell.setBorderWidthTop(1);
            lineCell.setBorderWidthBottom(0);
            lineCell.setBorderWidthLeft(0);
            lineCell.setBorderWidthRight(0);
            lineCell.setPadding(0);

            footerLine.addCell(lineCell);
            document.add(footerLine);
            document.add(new Paragraph(" "));

            Paragraph footer = new Paragraph(
                    "This is a computer-generated payment receipt.",
                    smallFont
            );

            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingAfter(4);
            document.add(footer);

            Paragraph footer2 = new Paragraph(
                    "Craftora Handicraft Development and Community Management System",
                    smallFont
            );

            footer2.setAlignment(Element.ALIGN_CENTER);
            footer2.setSpacingAfter(4);
            document.add(footer2);

            Paragraph footer3 = new Paragraph(
                    "Payment processed securely through PayHere.",
                    smallFont
            );

            footer3.setAlignment(Element.ALIGN_CENTER);
            document.add(footer3);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate product payment receipt PDF.",
                    e
            );
        }
    }

    private void addHeaderCell(
            PdfPTable table,
            String value,
            Font font) {

        PdfPCell cell = new PdfPCell(
                new Phrase(value, font)
        );

        cell.setPadding(7);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);

        table.addCell(cell);
    }

    private void addBodyCell(
            PdfPTable table,
            String value,
            Font font) {

        PdfPCell cell = new PdfPCell(
                new Phrase(value, font)
        );

        cell.setPadding(7);
        cell.setBorder(Rectangle.BOX);

        table.addCell(cell);
    }

    private void addDetailRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {

        PdfPCell labelCell = new PdfPCell(
                new Phrase(label, labelFont)
        );

        labelCell.setPadding(9);
        labelCell.setBorder(Rectangle.BOX);

        PdfPCell valueCell = new PdfPCell(
                new Phrase(value, valueFont)
        );

        valueCell.setPadding(9);
        valueCell.setBorder(Rectangle.BOX);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String safeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }

        return String.format("%.2f", amount);
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd-MM-yyyy HH:mm:ss"
                );

        return dateTime.format(formatter);
    }
}
