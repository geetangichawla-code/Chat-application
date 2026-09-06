package com.chat.chat_app.controller;

import com.chat.chat_app.model.User;
import com.chat.chat_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RestController
public class PresenceController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    // Get all users with online status
    @GetMapping("/api/users")
    public List<UserStatusDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserStatusDTO(u.getId(), u.getUsername(), u.isOnline()))
                .collect(Collectors.toList());
    }

    // Update user online status
    @PostMapping("/api/users/{userId}/status")
    public void updateStatus(@PathVariable Long userId, @RequestParam boolean online) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(online);
            userRepository.save(user);
            // Broadcast to everyone
            messagingTemplate.convertAndSend("/topic/presence",
                    new UserStatusDTO(user.getId(), user.getUsername(), online));
        });
    }

    // Simple DTO class for user status
    public static class UserStatusDTO {
        public Long id;
        public String username;
        public boolean online;

        public UserStatusDTO(Long id, String username, boolean online) {
            this.id = id;
            this.username = username;
            this.online = online;
        }
    }
}