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
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'PENDING'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @NotNull
    @Column(name = "subtotal", nullable = false)
    private Long subtotal;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "discount", nullable = false)
    private Long discount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "shipping_fee", nullable = false)
    private Long shippingFee;

    @NotNull
    @Column(name = "total", nullable = false)
    private Long total;

    @NotNull
    @Column(name = "address_snapshot", nullable = false, length = Integer.MAX_VALUE)
    private String addressSnapshot;

    @Size(max = 20)
    @NotNull
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Size(max = 50)
    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "points_used", nullable = false)
    private Long pointsUsed;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "points_earned", nullable = false)
    private Long pointsEarned;

    @Size(max = 100)
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}