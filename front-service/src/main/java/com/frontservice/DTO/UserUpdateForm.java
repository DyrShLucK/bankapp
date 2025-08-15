package com.frontservice.DTO;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserUpdateForm {
    private String name;

    private LocalDate birthdate;

    private List<String> account = new ArrayList<>();

    @AssertTrue(message = "Вам должно быть больше 18 лет")
    public boolean isOlderThan18() {
        return birthdate != null &&
                LocalDate.now().minusYears(18).isAfter(birthdate);
    }
}
