package com.accountservice.enums;

public enum Currency {
    RUB("Российский рубль"),
    USD("Доллар США"),
    EUR("Евро");

    private final String title;

    Currency(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
