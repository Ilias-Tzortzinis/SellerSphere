package com.streamcart.userservice;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.streamcart.userservice.notification.UserNotificationService;
import com.streamcart.userservice.security.InvalidRefreshTokenException;
import com.streamcart.userservice.security.JsonWebTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
public final class UsersRestController {

    private final UserService userService;
    private final JsonWebTokenService jwtService;
    private final UserNotificationService userNotificationService;

    public UsersRestController(UserService userService, JsonWebTokenService jwtService, UserNotificationService userNotificationService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.userNotificationService = userNotificationService;
    }

    @PostMapping("/register")
    public void registerUser(@RequestBody @Valid UserCredentials credentials){
        try {
            var verificationCode = userService.registerUser(credentials);
            userNotificationService.sendVerificationMail(credentials.email(), verificationCode);
        } catch (UserAlreadyExistsException exc) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, null, exc);
        }
    }

    @PatchMapping("/verify")
    public ResponseEntity<Void> verifyUserAccount(@RequestBody @Valid AccountVerificationPayload payload){
        if (userService.verifyUserAccount(payload)){
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    @PostMapping("/login")
    public ResponseEntity<UserProfile> loginUser(@RequestBody @Valid UserCredentials credentials){
        return userService.loginUser(credentials).map(userProfile -> {
            var tokens = jwtService.createNewSession(userProfile.userId());
            return ResponseEntity.ok()
                    .header("X-ACCESS-TOKEN", tokens.accessToken())
                    .header("X-REFRESH-TOKEN", tokens.refreshToken())
                    .body(userProfile);
        }).orElseGet(() -> new ResponseEntity<>(HttpStatus.FORBIDDEN));
    }

    @DeleteMapping("/logout")
    public void logoutUser(@RequestHeader("X-REFRESH-TOKEN") String refreshToken){
        try {
            jwtService.deleteSession(refreshToken);
        }
        catch (JWTVerificationException e){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, null, e);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshTokens(@RequestHeader("X-REFRESH-TOKEN") String refreshToken){
        try {
            var tokens = jwtService.refreshTokens(refreshToken);
            return ResponseEntity.ok()
                    .header("X-ACCESS-TOKEN", tokens.accessToken())
                    .header("X-REFRESH-TOKEN", tokens.refreshToken())
                    .build();
        }
        catch (InvalidRefreshTokenException e){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, null, e);
        }

    }

}
