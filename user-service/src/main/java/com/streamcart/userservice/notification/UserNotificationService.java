package com.streamcart.userservice.notification;

import com.streamcart.userservice.security.JsonWebTokenService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public final class UserNotificationService {

    private final MailSender mailSender;
    private final ObservationRegistry observationRegistry;
    private final SimpleMailMessage verificationMailTemplate;

    public UserNotificationService(MailSender mailSender, ObservationRegistry observationRegistry) {
        this.mailSender = mailSender;
        this.observationRegistry = observationRegistry;
        this.verificationMailTemplate = new SimpleMailMessage();
        verificationMailTemplate.setSubject("StreamCart Account Verification");
        verificationMailTemplate.setFrom("info@streamcart.com");
    }

    public void sendVerificationMail(String userMail, String verificationCode) {
        var mail = new SimpleMailMessage(verificationMailTemplate);
        mail.setTo(userMail);
        mail.setText("Thanks you for using StreamCart, VerificationCode: ".concat(verificationCode));

        Observation.start("user-service.notification-service.sendVerificationMail", observationRegistry)
                .lowCardinalityKeyValue("userEmail", userMail)
                .observe(() -> mailSender.send(mail));
    }
}
