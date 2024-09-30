package com.sellersphere.userservice.repository;

import com.sellersphere.userservice.UserEmailAlreadyExistException;
import com.sellersphere.userservice.data.UserCredentials;
import com.sellersphere.userservice.data.UserLoginSession;
import com.sellersphere.userservice.data.UserProfile;
import com.sellersphere.userservice.data.UserSignupVerification;

import java.util.Optional;

public interface UsersRepository {

    void signupUser(UserCredentials credentials, String verificationCode) throws UserEmailAlreadyExistException;

    boolean verifySignupUser(UserSignupVerification signupVerification);

    Optional<UserProfile> loginUser(UserCredentials credentials);
}
