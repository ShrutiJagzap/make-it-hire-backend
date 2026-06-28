package com.example.makeItHired.repository;

import com.example.makeItHired.entity.Notification;
import com.example.makeItHired.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrRecipientRoleOrderByCreatedAtDesc(Long userId, Role recipientRole);
}
