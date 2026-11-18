package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.messaging.Conversation;

import java.time.Instant;

/** Una fila de la bandeja de un usuario. La API toma de aquí el nombre de la contraparte según quién la ve. */
public interface InboxItem {

    Long getConversationId();

    Long getBusinessId();

    String getBusinessName();

    Long getBusinessOwnerId();

    Long getInitiatorId();

    String getInitiatorAlias();

    /** Nombre alternativo que se muestra cuando la contraparte no tiene alias; {@code null} cuando no hay perfil. */
    String getInitiatorFirstName();

    /** Nombre crudo del estado, para que la proyección también funcione con consultas nativas. */
    String getInitiatorStatus();

    /** {@code null} para una conversación aún sin mensajes. */
    String getLastMessage();

    Instant getLastMessageAt();

    Long getLastSenderId();

    long getUnreadCount();

    /** La misma regla que {@link Conversation#counterpartName}; se mantiene aquí para la proyección. */
    default String counterpartName(long viewerId) {
        AccountStatus status = getInitiatorStatus() == null ? null : AccountStatus.valueOf(getInitiatorStatus());
        return Conversation.counterpartName(viewerId, getInitiatorId(), getBusinessName(), getInitiatorAlias(),
                getInitiatorFirstName(), status);
    }
}
