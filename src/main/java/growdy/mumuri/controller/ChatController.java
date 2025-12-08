package growdy.mumuri.controller;

import growdy.mumuri.domain.ChatMessage;
import growdy.mumuri.dto.*;
import growdy.mumuri.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageDto message) {
        ChatMessage saved = chatService.saveMessage(message);
        messagingTemplate.convertAndSend(
                "/topic/messages/" + message.getRoomId(),
                ChatMessageResponse.from(saved)
        );
    }

    @MessageMapping("/chat.read")
    public void readMessages(ReadRequestDto dto) {
        chatService.markAsRead(dto.getRoomId(), dto.getUserId());
        messagingTemplate.convertAndSend(
                "/topic/messages/read/" + dto.getRoomId(),
                new ReadStatusDto(dto.getUserId(), dto.getRoomId())
        );
    }
    @GetMapping("/chat/{roomId}/history")
    public ChatHistoryResponse getChatHistory(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") int size
    ) {
        return chatService.getHistory(roomId, cursor, size);
    }
}
