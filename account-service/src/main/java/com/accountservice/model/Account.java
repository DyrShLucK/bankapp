package com.accountservice.model;

import com.accountservice.enums.Currency;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Table
public class Account {
    @Id
    private String id;
    @Column("userName")
    private String userName;
    @Column("currency")
    private Currency currency;
    @Column("balance")
    private BigDecimal balance;
    @Column("isExists")
    private Boolean isExists;
}
