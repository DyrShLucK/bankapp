package com.frontservice.DTO;

import lombok.Data;

@Data
public class TransferForm {
    private String from_currency;
    private String to_currency;
    private Double value;
    private String to_login;
}
