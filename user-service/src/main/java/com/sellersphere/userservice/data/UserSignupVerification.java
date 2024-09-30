package com.sellersphere.userservice.data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record UserSignupVerification(@NotNull @Email String email,
                                     @NotNull @Length(min = 6, max = 6) String verificationCode) {
}
