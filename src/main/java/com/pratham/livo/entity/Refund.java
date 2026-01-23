package com.pratham.livo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String razorpayRefundId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id",nullable = false,unique = true)
    @ToString.Exclude
    private Payment payment;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String refundStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
