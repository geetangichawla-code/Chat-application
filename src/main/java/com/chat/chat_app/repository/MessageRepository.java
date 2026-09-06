package com.chat.chat_app.repository;

import com.chat.chat_app.model.Message;
import com.chat.chat_app.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoomOrderBySentAtAsc(ChatRoom chatRoom);
    List<Message> findByChatRoomIdOrderBySentAtAsc(Long roomId);
}