package com.pratham.livo.entity;

import com.pratham.livo.enums.Role;
import com.pratham.livo.enums.RoleRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "role_request",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uniq_user_role",
                        columnNames = {"user_id", "role"}
                )
        },
        indexes = {
                @Index(name = "idx_role_request_status", columnList = "status")
        }
)
public class RoleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleRequestStatus status = RoleRequestStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    

    @Version
    private Long version;
}
