package com.craftora.craftora.Service;

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

@Service
public class PaymentReceiptPdfService {

    // =====================================================
    // GENERATE PAYMENT RECEIPT PDF
    // =====================================================

    public byte[] generateReceipt(
            String orderId,
            String artisanName,
            String artisanEmail,
            BigDecimal amount,
            String paymentStatus,
            LocalDateTime paidAt) {

        try {

            // -------------------------------------------------
            // CREATE PDF
            // -------------------------------------------------

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(
                            PageSize.A4,
                            45,
                            45,
                            45,
                            45
                    );

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();

            // =================================================
            // FONTS
            // =================================================

            Font receiptTitleFont =
                    new Font(
                            Font.HELVETICA,
                            17,
                            Font.BOLD
                    );

            Font sectionFont =
                    new Font(
                            Font.HELVETICA,
                            12,
                            Font.BOLD
                    );

            Font labelFont =
                    new Font(
                            Font.HELVETICA,
                            10,
                            Font.BOLD
                    );

            Font normalFont =
                    new Font(
                            Font.HELVETICA,
                            10,
                            Font.NORMAL
                    );

            Font amountFont =
                    new Font(
                            Font.HELVETICA,
                            21,
                            Font.BOLD
                    );

            Font statusFont =
                    new Font(
                            Font.HELVETICA,
                            11,
                            Font.BOLD
                    );

            Font thankYouFont =
                    new Font(
                            Font.HELVETICA,
                            11,
                            Font.NORMAL
                    );

            Font smallFont =
                    new Font(
                            Font.HELVETICA,
                            8,
                            Font.NORMAL
                    );

            // =================================================
            // CRAFTORA LOGO
            // =================================================

            InputStream logoStream =
                    getClass()
                            .getClassLoader()
                            .getResourceAsStream(
                                    "static/images/craftora-logo.png"
                            );

            if (logoStream != null) {

                Image logo =
                        Image.getInstance(
                                logoStream.readAllBytes()
                        );

                logo.scaleToFit(
                        150,
                        70
                );

                logo.setAlignment(
                        Element.ALIGN_CENTER
                );

                document.add(
                        logo
                );

                logoStream.close();

            } else {

                // Fallback if logo is not found

                Font brandFont =
                        new Font(
                                Font.HELVETICA,
                                25,
                                Font.BOLD
                        );

                Paragraph brand =
                        new Paragraph(
                                "CRAFTORA",
                                brandFont
                        );

                brand.setAlignment(
                        Element.ALIGN_CENTER
                );

                document.add(
                        brand
                );
            }

            // =================================================
            // RECEIPT TITLE
            // =================================================

            Paragraph receiptTitle =
                    new Paragraph(
                            "ARTISAN REGISTRATION PAYMENT RECEIPT",
                            receiptTitleFont
                    );

            receiptTitle.setAlignment(
                    Element.ALIGN_CENTER
            );

            receiptTitle.setSpacingBefore(8);
            receiptTitle.setSpacingAfter(18);

            document.add(
                    receiptTitle
            );

            // =================================================
            // PAYMENT STATUS
            // =================================================

            PdfPTable statusTable =
                    new PdfPTable(1);

            statusTable.setWidthPercentage(100);

            PdfPCell statusCell =
                    new PdfPCell();

            statusCell.setBorder(
                    Rectangle.BOX
            );

            statusCell.setPaddingTop(10);
            statusCell.setPaddingBottom(10);

            statusCell.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            String statusText;

            if (paymentStatus == null ||
                    paymentStatus.trim().isEmpty()) {

                statusText = "PAID";

            } else {

                statusText =
                        paymentStatus.toUpperCase();
            }

            Phrase statusPhrase =
                    new Phrase(
                            "PAYMENT STATUS : "
                                    + statusText,
                            statusFont
                    );

            statusCell.addElement(
                    statusPhrase
            );

            statusTable.addCell(
                    statusCell
            );

            document.add(
                    statusTable
            );

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // PAYMENT INFORMATION
            // =================================================

            Paragraph paymentHeading =
                    new Paragraph(
                            "PAYMENT INFORMATION",
                            sectionFont
                    );

            paymentHeading.setSpacingBefore(8);
            paymentHeading.setSpacingAfter(8);

            document.add(
                    paymentHeading
            );

            PdfPTable paymentTable =
                    new PdfPTable(2);

            paymentTable.setWidthPercentage(100);

            paymentTable.setWidths(
                    new float[]{
                            35f,
                            65f
                    }
            );

            addDetailRow(
                    paymentTable,
                    "Order ID",
                    safeValue(orderId),
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

            document.add(
                    paymentTable
            );

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // ARTISAN INFORMATION
            // =================================================

            Paragraph artisanHeading =
                    new Paragraph(
                            "ARTISAN INFORMATION",
                            sectionFont
                    );

            artisanHeading.setSpacingBefore(8);
            artisanHeading.setSpacingAfter(8);

            document.add(
                    artisanHeading
            );

            PdfPTable artisanTable =
                    new PdfPTable(2);

            artisanTable.setWidthPercentage(100);

            artisanTable.setWidths(
                    new float[]{
                            35f,
                            65f
                    }
            );

            addDetailRow(
                    artisanTable,
                    "Artisan Name",
                    safeValue(artisanName),
                    labelFont,
                    normalFont
            );

            addDetailRow(
                    artisanTable,
                    "Email Address",
                    safeValue(artisanEmail),
                    labelFont,
                    normalFont
            );

            document.add(
                    artisanTable
            );

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // AMOUNT SECTION
            // =================================================

            PdfPTable amountTable =
                    new PdfPTable(2);

            amountTable.setWidthPercentage(100);

            amountTable.setWidths(
                    new float[]{
                            55f,
                            45f
                    }
            );

            PdfPCell amountLabelCell =
                    new PdfPCell(
                            new Phrase(
                                    "ARTISAN REGISTRATION FEE",
                                    labelFont
                            )
                    );

            amountLabelCell.setBorder(
                    Rectangle.BOX
            );

            amountLabelCell.setPaddingTop(13);
            amountLabelCell.setPaddingBottom(13);

            amountLabelCell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            PdfPCell amountValueCell =
                    new PdfPCell(
                            new Phrase(
                                    "LKR "
                                            + formatAmount(amount),
                                    amountFont
                            )
                    );

            amountValueCell.setBorder(
                    Rectangle.BOX
            );

            amountValueCell.setPaddingTop(10);
            amountValueCell.setPaddingBottom(10);

            amountValueCell.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );

            amountValueCell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            amountTable.addCell(
                    amountLabelCell
            );

            amountTable.addCell(
                    amountValueCell
            );

            document.add(
                    amountTable
            );

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // TRANSACTION DETAILS
            // =================================================

            Paragraph transactionHeading =
                    new Paragraph(
                            "TRANSACTION DETAILS",
                            sectionFont
                    );

            transactionHeading.setSpacingBefore(8);
            transactionHeading.setSpacingAfter(8);

            document.add(
                    transactionHeading
            );

            PdfPTable transactionTable =
                    new PdfPTable(2);

            transactionTable.setWidthPercentage(100);

            transactionTable.setWidths(
                    new float[]{
                            35f,
                            65f
                    }
            );

            addDetailRow(
                    transactionTable,
                    "Transaction Reference",
                    safeValue(orderId),
                    labelFont,
                    normalFont
            );

            addDetailRow(
                    transactionTable,
                    "Payment Status",
                    statusText,
                    labelFont,
                    normalFont
            );

            document.add(
                    transactionTable
            );

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // SUCCESS MESSAGE
            // =================================================

            Paragraph completedMessage =
                    new Paragraph(
                            "Payment Completed Successfully",
                            sectionFont
                    );

            completedMessage.setAlignment(
                    Element.ALIGN_CENTER
            );

            completedMessage.setSpacingBefore(8);
            completedMessage.setSpacingAfter(6);

            document.add(
                    completedMessage
            );

            Paragraph thankYou =
                    new Paragraph(
                            "Thank you for registering as an artisan with Craftora.",
                            thankYouFont
                    );

            thankYou.setAlignment(
                    Element.ALIGN_CENTER
            );

            thankYou.setSpacingAfter(22);

            document.add(
                    thankYou
            );

            // =================================================
            // FOOTER LINE
            // =================================================

            PdfPTable footerLine =
                    new PdfPTable(1);

            footerLine.setWidthPercentage(100);

            PdfPCell lineCell =
                    new PdfPCell(
                            new Phrase("")
                    );

            lineCell.setBorderWidthTop(1);
            lineCell.setBorderWidthBottom(0);
            lineCell.setBorderWidthLeft(0);
            lineCell.setBorderWidthRight(0);

            lineCell.setPadding(0);

            footerLine.addCell(
                    lineCell
            );

            document.add(
                    footerLine
            );

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // FOOTER
            // =================================================

            Paragraph footer =
                    new Paragraph(
                            "This is a computer-generated payment receipt.",
                            smallFont
                    );

            footer.setAlignment(
                    Element.ALIGN_CENTER
            );

            footer.setSpacingAfter(4);

            document.add(
                    footer
            );

            Paragraph footer2 =
                    new Paragraph(
                            "Craftora Handicraft Development and Community Management System",
                            smallFont
                    );

            footer2.setAlignment(
                    Element.ALIGN_CENTER
            );

            footer2.setSpacingAfter(4);

            document.add(
                    footer2
            );

            Paragraph footer3 =
                    new Paragraph(
                            "Payment processed securely through PayHere.",
                            smallFont
                    );

            footer3.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(
                    footer3
            );

            // =================================================
            // CLOSE PDF
            // =================================================

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate payment receipt PDF.",
                    e
            );
        }
    }

    // =====================================================
    // ADD DETAIL ROW
    // =====================================================

    private void addDetailRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                label,
                                labelFont
                        )
                );

        labelCell.setPadding(9);

        labelCell.setBorder(
                Rectangle.BOX
        );

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                value,
                                valueFont
                        )
                );

        valueCell.setPadding(9);

        valueCell.setBorder(
                Rectangle.BOX
        );

        table.addCell(
                labelCell
        );

        table.addCell(
                valueCell
        );
    }

    // =====================================================
    // SAFE VALUE
    // =====================================================

    private String safeValue(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }

    // =====================================================
    // FORMAT AMOUNT
    // =====================================================

    private String formatAmount(
            BigDecimal amount) {

        if (amount == null) {
            return "0.00";
        }

        return String.format(
                "%.2f",
                amount
        );
    }

    // =====================================================
    // FORMAT DATE
    // =====================================================

    private String formatDate(
            LocalDateTime dateTime) {

        if (dateTime == null) {
            return "";
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd-MM-yyyy HH:mm:ss"
                );

        return dateTime.format(
                formatter
        );
    }
}