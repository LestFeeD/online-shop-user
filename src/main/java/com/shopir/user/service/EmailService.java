package com.shopir.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class EmailService implements EmailSender {
    private final JavaMailSender mailSender;
    private final Executor taskExecutor;

    @Autowired
    public EmailService(JavaMailSender mailSender, Executor taskExecutor) {
        this.mailSender = mailSender;
        this.taskExecutor = taskExecutor;
    }

    @Override
    @Async
    public CompletableFuture<Void> send(String to, String email) {
        return CompletableFuture.runAsync(() -> {

            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, "utf-8");
                messageHelper.setText(email, true);
                messageHelper.setTo(to);
                messageHelper.setSubject("Confirm your email");
                messageHelper.setFrom("axella50506@gmail.com");
                mailSender.send(mimeMessage);
            } catch (MessagingException e) {
                throw new IllegalArgumentException("failed to send email");
            }
        }, taskExecutor);
    }
}
