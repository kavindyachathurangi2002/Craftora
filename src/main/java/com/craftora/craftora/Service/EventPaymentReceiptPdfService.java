package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Event;

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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


@Service
public class EventPaymentReceiptPdfService {


    // =====================================================
    // GENERATE EVENT PAYMENT RECEIPT
    // =====================================================

    public byte[] generateEventReceipt(

            String orderId,
            String participantName,
            String participantEmail,
            String registrationRole,
            Event event,
            BigDecimal amount,
            String paymentStatus,
            LocalDateTime paidAt) {

        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();


            // =================================================
            // A4 DOCUMENT
            // =================================================

            Document document =
                    new Document(
                            PageSize.A4,
                            38,
                            38,
                            32,
                            32
                    );


            PdfWriter.getInstance(
                    document,
                    outputStream
            );


            document.open();


            // =================================================
            // FONTS
            // =================================================

            Font titleFont =
                    new Font(
                            Font.HELVETICA,
                            15,
                            Font.BOLD
                    );


            Font sectionFont =
                    new Font(
                            Font.HELVETICA,
                            10,
                            Font.BOLD
                    );


            Font labelFont =
                    new Font(
                            Font.HELVETICA,
                            8,
                            Font.BOLD
                    );


            Font normalFont =
                    new Font(
                            Font.HELVETICA,
                            8,
                            Font.NORMAL
                    );


            Font statusFont =
                    new Font(
                            Font.HELVETICA,
                            10,
                            Font.BOLD
                    );


            Font amountLabelFont =
                    new Font(
                            Font.HELVETICA,
                            9,
                            Font.BOLD
                    );


            Font amountFont =
                    new Font(
                            Font.HELVETICA,
                            18,
                            Font.BOLD
                    );


            Font thankYouFont =
                    new Font(
                            Font.HELVETICA,
                            9,
                            Font.BOLD
                    );


            Font smallFont =
                    new Font(
                            Font.HELVETICA,
                            7,
                            Font.NORMAL
                    );


            // =================================================
            // LOGO
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
                        105,
                        45
                );


                logo.setAlignment(
                        Element.ALIGN_CENTER
                );


                document.add(logo);

