package com.example.makeItHired.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Specific user targeted (null if targeted to role instead)

    @Enumerated(EnumType.STRING)
    private Role recipientRole; // Target role: USER or ADMIN (null if targeted to specific user)

    private String title;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    private String type; // e.g., "APPLICATION", "INTERVIEW", "SYSTEM", "FEEDBACK"
    
    private boolean isRead = false;
    
    private String targetUrl;
    
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Notification() {}
}
