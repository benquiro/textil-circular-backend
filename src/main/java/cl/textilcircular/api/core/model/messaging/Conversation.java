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
 * Chat between a customer and a business. The participants are the customer who started it and
 * the business owner; nobody else can read, send or mark messages.
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

    /** Set while a participant keeps the conversation blocked for both sides. */
    @Column(name = "blocked_at")
    private Instant blockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by")
    private UserAccount blockedBy;

    /**
     * A customer contacts a business of the directory. Callers first look for the existing
     * thread ({@code findByInitiatorIdAndBusinessId}) and reuse it.
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
     * New message from a participant. Idempotency is the caller's job and happens after this
     * participant check: look up {@code clientUuid} and use {@link Message#isRetryBy}.
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

    /** The send guard: the account must be usable, a participant, and the thread not blocked. */
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

    /** The contract answers 404 here, so an outsider cannot tell the conversation exists. */
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

    /** For reads: an outsider must see the conversation as missing, not as forbidden. */
    public void requireVisibleTo(UserAccount account) {
        if (!isParticipant(account)) {
            throw new NotFoundException();
        }
    }

    /** Name of the other side, as the viewer sees it in the inbox. */
    public String counterpartName(UserAccount viewer) {
        return counterpartName(viewer, null);
    }

    /** {@code counterpartFirstName} is only used when the other side has neither alias nor a deleted account. */
    public String counterpartName(UserAccount viewer, String counterpartFirstName) {
        if (sameEntity(initiator, viewer)) {
            return business.getName();
        }
        return displayedName(initiator.getAlias(), counterpartFirstName, initiator.getStatus());
    }

    /** Same rule for inbox projections, which do not load a {@link Conversation} instance. */
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
