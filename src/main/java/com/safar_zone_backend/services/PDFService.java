package com.safar_zone_backend.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PDFService {

    private final TemplateEngine templateEngine;

    public byte[] generateTicket(
            Map<String, Object> data
    ) throws Exception {

        // =====================================================
        // QR
        // =====================================================

        QRCodeWriter qrCodeWriter =
                new QRCodeWriter();

        var bitMatrix =
                qrCodeWriter.encode(
                        data.get("bookingCode").toString(),
                        BarcodeFormat.QR_CODE,
                        250,
                        250
                );

        ByteArrayOutputStream qrOut =
                new ByteArrayOutputStream();

        javax.imageio.ImageIO.write(
                com.google.zxing.client.j2se.MatrixToImageWriter
                        .toBufferedImage(bitMatrix),
                "PNG",
                qrOut
        );

        String qrBase64 =
                Base64.getEncoder()
                        .encodeToString(
                                qrOut.toByteArray()
                        );

        data.put(
                "qrCode",
                "data:image/png;base64," + qrBase64
        );

        // =====================================================
        // HTML
        // =====================================================

        Context context =
                new Context();

        context.setVariables(data);

        String html =
                templateEngine.process(
                        "ticket-template",
                        context
                );

        // =====================================================
        // PDF
        // =====================================================

        ByteArrayOutputStream pdfOut =
                new ByteArrayOutputStream();

        PdfRendererBuilder builder =
                new PdfRendererBuilder();

        builder.withHtmlContent(
                html,
                null
        );

        builder.toStream(pdfOut);

        builder.run();

        return pdfOut.toByteArray();
    }
}