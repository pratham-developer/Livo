package com.pratham.livo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //declare many-to-one relationship with hotel
    //owner of the relationship (many side is the owner)
    //join column is brought here which is the primary key of inverse side(one) as the foreign key
    //here join column will be hotel_id
    //one room entry belongs to one hotel
    //and one hotel entry can have multiple room entries
    //a room entry will always have a hotel_id associated as the foreign key
    //cant be null
    //give reference with the hotel

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    @ToString.Exclude
    private Hotel hotel;
    //FETCH TYPE = LAZY means that when we fetch a room we don't fetch its hotel unless we do room.getHotel()
    //in many to one by default it is EAGER, where as soon as room is fetched, its hotel is also fetched

    @Column(nullable = false)
    private String type;

    @Column(precision = 10, scale = 2,nullable = false)
    private BigDecimal basePrice;

    @Column(columnDefinition = "TEXT[]")
    private List<String> photos;

    @Column(columnDefinition = "TEXT[]")
    private List<String> amenities;

    //total number of that type of rooms available in that hotel
    @Column(nullable = false)
    private Integer totalCount;

    //capacity of those rooms in that hotel
    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false; // Inherits from Hotel, or managed individually

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false; // Soft Delete flag

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
