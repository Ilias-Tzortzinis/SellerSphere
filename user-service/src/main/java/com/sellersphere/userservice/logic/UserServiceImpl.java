package com.sellersphere.userservice.logic;

import com.sellersphere.userservice.UserEmailAlreadyExistException;
import com.sellersphere.userservice.data.UserCredentials;
import com.sellersphere.userservice.data.UserLoginSession;
import com.sellersphere.userservice.data.UserSignupVerification;
import com.sellersphere.userservice.jwt.AccessAndRefreshTokens;
import com.sellersphere.userservice.jwt.InvalidRefreshTokenException;
import com.sellersphere.userservice.jwt.UserJsonWebTokenService;
import com.sellersphere.userservice.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Optional;

@Service
public final class UserServiceImpl implements UserService {

    private final SecureRandom secureRandom;
    private final UsersRepository usersRepository;
    private final NotificationService notificationService;
    private final UserJsonWebTokenService jsonWebTokenService;

    public UserServiceImpl(UsersRepository usersRepository,
                           NotificationService notificationService,
                           UserJsonWebTokenService jsonWebTokenService) {
        this.secureRandom = new SecureRandom();
        this.usersRepository = usersRepository;
        this.notificationService = notificationService;
        this.jsonWebTokenService = jsonWebTokenService;
    }

    @Override
    public void signupUser(UserCredentials user) throws UserEmailAlreadyExistException {
        var verificationCode = String.valueOf(secureRandom.nextInt(111_111, 999_999));
        usersRepository.signupUser(user, verificationCode);
        notificationService.sendUserSignupVerificationMail(user.email(), verificationCode);
    }

    @Override
    public boolean verifyUserSignup(UserSignupVerification data) {
        return usersRepository.verifySignupUser(data);
    }

    @Override
    public Optional<UserLoginSession> loginUser(UserCredentials credentials) {
        return usersRepository.loginUser(credentials).map(profile -> {
            var session = jsonWebTokenService.createUserSession(profile.userId());
            return new UserLoginSession(profile, session.accessToken(), session.refreshToken());
        });
    }

    @Override
    public AccessAndRefreshTokens refreshTokens(String refreshToken) throws InvalidRefreshTokenException {
        return jsonWebTokenService.refreshTokens(refreshToken);
    }

    @Override
    public void logoutUser(String userRefreshToken) throws InvalidRefreshTokenException {
        jsonWebTokenService.deleteUserSession(userRefreshToken);
    }

}
