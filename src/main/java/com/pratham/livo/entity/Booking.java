package com.pratham.livo.entity;


import com.pratham.livo.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        indexes = {
                //for cleaning expired bookings
                @Index(name = "idx_booking_status_time", columnList = "bookingStatus, updatedAt"),
                //for searching by user and booking status
                @Index(name = "idx_booking_user_status", columnList = "user_id, bookingStatus")
        }

)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id",nullable = false)
    @ToString.Exclude
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",nullable = false)
    @ToString.Exclude
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false)
    private Integer roomsCount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private BookingStatus bookingStatus;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<Guest> guests;

    @Version //optimistic concurrency control b/w booking cleanup job and booking by user
    @Column(nullable = false)
    private Long version;
}
