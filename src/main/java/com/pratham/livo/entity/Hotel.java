package com.pratham.livo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

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

    //for showing inactive
    @Column(nullable = false)
    private Boolean active;

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


}
