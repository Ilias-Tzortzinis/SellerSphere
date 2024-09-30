package com.sellersphere.userservice.jwt;

public interface UserJsonWebTokenService {

    AccessAndRefreshTokens createUserSession(String userId);


    AccessAndRefreshTokens refreshTokens(String refreshToken) throws InvalidRefreshTokenException;

    void deleteUserSession(String refreshToken) throws InvalidRefreshTokenException;
}
