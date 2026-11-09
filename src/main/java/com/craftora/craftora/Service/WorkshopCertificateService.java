
package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.EventRegistration;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.EventRegistrationRepository;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Repository.UserRepository;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class WorkshopCertificateService {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public WorkshopCertificateService(
            EventRegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository) {

        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public byte[] generateCertificate(
            Long registrationId, Long customerId) throws Exception {

        EventRegistration registration =
                registrationRepository.findById(registrationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Registration not found."));

        if (!"CUSTOMER".equalsIgnoreCase(
                registration.getRegistrationRole())) {
            throw new IllegalArgumentException(
                    "Certificates are available for customers only.");
        }

        if (!registration.getUserId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "You cannot download another customer's certificate.");
        }

        if (!"ATTENDED".equalsIgnoreCase(
                registration.getAttendanceStatus())) {
            throw new IllegalArgumentException(
                    "Attendance must be confirmed first.");
        }

        // Do not check certificateAvailable here.
        // The controller checks whether the customer can access
        // the certificate. This allows repeat downloads.

        Event workshop = eventRepository.findById(
                registration.getEventId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Workshop not found."));

        if (!"WORKSHOP".equalsIgnoreCase(workshop.getEventType())) {
            throw new IllegalArgumentException(
                    "Certificates are available for workshops only.");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found."));

        if (registration.getCertificateId() == null
                || registration.getCertificateId().isBlank()) {

            registration.setCertificateId(
                    "CRAFTORA-" + UUID.randomUUID()
                            .toString().substring(0, 8).toUpperCase());

            registrationRepository.save(registration);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Document document = new Document(
                PageSize.A4.rotate(), 55, 55, 40, 40);

        PdfWriter writer = PdfWriter.getInstance(document, output);
        writer.setPageEvent(new CertificateBorder());

        document.open();

        Color darkGreen = new Color(29, 72, 53);
        Color gold = new Color(174, 137, 57);
        Color darkText = new Color(45, 48, 42);

        Font brandFont = FontFactory.getFont(
                FontFactory.TIMES_BOLD, 34, darkGreen);

        Font titleFont = FontFactory.getFont(
                FontFactory.TIMES_BOLD, 21, gold);

        Font bodyFont = FontFactory.getFont(
                FontFactory.TIMES_ROMAN, 14, darkText);

        Font nameFont = FontFactory.getFont(
                FontFactory.TIMES_BOLD, 29, darkGreen);

        Font workshopFont = FontFactory.getFont(
                FontFactory.TIMES_BOLD, 19, darkGreen);

        Font signatureFont = FontFactory.getFont(
                FontFactory.TIMES_ITALIC, 17, darkGreen);

        Font smallFont = FontFactory.getFont(
                FontFactory.HELVETICA, 9, darkText);

        Paragraph brand = new Paragraph("CRAFTORA", brandFont);
        brand.setAlignment(Element.ALIGN_CENTER);
        brand.setSpacingBefore(8);
        document.add(brand);

        Paragraph tagline = new Paragraph(
                "Handicraft Development and Community Management",
                smallFont);
        tagline.setAlignment(Element.ALIGN_CENTER);
        tagline.setSpacingBefore(3);
        document.add(tagline);

        Paragraph title = new Paragraph(
                "CERTIFICATE OF WORKSHOP COMPLETION", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(24);
        document.add(title);

        Paragraph presented = new Paragraph(
                "This is to certify that", bodyFont);
        presented.setAlignment(Element.ALIGN_CENTER);
        presented.setSpacingBefore(22);
        document.add(presented);

        Paragraph customerName = new Paragraph(
                customer.getFullName(), nameFont);
        customerName.setAlignment(Element.ALIGN_CENTER);
        customerName.setSpacingBefore(13);
        customerName.setSpacingAfter(13);
        document.add(customerName);

        Paragraph completion = new Paragraph(
                "has successfully completed the workshop", bodyFont);
        completion.setAlignment(Element.ALIGN_CENTER);
        document.add(completion);

        Paragraph workshopName = new Paragraph(
                workshop.getEventTitle(), workshopFont);
        workshopName.setAlignment(Element.ALIGN_CENTER);
        workshopName.setSpacingBefore(14);
        workshopName.setSpacingAfter(10);
        document.add(workshopName);

        String eventDate = workshop.getEventDate() != null
                ? workshop.getEventDate().format(
                        DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "Date not specified";

        Paragraph date = new Paragraph(
                "Conducted by Craftora on " + eventDate, bodyFont);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingBefore(5);
        document.add(date);

        Paragraph certificateId = new Paragraph(
                "Certificate ID: " + registration.getCertificateId(),
                smallFont);
        certificateId.setAlignment(Element.ALIGN_CENTER);
        certificateId.setSpacingBefore(12);
        document.add(certificateId);

        Paragraph signature = new Paragraph(
                "Vidusha Nanayakkara", signatureFont);
        signature.setAlignment(Element.ALIGN_RIGHT);
        signature.setIndentationRight(70);
        signature.setSpacingBefore(25);
        document.add(signature);

        Paragraph signatureLine = new Paragraph(
                "________________________________", smallFont);
        signatureLine.setAlignment(Element.ALIGN_RIGHT);
        signatureLine.setIndentationRight(40);
        document.add(signatureLine);

        Paragraph authorized = new Paragraph(
                "Authorized Administrator", smallFont);
        authorized.setAlignment(Element.ALIGN_RIGHT);
        authorized.setIndentationRight(65);
        document.add(authorized);

        Paragraph footer = new Paragraph(
                "CRAFTORA | Preserving Traditional Skills",
                smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(15);
        document.add(footer);

        document.close();

        return output.toByteArray();
    }

    private static class CertificateBorder extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {

            PdfContentByte canvas = writer.getDirectContent();
            Rectangle page = document.getPageSize();

            canvas.saveState();

            canvas.setColorStroke(new Color(29, 72, 53));
            canvas.setLineWidth(5f);
            canvas.rectangle(
                    page.getLeft() + 15,
                    page.getBottom() + 15,
                    page.getWidth() - 30,
                    page.getHeight() - 30);
            canvas.stroke();

            canvas.setColorStroke(new Color(174, 137, 57));
            canvas.setLineWidth(1.5f);
            canvas.rectangle(
                    page.getLeft() + 23,
                    page.getBottom() + 23,
                    page.getWidth() - 46,
                    page.getHeight() - 46);
            canvas.stroke();

            canvas.restoreState();
        }
    }
}
