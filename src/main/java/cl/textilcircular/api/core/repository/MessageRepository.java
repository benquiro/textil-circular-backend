package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.messaging.Conversation;
import cl.textilcircular.api.core.model.messaging.Message;
import cl.textilcircular.api.core.model.account.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByClientUuid(UUID clientUuid);

    List<Message> findByConversationIdOrderBySentAtAscIdAsc(Long conversationId);

    /** Puesta al día tras una reconexión: solo lo que la aplicación aún no tiene, igualmente lo más antiguo primero. */
    List<Message> findByConversationIdAndIdGreaterThanOrderBySentAtAscIdAsc(Long conversationId, Long afterId);

    /** Marca como leído lo que envió el otro participante; rechaza lectores ajenos a la conversación. */
    default int markReadBy(Conversation conversation, UserAccount reader, Instant now) {
        conversation.requireParticipant(reader);
        return markUnreadFromOthers(conversation.getId(), reader.getId(), now);
    }

    /**
     * Actualización de bajo nivel detrás de {@link #markReadBy}; no verifica la participación.
     * {@code read_at} nunca es anterior a {@code sent_at}, aunque el reloj del emisor estuviera
     * adelantado. El contexto de persistencia no se limpia, por lo que las entidades que el
     * llamador ya mantiene siguen gestionadas; un Message ya cargado en esta transacción
     * conserva su {@code readAt} anterior hasta que se recargue.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Message m set m.readAt = greatest(m.sentAt, :now)
            where m.conversation.id = :conversationId and m.sender.id <> :readerId and m.readAt is null
            """)
    int markUnreadFromOthers(Long conversationId, Long readerId, Instant now);

    /** Total de mensajes sin leer para el contador de la navegación. */
    @Query("""
            select count(m) from Message m join m.conversation c join c.business b
            where m.readAt is null and m.sender.id <> :userId
              and (c.initiator.id = :userId or b.owner.id = :userId)
            """)
    long countUnreadFor(Long userId);
}
