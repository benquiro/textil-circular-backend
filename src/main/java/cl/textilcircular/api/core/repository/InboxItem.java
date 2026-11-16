package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.messaging.Conversation;

import java.time.Instant;

/** One row of a user's inbox. The API picks the counterpart name from it depending on the viewer. */
public interface InboxItem {

    Long getConversationId();

    Long getBusinessId();

    String getBusinessName();

    Long getBusinessOwnerId();

    Long getInitiatorId();

    String getInitiatorAlias();

    /** Fallback shown when the counterpart has no alias; {@code null} when there is no profile. */
    String getInitiatorFirstName();

    /** Raw status name, so the projection also works with native queries. */
    String getInitiatorStatus();

    /** {@code null} for a conversation without messages yet. */
    String getLastMessage();

    Instant getLastMessageAt();

    Long getLastSenderId();

    long getUnreadCount();

    /** Same rule as {@link Conversation#counterpartName}; kept here for the projection. */
    default String counterpartName(long viewerId) {
        AccountStatus status = getInitiatorStatus() == null ? null : AccountStatus.valueOf(getInitiatorStatus());
        return Conversation.counterpartName(viewerId, getInitiatorId(), getBusinessName(), getInitiatorAlias(),
                getInitiatorFirstName(), status);
    }
}
