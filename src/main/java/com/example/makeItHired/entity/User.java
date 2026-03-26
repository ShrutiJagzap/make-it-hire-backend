package com.example.makeItHired.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    @Column(unique = true)
    private String email;
    @Column(unique = true)
    private String password; //store hashed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;//ROLE_USER, ROLE_ADMIN

    private String phone;
    private String title;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "id_photo_url")
    private String idPhotoUrl;

}
