package com.safar_zone_backend.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendTicketEmail(
            String to,
            String travelerName,
            byte[] pdf
    ) throws Exception {

        MimeMessage message =
                mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(
                        message,
                        true
                );

        helper.setTo(to);

        helper.setSubject(
                "🎟 Your Safar Zone Ticket"
        );

        helper.setText(
                """
                Hello %s,

                Your booking has been confirmed.

                Your premium ticket is attached.

                Thank you for choosing Safar Zone ❤️
                """
                        .formatted(travelerName)
        );

        helper.addAttachment(
                "SafarZoneTicket.pdf",
                new ByteArrayResource(pdf)
        );

        mailSender.send(message);
    }
}