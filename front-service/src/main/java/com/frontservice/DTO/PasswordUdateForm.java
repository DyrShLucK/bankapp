package com.frontservice.DTO;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.time.LocalDate;
import java.util.Objects;

@Data
public class PasswordUdateForm {
    String password;
    String confirm_password;

    @AssertTrue(message = "Пароли должны совпадать")
    public boolean isPasswordsMatch() {
        return password != null && password.equals(confirm_password);
    }
}