                logoStream.close();

            } else {

                Paragraph brand =
                        new Paragraph(
                                "CRAFTORA",
                                new Font(
                                        Font.HELVETICA,
                                        22,
                                        Font.BOLD
                                )
                        );

                brand.setAlignment(
                        Element.ALIGN_CENTER
                );

                document.add(brand);
            }


            // =================================================
            // TITLE
            // =================================================

            Paragraph title =
                    new Paragraph(
                            "EVENT REGISTRATION PAYMENT RECEIPT",
                            titleFont
                    );


            title.setAlignment(
                    Element.ALIGN_CENTER
            );


            title.setSpacingBefore(3);

            title.setSpacingAfter(8);


            document.add(title);


            // =================================================
            // PAYMENT STATUS
            // =================================================

            String statusText =
                    paymentStatus == null
                            || paymentStatus.trim().isEmpty()
                            ? "PAID"
                            : paymentStatus.toUpperCase();


            PdfPTable statusTable =
                    new PdfPTable(1);


            statusTable.setWidthPercentage(100);


            PdfPCell statusCell =
                    new PdfPCell(
                            new Phrase(
                                    "PAYMENT STATUS : "
                                            + statusText,
                                    statusFont
                            )
                    );


            statusCell.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );


            statusCell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );


            statusCell.setPaddingTop(6);

            statusCell.setPaddingBottom(6);


            statusCell.setBorder(
                    Rectangle.BOX
            );


            statusTable.addCell(
                    statusCell
            );


            statusTable.setSpacingAfter(10);


            document.add(statusTable);


            // =================================================
            // GET EVENT DATA
            // =================================================

            String eventTitle = "-";
            String eventType = "-";
            String eventDate = "-";
            String eventTime = "-";
            String eventLocation = "-";


            if (event != null) {

                eventTitle =
                        safeValue(
                                event.getEventTitle()
                        );


                eventType =
                        safeValue(
                                event.getEventType()
                        );


                if (event.getEventDate() != null) {

                    eventDate =
                            event.getEventDate()
                                    .format(
                                            DateTimeFormatter
                                                    .ofPattern(
                                                            "dd-MM-yyyy"
                                                    )
                                    );
                }


                eventTime =
                        formatEventTime(
                                event.getStartTime(),
                                event.getEndTime()
                        );


                eventLocation =
                        safeValue(
                                event.getLocation()
                        );
            }


            // =================================================
            // PAYMENT + PARTICIPANT
            // =================================================

            PdfPTable twoColumnTable =
                    new PdfPTable(2);


            twoColumnTable.setWidthPercentage(100);


            twoColumnTable.setWidths(
                    new float[]{
                            50f,
                            50f
                    }
            );


            twoColumnTable.setSpacingAfter(10);


            // -------------------------------------------------
            // PAYMENT INFORMATION
            // -------------------------------------------------

            PdfPCell paymentCell =
                    createSectionCell();


            paymentCell.addElement(
                    createSectionTitle(
                            "PAYMENT INFORMATION",
                            sectionFont
                    )
            );


            addSimpleRow(
                    paymentCell,
                    "Order ID",
                    safeValue(orderId),
                    labelFont,
                    normalFont
            );


            addSimpleRow(
                    paymentCell,
                    "Payment Method",
                    "PayHere",
                    labelFont,
                    normalFont
            );


            addSimpleRow(
                    paymentCell,
                    "Payment Date",
                    formatDate(paidAt),
                    labelFont,
                    normalFont
            );


            twoColumnTable.addCell(
                    paymentCell
            );


            // -------------------------------------------------
            // PARTICIPANT INFORMATION
            // -------------------------------------------------

            PdfPCell participantCell =
                    createSectionCell();


            participantCell.addElement(
                    createSectionTitle(
                            "PARTICIPANT INFORMATION",
                            sectionFont
                    )
            );


            addSimpleRow(
                    participantCell,
                    "Name",
                    safeValue(participantName),
                    labelFont,
                    normalFont
            );


            addSimpleRow(
                    participantCell,
                    "Email",
                    safeValue(participantEmail),
                    labelFont,
                    normalFont
            );


            addSimpleRow(
                    participantCell,
                    "Role",
                    safeValue(registrationRole),
                    labelFont,
                    normalFont
            );


            twoColumnTable.addCell(
                    participantCell
            );


            document.add(
                    twoColumnTable
            );


            // =================================================
            // EVENT INFORMATION
            // =================================================

            PdfPCell eventCell =
                    createSectionCell();


            eventCell.addElement(
                    createSectionTitle(
                            "EVENT INFORMATION",
                            sectionFont
                    )
            );


            addSimpleRow(
                    eventCell,
                    "Event Title",
                    eventTitle,
                    labelFont,
                    normalFont
            );


            addSimpleRow(
                    eventCell,
                    "Event Type",
                    eventType,
                    labelFont,
                    normalFont
            );


            addSimpleRow(
                    eventCell,
                    "Date",
                    eventDate,
                    labelFont,
                    normalFont
            );


            addSimpleRow(
                    eventCell,
                    "Time",
                    eventTime,
                    labelFont,
                    normalFont
            );


            addSimpleRow(
                    eventCell,
                    "Location",
                    eventLocation,
                    labelFont,
                    normalFont
            );


            PdfPTable eventTable =
                    new PdfPTable(1);


            eventTable.setWidthPercentage(100);

            eventTable.setSpacingAfter(10);


            eventTable.addCell(
                    eventCell
            );


            document.add(
                    eventTable
            );


            // =================================================
            // TOTAL AMOUNT
            // =================================================

            PdfPTable amountTable =
                    new PdfPTable(2);


            amountTable.setWidthPercentage(100);


            amountTable.setWidths(
                    new float[]{
                            58f,
                            42f
                    }
            );


            amountTable.setSpacingAfter(9);


            PdfPCell amountLabel =
                    new PdfPCell(
                            new Phrase(
                                    "TOTAL REGISTRATION FEE",
                                    amountLabelFont
                            )
                    );


            amountLabel.setBorder(
                    Rectangle.BOX
            );


            amountLabel.setPadding(
                    9
            );


            amountLabel.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );


            amountTable.addCell(
                    amountLabel
            );


            PdfPCell amountValue =
                    new PdfPCell(
                            new Phrase(
                                    "LKR "
                                            + formatAmount(
                                                    amount
                                            ),
                                    amountFont
                            )
                    );


            amountValue.setBorder(
                    Rectangle.BOX
            );


            amountValue.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );


            amountValue.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );


            amountValue.setPadding(
                    7
            );


            amountTable.addCell(
                    amountValue
            );


            document.add(
                    amountTable
            );


            // =================================================
            // TRANSACTION INFORMATION
            // =================================================

            PdfPTable transactionTable =
                    new PdfPTable(2);


            transactionTable.setWidthPercentage(100);


            transactionTable.setWidths(
                    new float[]{
                            30f,
                            70f
                    }
            );


            transactionTable.setSpacingAfter(9);


            addPlainTableRow(
                    transactionTable,
                    "Transaction Reference",
                    safeValue(orderId),
                    labelFont,
                    normalFont
            );


            addPlainTableRow(
                    transactionTable,
                    "Payment Status",
                    statusText,
                    labelFont,
                    normalFont
            );


            document.add(
                    transactionTable
            );


            // =================================================
            // SUCCESS MESSAGE
            // =================================================

            Paragraph success =
                    new Paragraph(
                            "Payment Completed Successfully",
                            thankYouFont
                    );


            success.setAlignment(
                    Element.ALIGN_CENTER
            );


            success.setSpacingAfter(3);


            document.add(success);


            Paragraph thankYou =
                    new Paragraph(
                            "Thank you for registering for this event with Craftora.",
                            smallFont
                    );


            thankYou.setAlignment(
                    Element.ALIGN_CENTER
            );


            thankYou.setSpacingAfter(8);


            document.add(thankYou);


            // =================================================
            // FOOTER LINE
            // =================================================

            PdfPTable lineTable =
                    new PdfPTable(1);


            lineTable.setWidthPercentage(100);


            PdfPCell lineCell =
                    new PdfPCell(
                            new Phrase("")
                    );


            lineCell.setBorderWidthTop(
                    0.7f
            );


            lineCell.setBorderWidthBottom(
                    0
            );


            lineCell.setBorderWidthLeft(
                    0
            );


            lineCell.setBorderWidthRight(
                    0
            );


            lineCell.setPadding(0);


            lineTable.addCell(
                    lineCell
            );


            document.add(
                    lineTable
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


            footer.setSpacingBefore(4);

            footer.setSpacingAfter(2);


            document.add(footer);


            Paragraph footer2 =
                    new Paragraph(
                            "Craftora Handicraft Development and Community Management System",
                            smallFont
                    );


            footer2.setAlignment(
                    Element.ALIGN_CENTER
            );


            footer2.setSpacingAfter(2);


            document.add(footer2);


            Paragraph footer3 =
                    new Paragraph(
                            "Payment processed securely through PayHere.",
                            smallFont
                    );


            footer3.setAlignment(
                    Element.ALIGN_CENTER
            );


            document.add(footer3);


            // =================================================
            // CLOSE
            // =================================================

            document.close();


            return outputStream.toByteArray();


        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate event payment receipt PDF.",
                    e
            );
        }
    }


    // =====================================================
    // SECTION CELL
    // =====================================================

    private PdfPCell createSectionCell() {

        PdfPCell cell =
                new PdfPCell();


        cell.setBorder(
                Rectangle.BOX
        );


        cell.setPadding(
                7
        );


        return cell;
    }


    // =====================================================
    // SECTION TITLE
    // =====================================================

    private Paragraph createSectionTitle(
            String text,
            Font font) {

        Paragraph title =
                new Paragraph(
                        text,
                        font
                );


        title.setSpacingAfter(
                5
        );


        return title;
    }


    // =====================================================
    // SIMPLE ROW
    // =====================================================

    private void addSimpleRow(

            PdfPCell parentCell,

            String label,

            String value,

            Font labelFont,

            Font valueFont) {


        Paragraph row =
                new Paragraph();


        row.setLeading(
                11
        );


        row.add(
                new Phrase(
                        label + " : ",
                        labelFont
                )
        );


        row.add(
                new Phrase(
                        value,
                        valueFont
                )
        );


        row.setSpacingAfter(
                2
        );


        parentCell.addElement(
                row
        );
    }


    // =====================================================
    // PLAIN TABLE ROW
    // =====================================================

    private void addPlainTableRow(

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


        labelCell.setBorder(
                Rectangle.NO_BORDER
        );


        labelCell.setPaddingTop(
                2
        );


        labelCell.setPaddingBottom(
                2
        );


        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                value,
                                valueFont
                        )
                );


        valueCell.setBorder(
                Rectangle.NO_BORDER
        );


        valueCell.setPaddingTop(
                2
        );


        valueCell.setPaddingBottom(
                2
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

        if (value == null
                || value.trim().isEmpty()) {

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

            return "-";
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd-MM-yyyy HH:mm"
                );

        return dateTime.format(
                formatter
        );
    }


    // =====================================================
    // FORMAT EVENT TIME
    // =====================================================

    private String formatEventTime(

            LocalTime startTime,

            LocalTime endTime) {

        if (startTime == null
                && endTime == null) {

            return "-";
        }


        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "hh:mm a"
                );


        if (startTime != null
                && endTime != null) {

            return startTime.format(formatter)
                    + " - "
                    + endTime.format(formatter);
        }


        if (startTime != null) {

            return startTime.format(
                    formatter
            );
        }


        return endTime.format(
                formatter
        );
    }

}