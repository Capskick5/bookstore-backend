package edu.fpt.sba301.bookstore.entity;

import edu.fpt.sba301.bookstore.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Account {
    @Id
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    Role role;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
}
