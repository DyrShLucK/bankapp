CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    birthday DATE,
    role VARCHAR(255)
    );
CREATE TABLE IF NOT EXISTS account (
                                       id BIGSERIAL PRIMARY KEY,
                                       username VARCHAR(255) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    balance NUMERIC(19, 2) DEFAULT 0.00,
    isexists BOOLEAN NOT NULL
    );