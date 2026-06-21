package edu.fpt.sba301.bookstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "email", nullable = false)
    private String email;

    @Size(max = 255)
    @NotNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Size(max = 255)
    @NotNull
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'CUSTOMER'")
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "points", nullable = false)
    private Long points;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "lifetime_points", nullable = false)
    private Long lifetimePoints;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'SILVER'")
    @Column(name = "tier", nullable = false, length = 20)
    private String tier;

    @Size(max = 500)
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'LOCAL'")
    @Column(name = "auth_provider", nullable = false, length = 20)
    private String authProvider;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}