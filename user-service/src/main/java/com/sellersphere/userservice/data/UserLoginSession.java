package com.sellersphere.userservice.data;

public record UserLoginSession(UserProfile profile, String accessToken, String refreshToken) {
}
