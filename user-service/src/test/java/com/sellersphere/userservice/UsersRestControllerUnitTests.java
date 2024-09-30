package com.sellersphere.userservice;

import com.sellersphere.userservice.data.UserCredentials;
import com.sellersphere.userservice.data.UserSignupVerification;
import com.sellersphere.userservice.logic.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersRestControllerUnitTests {

    static final UserCredentials BOB = new UserCredentials("bob@bmail.com", "bobthebest");
    static final UserSignupVerification BOB_SIGNUP_VERIFICATION = new UserSignupVerification(BOB.email(), "123456");

    @Mock
    UserService userService;
    @InjectMocks
    UsersRestController controller;

    @Test
    @DisplayName("If a user is successfully created then the status code must be CREATED")
    void ifAUserIsSuccessfullyCreatedThenTheStatusCodeMustBeCreated() throws UserEmailAlreadyExistException {
        assertThat(controller.signupUser(BOB).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        verify(userService, times(1)).signupUser(BOB);
    }

    @Test
    @DisplayName("if signup was verified then return status code OK")
    void ifSignupWasVerifiedThenReturnStatusCodeOk() {
        when(userService.verifyUserSignup(BOB_SIGNUP_VERIFICATION)).thenReturn(true);

        assertThat(controller.verifySignup(BOB_SIGNUP_VERIFICATION).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("if signup verified fails then return status code CONFLICT")
    void ifSignupVerifiedFailsThenReturnStatusCodeConflict() {
        when(userService.verifyUserSignup(BOB_SIGNUP_VERIFICATION)).thenReturn(false);

        assertThat(controller.verifySignup(BOB_SIGNUP_VERIFICATION).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }


}