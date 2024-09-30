package com.sellersphere.userservice.logic;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

@Component
public final class NotificationServiceImpl implements NotificationService {
    private final String from;
    private final MailSender mailSender;

    public NotificationServiceImpl(MailSender mailSender,
                                   @Value("${notification.mail.from:info@seller-sphere.com}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendUserSignupVerificationMail(String userEmail, String verificationCode) {
        var mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(userEmail);
        mail.setSubject("Signup Verification");
        mail.setText("Thanks you for signup in SellerSphere, VerificationCode: ".concat(verificationCode));

        mailSender.send(mail);
    }
}
