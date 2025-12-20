package com.pratham.livo.entity;

import com.pratham.livo.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "livo_user")
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    //we could have stored this as an array itself in postgres
    //but arrays are better when we don't have to search inside that array
    //here we will have to go through the roles attribute so array is not good
    //instead we should have a new table (user_id,role) where user_id is the fk
    //we can create that new table without defining an entity by using @ElementCollection
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(value = EnumType.STRING) //stores actual name of enum as string
    //EnumType.ORDINAL stores 0,1,2 etc. instead of actual name
    @ToString.Exclude
    private Set<Role> roles;
    //one to many mapping is done in the background
    //user_roles table will be created

    @CreationTimestamp
    @Column(updatable = false,nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
