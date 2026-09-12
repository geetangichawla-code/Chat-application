package com.chat.chat_app.controller;

import com.chat.chat_app.model.Message;
import com.chat.chat_app.model.ChatRoom;
import com.chat.chat_app.model.User;
import com.chat.chat_app.repository.MessageRepository;
import com.chat.chat_app.repository.ChatRoomRepository;
import com.chat.chat_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload Map<String, Object> payload) {
        try {
            String content = (String) payload.get("content");
            if (content == null || content.trim().isEmpty()) {
                return;
            }

            Map<String, Object> senderMap = (Map<String, Object>) payload.get("sender");
            if (senderMap == null || senderMap.get("id") == null) {
                return;
            }

            Number senderIdNumber = (Number) senderMap.get("id");
            Long senderId = senderIdNumber.longValue();

            ChatRoom room = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + senderId));

            Message message = new Message();
            message.setContent(content);
            message.setChatRoom(room);
            message.setSender(sender);
            message.setMessageType(Message.MessageType.CHAT);

            Message savedMessage = messageRepository.save(message);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, savedMessage);

        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/typing/{roomId}")
    public void typing(
            @DestinationVariable Long roomId,
            @Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/typing/" + roomId, (Object) payload);
    }

    @GetMapping("/api/messages/{roomId}")
    public List<Message> getMessages(@PathVariable Long roomId) {
        return messageRepository.findByChatRoomIdOrderBySentAtAsc(roomId);
    }

    @PostMapping("/api/rooms")
    public ChatRoom createRoom(@RequestBody ChatRoom room) {
        return chatRoomRepository.save(room);
    }

    @GetMapping("/api/rooms")
    public List<ChatRoom> getRooms() {
        return chatRoomRepository.findAll();
    }

    @DeleteMapping("/api/rooms/{roomId}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long roomId) {
        try {
            if (!chatRoomRepository.existsById(roomId)) {
                return ResponseEntity.notFound().build();
            }
            messageRepository.deleteAll(
                    messageRepository.findByChatRoomIdOrderBySentAtAsc(roomId)
            );
            chatRoomRepository.deleteById(roomId);
            return ResponseEntity.ok("Room deleted successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting room: " + e.getMessage());
        }
    }

}