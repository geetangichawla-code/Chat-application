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
import org.springframework.web.bind.annotation.*;

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


    // =========================
    // SEND MESSAGE
    // =========================

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload Map<String, Object> payload) {

        try {

            // Get message content
            String content = (String) payload.get("content");

            if (content == null || content.trim().isEmpty()) {
                System.out.println("Message content is empty");
                return;
            }


            // Get sender information
            Map<String, Object> senderMap =
                    (Map<String, Object>) payload.get("sender");

            if (senderMap == null || senderMap.get("id") == null) {
                System.out.println("Sender information is missing");
                return;
            }


            // Safely convert sender ID
            Number senderIdNumber =
                    (Number) senderMap.get("id");

            Long senderId =
                    senderIdNumber.longValue();


            // Find room
            ChatRoom room =
                    chatRoomRepository
                            .findById(roomId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Room not found: " + roomId
                                    )
                            );


            // Find sender
            User sender =
                    userRepository
                            .findById(senderId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "User not found: " + senderId
                                    )
                            );


            // Create message
            Message message = new Message();

            message.setContent(content);
            message.setChatRoom(room);
            message.setSender(sender);
            message.setMessageType(
                    Message.MessageType.CHAT
            );


            // Save message to database
            Message savedMessage =
                    messageRepository.save(message);


            // Send message to everyone in the room
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    savedMessage
            );


        } catch (Exception e) {

            System.out.println(
                    "Error sending message: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // =========================
    // GET MESSAGES
    // =========================

    @GetMapping("/api/messages/{roomId}")
    public List<Message> getMessages(
            @PathVariable Long roomId) {

        return messageRepository
                .findByChatRoomIdOrderBySentAtAsc(roomId);
    }


    // =========================
    // CREATE ROOM
    // =========================

    @PostMapping("/api/rooms")
    public ChatRoom createRoom(
            @RequestBody ChatRoom room) {

        return chatRoomRepository.save(room);
    }


    // =========================
    // GET ALL ROOMS
    // =========================

    @GetMapping("/api/rooms")
    public List<ChatRoom> getRooms() {

        return chatRoomRepository.findAll();
    }


    // =========================
    // DELETE ROOM
    // =========================

    @DeleteMapping("/api/rooms/{roomId}")
    public ResponseEntity<?> deleteRoom(
            @PathVariable Long roomId) {

        try {

            // Check if room exists
            if (!chatRoomRepository.existsById(roomId)) {

                return ResponseEntity
                        .notFound()
                        .build();
            }


            // Delete messages belonging to room
            messageRepository.deleteAll(
                    messageRepository
                            .findByChatRoomIdOrderBySentAtAsc(
                                    roomId
                            )
            );


            // Delete room
            chatRoomRepository.deleteById(roomId);


            return ResponseEntity.ok(
                    "Room deleted successfully!"
            );


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Error deleting room: "
                                    + e.getMessage()
                    );
        }
    }
}