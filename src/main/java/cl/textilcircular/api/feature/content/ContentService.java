package cl.textilcircular.api.feature.content;

import cl.textilcircular.api.core.error.NotFoundException;
import cl.textilcircular.api.core.error.UnauthorizedException;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.outreach.OutreachContent;
import cl.textilcircular.api.core.repository.OutreachContentRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.content.ContentDtos.Content;
import cl.textilcircular.api.feature.content.ContentDtos.ContentPage;
import cl.textilcircular.api.feature.content.ContentDtos.ContentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * The foundation's home feed. Reading is public; publishing, editing and deleting are for administrators,
 * a rule that {@link OutreachContent} enforces (SecurityConfig already turns other roles away by role).
 */
@Service
@RequiredArgsConstructor
@Transactional
class ContentService {

    private static final int MAX_PAGE_SIZE = 50;

    private final OutreachContentRepository contents;
    private final UserAccountRepository accounts;
    private final Clock clock;

    /** Newest first; deleted content never shows up. */
    @Transactional(readOnly = true)
    ContentPage list(int page, int size) {
        Page<OutreachContent> found = contents.findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
        return new ContentPage(found.map(Content::of).getContent(), found.getNumber(), found.getSize(),
                found.getTotalElements(), found.isLast());
    }

    @Transactional(readOnly = true)
    Content get(long contentId) {
        return Content.of(visible(contentId));
    }

    Content publish(Caller caller, ContentRequest request) {
        OutreachContent content = OutreachContent.publish(author(caller), request.type(), request.title(),
                request.body(), request.imageUrl(), request.eventAt(), request.eventLocation());
        // saveAndFlush so createdAt (auditing) and the id are set before building the response
        return Content.of(contents.saveAndFlush(content));
    }

    /** A deleted content still exists: the entity answers 409 CONTENT_DELETED instead of 404. */
    Content update(Caller caller, long contentId, ContentRequest request) {
        UserAccount editor = author(caller);
        OutreachContent content = contents.findById(contentId).orElseThrow(NotFoundException::new);
        content.update(editor, request.type(), request.title(), request.body(), request.imageUrl(),
                request.eventAt(), request.eventLocation());
        return Content.of(content);
    }

    /** Soft delete: the row stays, but leaves the list and the detail. */
    void delete(Caller caller, long contentId) {
        UserAccount editor = author(caller);
        visible(contentId).delete(editor, clock.instant());
    }

    private OutreachContent visible(long contentId) {
        return contents.findByIdAndDeletedAtIsNull(contentId).orElseThrow(NotFoundException::new);
    }

    private UserAccount author(Caller caller) {
        return accounts.findById(caller.userId()).orElseThrow(UnauthorizedException::new);
    }
}
