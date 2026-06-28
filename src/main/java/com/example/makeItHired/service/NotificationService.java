package com.example.makeItHired.service;

import com.example.makeItHired.entity.Notification;
import com.example.makeItHired.entity.Role;
import com.example.makeItHired.entity.User;
import com.example.makeItHired.repository.NotificationRepository;
import com.example.makeItHired.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification createNotification(Long userId, Role recipientRole, String title, String message, String type, String targetUrl) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setRecipientRole(recipientRole);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setTargetUrl(targetUrl);
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            Role role = userOpt.get().getRole();
            return notificationRepository.findByUserIdOrRecipientRoleOrderByCreatedAtDesc(userId, role);
        }
        return notificationRepository.findByUserIdOrRecipientRoleOrderByCreatedAtDesc(userId, null);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Optional<Notification> notifOpt = notificationRepository.findById(notificationId);
        if (notifOpt.isPresent()) {
            Notification notif = notifOpt.get();
            notif.setRead(true);
            return notificationRepository.save(notif);
        }
        throw new RuntimeException("Notification not found");
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = getNotificationsForUser(userId);
        for (Notification notif : notifications) {
            if (!notif.isRead()) {
                notif.setRead(true);
                notificationRepository.save(notif);
            }
        }
    }
}
