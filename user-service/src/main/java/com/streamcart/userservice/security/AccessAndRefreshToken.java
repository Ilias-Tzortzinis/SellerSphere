package com.streamcart.userservice.security;

public record AccessAndRefreshToken(String accessToken, String refreshToken) {
}
