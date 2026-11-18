package cl.textilcircular.api.feature.messaging;

import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.NotFoundException;
import cl.textilcircular.api.core.error.UnauthorizedException;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.messaging.Conversation;
import cl.textilcircular.api.core.model.messaging.ConversationReport;
import cl.textilcircular.api.core.model.messaging.Message;
import cl.textilcircular.api.core.model.messaging.ReportStatus;
import cl.textilcircular.api.core.persistence.BaseEntity;
import cl.textilcircular.api.core.realtime.RealtimeSessions;
import cl.textilcircular.api.core.repository.BusinessRepository;
import cl.textilcircular.api.core.repository.ConversationReportRepository;
import cl.textilcircular.api.core.repository.ConversationRepository;
import cl.textilcircular.api.core.repository.EntrepreneurRepository;
import cl.textilcircular.api.core.repository.InboxItem;
import cl.textilcircular.api.core.repository.MessageRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.core.validation.Validators;
import cl.textilcircular.api.feature.messaging.MessagingDtos.ConversationView;
import cl.textilcircular.api.feature.messaging.MessagingDtos.MessageCreated;
import cl.textilcircular.api.feature.messaging.MessagingDtos.MessageRead;
import cl.textilcircular.api.feature.messaging.MessagingDtos.MessageView;
import cl.textilcircular.api.feature.messaging.MessagingDtos.OpenConversationRequest;
import cl.textilcircular.api.feature.messaging.MessagingDtos.ReportRequest;
import cl.textilcircular.api.feature.messaging.MessagingDtos.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bandeja, historial de mensajes, envío, confirmaciones de lectura, bloqueo y reportes. */
@Service
@RequiredArgsConstructor
@Transactional
class MessagingService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ConversationReportRepository reports;
    private final BusinessRepository businesses;
    private final UserAccountRepository accounts;
    private final EntrepreneurRepository entrepreneurs;
    private final RealtimeSessions realtime;
    private final Clock clock;

    @Transactional(readOnly = true)
    List<ConversationView> list(Caller caller) {
        UserAccount user = activeAccount(caller);
        return conversations.findInbox(user.getId()).stream()
                .map(item -> ConversationView.of(item, user.getId()))
                .toList();
    }

    ConversationView open(Caller caller, OpenConversationRequest request) {
        UserAccount user = activeAccount(caller);
        Long businessId = Validators.required("businessId", request.businessId(), "Elige el emprendimiento");
        Business business = businesses.findById(businessId)
                .filter(Business::isPublic)
                .orElseThrow(NotFoundException::new);
        Conversation conversation = conversations.findByInitiatorIdAndBusinessId(user.getId(), business.getId())
                .orElseGet(() -> conversations.saveAndFlush(Conversation.start(user, business, clock.instant())));
        return view(conversation, user);
    }

    @Transactional(readOnly = true)
    List<MessageView> messages(Caller caller, long conversationId, Long afterId) {
        UserAccount user = activeAccount(caller);
        Conversation conversation = conversation(conversationId);
        conversation.requireVisibleTo(user);
        List<Message> found = afterId == null
                ? messages.findByConversationIdOrderBySentAtAscIdAsc(conversationId)
                : messages.findByConversationIdAndIdGreaterThanOrderBySentAtAscIdAsc(conversationId, afterId);
        return found.stream().map(MessageView::of).toList();
    }

    /**
     * Un reintento con el mismo {@code clientUuid} y remitente devuelve el mensaje guardado con 200;
     * el mismo uuid usado por otra persona es un conflicto. La verificación de bloqueo se ejecuta antes
     * de la búsqueda, así que una conversación bloqueada nunca acepta un envío, sea reintento o no.
     */
    SendResult send(Caller caller, long conversationId, SendMessageRequest request) {
        UserAccount user = activeAccount(caller);
        Conversation conversation = conversation(conversationId);
        conversation.ensureCanSend(user);

        UUID uuid = request.clientUuid();
        if (uuid != null) {
            Optional<Message> existing = messages.findByClientUuid(uuid);
            if (existing.isPresent()) {
                Message message = existing.get();
                message.isRetryBy(conversation, user);
                return new SendResult(MessageView.of(message), false);
            }
        }

        Message saved = messages.save(conversation.send(user, uuid, request.content(), null, clock.instant()));
        realtime.send(otherParticipant(conversation, user), "message.created",
                MessageCreated.of(saved, senderName(user)));
        return new SendResult(MessageView.of(saved), true);
    }

    void markRead(Caller caller, long conversationId) {
        UserAccount user = activeAccount(caller);
        Conversation conversation = conversation(conversationId);
        Instant now = clock.instant();
        int marked = messages.markReadBy(conversation, user, now);
        if (marked > 0) {
            realtime.send(otherParticipant(conversation, user), "message.read", new MessageRead(conversationId, now));
        }
    }

    void block(Caller caller, long conversationId) {
        UserAccount user = activeAccount(caller);
        conversation(conversationId).block(user, clock.instant());
    }

    void unblock(Caller caller, long conversationId) {
        UserAccount user = activeAccount(caller);
        conversation(conversationId).unblock(user);
    }

    void report(Caller caller, long conversationId, ReportRequest request) {
        UserAccount user = activeAccount(caller);
        Conversation conversation = conversation(conversationId);
        conversation.requireParticipant(user);
        if (reports.existsByConversationIdAndReporterIdAndStatus(conversationId, user.getId(), ReportStatus.OPEN)) {
            throw new ConflictException("REPORT_ALREADY_OPEN", "Ya tienes un reporte abierto en esta conversación");
        }
        Message message = null;
        if (request.messageId() != null) {
            message = messages.findById(request.messageId())
                    .orElseThrow(() -> new ValidationException("messageId", "El mensaje no pertenece a esta conversación"));
        }
        reports.save(ConversationReport.file(conversation, user, message, request.reason(), request.detail(),
                clock.instant()));
    }

    private ConversationView view(Conversation conversation, UserAccount viewer) {
        InboxItem item = conversations.findInboxItem(conversation.getId(), viewer.getId()).orElse(null);
        if (item != null) {
            return ConversationView.of(item, viewer.getId());
        }
        String counterpart = conversation.counterpartName(viewer, firstNameOf(conversation.getInitiator()));
        return new ConversationView(conversation.getId(), conversation.getBusiness().getId(),
                conversation.getBusiness().getName(), counterpart, null, null, null, 0);
    }

    private Conversation conversation(long conversationId) {
        return conversations.findById(conversationId).orElseThrow(NotFoundException::new);
    }

    private long otherParticipant(Conversation conversation, UserAccount sender) {
        if (BaseEntity.sameEntity(conversation.getInitiator(), sender)) {
            return conversation.getBusiness().getOwner().getAccount().getId();
        }
        return conversation.getInitiator().getId();
    }

    private String senderName(UserAccount account) {
        if (account.getAlias() != null && !account.getAlias().isBlank()) {
            return account.getAlias();
        }
        String firstName = firstNameOf(account);
        return firstName == null ? "Usuario" : firstName;
    }

    private String firstNameOf(UserAccount account) {
        return account.isEntrepreneur()
                ? entrepreneurs.findById(account.getId()).map(Entrepreneur::getFirstName).orElse(null)
                : null;
    }

    /** Un token puede sobrevivir al estado de su cuenta: aquí se rechazan las cuentas suspendidas o desactivadas. */
    private UserAccount activeAccount(Caller caller) {
        UserAccount account = accounts.findById(caller.userId())
                .filter(a -> a.getStatus() != AccountStatus.DELETED)
                .orElseThrow(UnauthorizedException::new);
        account.ensureActive();
        return account;
    }

    record SendResult(MessageView message, boolean created) {
    }
}
