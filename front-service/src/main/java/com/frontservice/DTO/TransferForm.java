package com.frontservice.DTO;

import lombok.Data;

@Data
public class TransferForm {
    public String from_currency;
    public String to_currency;
    public double value;
}
