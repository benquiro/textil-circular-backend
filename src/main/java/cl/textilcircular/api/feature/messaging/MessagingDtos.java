package cl.textilcircular.api.feature.messaging;

import cl.textilcircular.api.core.model.messaging.Message;
import cl.textilcircular.api.core.model.messaging.ReportReason;
import cl.textilcircular.api.core.repository.InboxItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Cuerpos de petición y respuesta de los endpoints de mensajería, con los nombres del contrato de la API. */
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

    /** Evento {@code message.created}: el Message del contrato más el nombre que muestra la app. */
    record MessageCreated(long id, long conversationId, UUID clientUuid, long senderId, String content,
                          Instant sentAt, Instant readAt, String senderName) {

        static MessageCreated of(Message message, String senderName) {
            return new MessageCreated(message.getId(), message.getConversation().getId(), message.getClientUuid(),
                    message.getSender().getId(), message.getContent(), message.getSentAt(), message.getReadAt(),
                    senderName);
        }
    }

    /** Evento {@code message.read}. */
    record MessageRead(long conversationId, Instant readAt) {
    }
}
