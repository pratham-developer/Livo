package com.pratham.livo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uniq_hotel_room_date",
                columnNames = {
                        "hotel_id","room_id","date"
                })
        },
        indexes = {
                //for searching hotels in a city b/w dates
                @Index(name = "idx_city_date",
                        columnList = "city, date"),

                //for searching available rooms in a hotel b/w dates
                @Index(name = "idx_hotel_date",
                        columnList = "hotel_id, date"
                )

        }
)
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inv_seq")
    @SequenceGenerator(name = "inv_seq", sequenceName = "inventory_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="hotel_id",nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer totalCount;

    @Column(nullable = false,columnDefinition = "INTEGER DEFAULT 0")
    private Integer bookedCount;

    @Column(nullable = false,precision = 5,scale = 2)
    private BigDecimal surgeFactor;

    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal price; //base_price*surge_factor

    @Column(nullable = false)
    private String city; //we are de-normalizing by storing city again to increase search speed by preventing join

    @Column(nullable = false)
    private Boolean closed;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
