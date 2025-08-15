package com.frontservice.DTO;

import lombok.Data;

@Data
public class CashForm {
    String currency;
    Double value;
    String action;
}
