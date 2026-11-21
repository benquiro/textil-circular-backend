package cl.textilcircular.api.core.model.messaging;

import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.product.Product;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.NotFoundException;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.persistence.BaseEntity;
import cl.textilcircular.api.core.validation.Validators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Chat entre un cliente y un negocio. Los participantes son el cliente que lo inició y el dueño
 * del negocio; nadie más puede leer, enviar ni marcar mensajes.
 */
@Getter
@Entity
@Table(name = "conversation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseEntity {

    private static final String FALLBACK_NAME = "Usuario";
    private static final String DELETED_NAME = "Usuario eliminado";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conversation_seq")
    @SequenceGenerator(name = "conversation_seq", sequenceName = "conversation_seq", allocationSize = 50)
    @Column(name = "conversation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiator_id", nullable = false, updatable = false)
    private UserAccount initiator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, updatable = false)
    private Business business;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Se establece mientras un participante mantiene la conversación bloqueada para ambas partes. */
    @Column(name = "blocked_at")
    private Instant blockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by")
    private UserAccount blockedBy;

    /**
     * Un cliente contacta a un negocio del directorio. El llamador busca primero el hilo
     * existente ({@code findByInitiatorIdAndBusinessId}) y lo reutiliza.
     */
    public static Conversation start(UserAccount customer, Business business, Instant now) {
        customer.ensureActive();
        if (!customer.isCustomer()) {
            throw new ForbiddenException("Solo los clientes pueden iniciar una conversación");
        }
        if (!business.isPublic()) {
            throw new NotFoundException();
        }
        Conversation conversation = new Conversation();
        conversation.initiator = customer;
        conversation.business = business;
        conversation.createdAt = now;
        return conversation;
    }

    /**
     * Mensaje nuevo de un participante. La idempotencia es tarea del llamador y ocurre después
     * de esta verificación de participación: buscar {@code clientUuid} y usar {@link Message#isRetryBy}.
     */
    public Message send(UserAccount sender, UUID clientUuid, String content, Product product, Instant now) {
        ensureCanSend(sender);
        UUID uuid = Validators.required("clientUuid", clientUuid, "Falta el identificador del mensaje");
        String text = Validators.text("content", content, Validators.MESSAGE_MAX, "el mensaje");
        if (product != null && !sameEntity(product.getBusiness(), business)) {
            throw new ValidationException("productId", "El producto no pertenece a este taller");
        }
        return new Message(this, sender, uuid, text, product, now);
    }

    /** La guarda de envío: la cuenta debe estar activa, ser participante y el hilo no estar bloqueado. */
    public void ensureCanSend(UserAccount sender) {
        sender.ensureActive();
        requireParticipant(sender);
        if (isBlocked()) {
            throw new ForbiddenException("CONVERSATION_BLOCKED", "La conversación está bloqueada");
        }
    }

    public void block(UserAccount actor, Instant now) {
        actor.ensureActive();
        requireParticipant(actor);
        if (blockedAt == null) {
            blockedAt = now;
            blockedBy = actor;
        }
    }

    /** El contrato responde 404 aquí, para que un ajeno no pueda saber que la conversación existe. */
    public void unblock(UserAccount actor) {
        actor.ensureActive();
        requireVisibleTo(actor);
        blockedAt = null;
        blockedBy = null;
    }

    public boolean isBlocked() {
        return blockedAt != null;
    }

    public boolean isParticipant(UserAccount account) {
        return sameEntity(initiator, account) || business.isOwnedBy(account);
    }

    public void requireParticipant(UserAccount account) {
        if (!isParticipant(account)) {
            throw new ForbiddenException();
        }
    }

    /** Para lecturas: un ajeno debe ver la conversación como inexistente, no como prohibida. */
    public void requireVisibleTo(UserAccount account) {
        if (!isParticipant(account)) {
            throw new NotFoundException();
        }
    }

    /** Nombre de la otra parte, tal como lo ve quien consulta la bandeja. */
    public String counterpartName(UserAccount viewer) {
        return counterpartName(viewer, null);
    }

    /** {@code counterpartFirstName} solo se usa cuando la otra parte no tiene alias y su cuenta no está eliminada. */
    public String counterpartName(UserAccount viewer, String counterpartFirstName) {
        if (sameEntity(initiator, viewer)) {
            return business.getName();
        }
        return displayedName(initiator.getAlias(), counterpartFirstName, initiator.getStatus());
    }

    /** La misma regla para las proyecciones de bandeja, que no cargan una instancia de {@link Conversation}. */
    public static String counterpartName(long viewerId, long initiatorId, String businessName,
                                         String initiatorAlias, String initiatorFirstName,
                                         AccountStatus initiatorStatus) {
        if (viewerId == initiatorId) {
            return businessName;
        }
        return displayedName(initiatorAlias, initiatorFirstName, initiatorStatus);
    }

    private static String displayedName(String alias, String firstName, AccountStatus status) {
        if (status == AccountStatus.DELETED) {
            return DELETED_NAME;
        }
        if (alias != null && !alias.isBlank()) {
            return alias;
        }
        if (firstName != null && !firstName.isBlank()) {
            return firstName;
        }
        return FALLBACK_NAME;
    }
}
