package cl.textilcircular.api.feature.messaging;

import cl.textilcircular.api.core.model.messaging.Message;
import cl.textilcircular.api.core.model.messaging.ReportReason;
import cl.textilcircular.api.core.repository.InboxItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Request and response bodies of the messaging endpoints, as named in the API contract. */
final class MessagingDtos {

    private MessagingDtos() {
    }

    record OpenConversationRequest(@JsonProperty(required = true) Long businessId) {
    }

    record SendMessageRequest(@JsonProperty(required = true) UUID clientUuid,
                              @JsonProperty(required = true) String content) {
    }

    record ReportRequest(@JsonProperty(required = true) ReportReason reason, String detail, Long messageId) {
    }

    @Schema(name = "Conversation")
    record ConversationView(long id, long businessId, String businessName, String counterpartName,
                            String lastMessage, Instant lastSentAt, Long lastSenderId, long unreadCount) {

        static ConversationView of(InboxItem item, long viewerId) {
            return new ConversationView(item.getConversationId(), item.getBusinessId(), item.getBusinessName(),
                    item.counterpartName(viewerId), item.getLastMessage(), item.getLastMessageAt(),
                    item.getLastSenderId(), item.getUnreadCount());
        }
    }

    @Schema(name = "Message")
    record MessageView(long id, long conversationId, UUID clientUuid, long senderId, String content,
                       Instant sentAt, Instant readAt) {

        static MessageView of(Message message) {
            return new MessageView(message.getId(), message.getConversation().getId(), message.getClientUuid(),
                    message.getSender().getId(), message.getContent(), message.getSentAt(), message.getReadAt());
        }
    }

    /** {@code message.created} event: the contract's Message plus the name the app shows. */
    record MessageCreated(long id, long conversationId, UUID clientUuid, long senderId, String content,
                          Instant sentAt, Instant readAt, String senderName) {

        static MessageCreated of(Message message, String senderName) {
            return new MessageCreated(message.getId(), message.getConversation().getId(), message.getClientUuid(),
                    message.getSender().getId(), message.getContent(), message.getSentAt(), message.getReadAt(),
                    senderName);
        }
    }

    /** {@code message.read} event. */
    record MessageRead(long conversationId, Instant readAt) {
    }
}
