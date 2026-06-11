package edu.fpt.sba301.bookstore.entity;

import edu.fpt.sba301.bookstore.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public class Account {
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    Role role;
}
