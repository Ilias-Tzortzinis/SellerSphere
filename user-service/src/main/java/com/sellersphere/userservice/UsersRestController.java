package com.sellersphere.userservice;

import com.sellersphere.userservice.data.UserCredentials;
import com.sellersphere.userservice.data.UserProfile;
import com.sellersphere.userservice.data.UserSignupVerification;
import com.sellersphere.userservice.jwt.AccessAndRefreshTokens;
import com.sellersphere.userservice.jwt.InvalidRefreshTokenException;
import com.sellersphere.userservice.logic.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
public final class UsersRestController {

    private final UserService userService;

    public UsersRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signupUser(@RequestBody @Valid UserCredentials credentials) {
        try {
            userService.signupUser(credentials);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (UserEmailAlreadyExistException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @PatchMapping("/verify/signup")
    public ResponseEntity<Void> verifySignup(@RequestBody @Valid UserSignupVerification verification) {
        if (userService.verifyUserSignup(verification)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    @PostMapping("/login")
    public ResponseEntity<UserProfile> loginUser(@RequestBody @Valid UserCredentials credentials) {
        return userService.loginUser(credentials).map(session -> {
            return ResponseEntity.status(HttpStatus.OK)
                    .header("X-ACCESS-TOKEN", session.accessToken())
                    .header("X-REFRESH-TOKEN", session.refreshToken())
                    .body(session.profile());
        }).orElseGet(() -> new ResponseEntity<>(HttpStatus.FORBIDDEN));
    }

    @GetMapping("/refreshTokens")
    public AccessAndRefreshTokens refreshTokens(@RequestParam String refreshToken){
        try {
            return userService.refreshTokens(refreshToken);
        } catch (InvalidRefreshTokenException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, null, e);
        }
    }

    @DeleteMapping("/logout")
    public void logoutUser(@RequestParam String refreshToken){
        try {
            userService.logoutUser(refreshToken);
        } catch (InvalidRefreshTokenException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, null, e);
        }
    }
}
