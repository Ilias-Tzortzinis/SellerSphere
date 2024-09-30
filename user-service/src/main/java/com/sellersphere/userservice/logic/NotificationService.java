package com.sellersphere.userservice.logic;

public interface NotificationService {

    void sendUserSignupVerificationMail(String userEmail, String verificationCode);

}
