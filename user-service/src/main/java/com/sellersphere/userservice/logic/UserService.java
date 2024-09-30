package com.sellersphere.userservice.logic;

import com.sellersphere.userservice.UserEmailAlreadyExistException;
import com.sellersphere.userservice.data.UserCredentials;
import com.sellersphere.userservice.data.UserLoginSession;
import com.sellersphere.userservice.data.UserSignupVerification;
import com.sellersphere.userservice.jwt.AccessAndRefreshTokens;
import com.sellersphere.userservice.jwt.InvalidRefreshTokenException;

import java.util.Optional;

public interface UserService {

    void signupUser(UserCredentials data) throws UserEmailAlreadyExistException;

    boolean verifyUserSignup(UserSignupVerification data);

    Optional<UserLoginSession> loginUser(UserCredentials credentials);

    AccessAndRefreshTokens refreshTokens(String refreshToken) throws InvalidRefreshTokenException;

    void logoutUser(String userRefreshToken) throws InvalidRefreshTokenException;
}
