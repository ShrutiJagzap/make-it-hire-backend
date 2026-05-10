package com.example.makeItHired.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name ="resumes")
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String filename;
    private String filepath;
    private Long userId;
    @Column(columnDefinition = "TEXT")  // This works for both MySQL and PostgreSQL
    private String parsedJson; //store parser output
    private LocalDateTime uploadedAt;
    private String storageType; // "firebase" or "local"

}