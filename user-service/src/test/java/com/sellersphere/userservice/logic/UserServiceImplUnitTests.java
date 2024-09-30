package com.sellersphere.userservice.logic;

import com.sellersphere.userservice.UserEmailAlreadyExistException;
import com.sellersphere.userservice.data.UserCredentials;
import com.sellersphere.userservice.jwt.UserJsonWebTokenService;
import com.sellersphere.userservice.repository.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplUnitTests {

    static final UserCredentials BOB = new UserCredentials("bob@bmail.com", "bobthebest");

    @Mock
    NotificationService notificationService;
    @Mock
    UsersRepository usersRepository;
    @Mock
    UserJsonWebTokenService jsonWebTokenService;
    @InjectMocks
    UserServiceImpl userService;

    @Test
    @DisplayName("On successful user signup a verification email is send to the user")
    void onSuccessfulUserSignupAVerificationEmailIsSendToTheUser() throws UserEmailAlreadyExistException {
        userService.signupUser(BOB);
        String[] verificationCodeHolder = new String[1];
        verify(usersRepository, times(1)).signupUser(eq(BOB), argThat(verificationCode -> {
            verificationCodeHolder[0] = verificationCode;
            return verificationCode.length() == 6;
        }));
        verify(notificationService).sendUserSignupVerificationMail(argThat(BOB.email()::equals),
                argThat(verificationCodeHolder[0]::equals));
    }




}