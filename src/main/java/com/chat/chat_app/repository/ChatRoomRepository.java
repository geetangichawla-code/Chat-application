package com.chat.chat_app.repository;

import com.chat.chat_app.model.ChatRoom;
import com.chat.chat_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByMembersContaining(User user);
    Optional<ChatRoom> findByName(String name);
}