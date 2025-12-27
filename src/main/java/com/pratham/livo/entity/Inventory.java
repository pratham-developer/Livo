package com.pratham.livo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@DynamicUpdate //hibernate generates update query only with modified columns
@Getter
@Setter
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
                        columnList = "hotel_id, date"),

                //for searching inventory by room and dates
                @Index(name = "idx_room_date",
                        columnList = "room_id, date")
        }
)
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inv_seq")
    @SequenceGenerator(name = "inv_seq", sequenceName = "inventory_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="hotel_id",nullable = false)
    @ToString.Exclude
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",nullable = false)
    @ToString.Exclude
    private Room room;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer totalCount;

    @Column(nullable = false,columnDefinition = "INTEGER DEFAULT 0")
    private Integer bookedCount;

    @Column(nullable = false,columnDefinition = "INTEGER DEFAULT 0")
    private Integer reservedCount;

    @Column(nullable = false,precision = 5,scale = 2)
    private BigDecimal surgeFactor; //for manual hike

    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal price; //calculated using dynamic pricing strategy

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
