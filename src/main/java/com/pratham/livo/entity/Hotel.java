package com.pratham.livo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        indexes = {
                //for searching hotel by owner id
                @Index(name = "idx_hotel_owner", columnList = "owner_id")
        }
)
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    //we can store array as a column in PostgresSQL
    //stores links of photos
    @Column(columnDefinition = "TEXT[]")
    private List<String> photos;

    @Column(columnDefinition = "TEXT[]")
    private List<String> amenities;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    //rows will be retrieved from this class
    @Embedded
    private HotelContactInfo contactInfo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false; // "Draft" vs "Live"

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false; // "Existing" vs "Permanently Closed"

    //inverse side for bidirectional relationship
    //mappedBy tells jpa that, this is the inverse side, and it is not storing the foreign key
    //actually this is mapped using the field "hotel" in the room entity which is the reference there

    /*
    @OneToMany(mappedBy = "hotel")
    private List<Room> rooms;
    //by default lazy fetch because a hotel can have multiple rooms

    @OneToMany(mappedBy = "hotel")
    private List<Inventory> inventories;
     */

    @ManyToOne //M:1 mapping for hotel-user(owner)
    @ToString.Exclude
    private User owner;


}
