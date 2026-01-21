package com.pratham.livo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    @Column(nullable = false,unique = true)
    private String familyId;

    @Column(unique = true, nullable = false)
    private String refreshTokenHash;

    @Column(unique = true, nullable = false)
    private String jti;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime lastUsedAt = LocalDateTime.now();

    @Version
    private Long version;
}
