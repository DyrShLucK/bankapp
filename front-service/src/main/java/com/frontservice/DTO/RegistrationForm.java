package com.frontservice.DTO;


import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Data;


import java.time.LocalDate;

@Data
public class RegistrationForm {
    @NotBlank(message = "Логин обязателен")
    private String login;

    @NotBlank(message = "Пароль обязателен")
    private String password;

    @NotBlank(message = "Подтверждение пароля обязательно")
    private String confirm_password;

    @NotBlank(message = "ФИО обязательно")
    private String name;

    @Past(message = "Дата рождения должна быть в прошлом")
    private LocalDate birthdate;

    @AssertTrue(message = "Пароли должны совпадать")
    public boolean isPasswordsMatch() {
        return password != null && password.equals(confirm_password);
    }

    @AssertTrue(message = "Вам должно быть больше 18 лет")
    public boolean isOlderThan18() {
        return birthdate != null &&
                LocalDate.now().minusYears(18).isAfter(birthdate);
    }
}
