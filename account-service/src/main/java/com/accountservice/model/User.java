package com.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private Long id;
    @Column("username")
    private String username;
    @Column("password")
    private String password;
    @Column("name")
    private String name;
    @Column("birthday")
    private LocalDate birthday;
    @Column("role")
    private String role;

    public User(String username, String password, String name, LocalDate birthday, String role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.birthday = birthday;
        this.role = role;
    }

}
