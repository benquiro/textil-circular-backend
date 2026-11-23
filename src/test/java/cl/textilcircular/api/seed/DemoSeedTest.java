package cl.textilcircular.api.seed;

import cl.textilcircular.api.core.model.account.AccountNotActiveException;
import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.account.InvalidCredentialsException;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.repository.BusinessRepository;
import cl.textilcircular.api.core.model.business.BusinessReview;
import cl.textilcircular.api.core.model.business.BusinessStatus;
import cl.textilcircular.api.core.model.business.CatalogEntry;
import cl.textilcircular.api.core.repository.CatalogEntryRepository;
import cl.textilcircular.api.core.model.business.RejectionReason;
import cl.textilcircular.api.core.repository.ConversationRepository;
import cl.textilcircular.api.core.repository.OutreachContentRepository;
import cl.textilcircular.api.core.model.product.OfferingType;
import cl.textilcircular.api.core.model.product.Product;
import cl.textilcircular.api.core.model.product.ProductCondition;
import cl.textilcircular.api.core.model.product.ProductSize;
import cl.textilcircular.api.core.repository.ProductRepository;
import cl.textilcircular.api.support.RepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Loads the dev profile migrations (schema + demo data) into its own database. */
@RepositoryTest
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
class DemoSeedTest {

    @Autowired UserAccountRepository accounts;
    @Autowired BusinessRepository businesses;
    @Autowired CatalogEntryRepository catalog;
    @Autowired ProductRepository products;
    @Autowired ConversationRepository conversations;
    @Autowired OutreachContentRepository contents;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void demoAccountsLogInWithTheDocumentedPassword() {
        assertThat(accounts.count()).isEqualTo(43);
        UserAccount martin = accounts.findByEmail("martin.diaz@example.cl").orElseThrow();

        martin.authenticate("Demo1234", encoder, Instant.now());

        assertThat(martin.getPasswordHash()).startsWith("{bcrypt}");
    }

    @Test
    void accountsCoverEveryStatusAndItsLoginOutcome() {
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM user_account", String.class))
                .containsExactlyInAnyOrder("ACTIVE", "DEACTIVATED", "SUSPENDED", "DELETED");

        UserAccount suspended = accounts.findByEmail("bruno.silva@example.cl").orElseThrow();
        assertThatThrownBy(() -> suspended.authenticate("Demo1234", encoder, Instant.now()))
                .isInstanceOf(AccountNotActiveException.class);

        UserAccount deactivated = accounts.findByEmail("diego.morales@example.cl").orElseThrow();
        deactivated.authenticate("Demo1234", encoder, Instant.now());
        assertThat(deactivated.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        UserAccount deleted = accounts.findById(19L).orElseThrow();
        assertThat(deleted.getEmail()).endsWith("@anonymized.invalid");
        assertThatThrownBy(() -> deleted.authenticate("Demo1234", encoder, Instant.now()))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entrepreneur WHERE anonymized_at IS NOT NULL AND rut IS NULL",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void directoryNeedsMoreThanOnePageAndSkipsNonPublicBusinesses() {
        var firstPage = catalog.search(null, null, null, null, "demo", PageRequest.of(0, 20));

        assertThat(firstPage.getTotalElements()).isEqualTo(28);
        assertThat(firstPage.getContent()).hasSize(20);
        assertThat(catalog.search(null, null, null, null, "demo", PageRequest.of(0, 50)).getContent())
                .extracting(CatalogEntry::getId)
                .contains(1L, 2L, 3L, 6L)
                // hidden by its owner, deleted, owner suspended, pending, rejected
                .doesNotContain(7L, 9L, 10L, 4L, 5L, 8L);
        assertThat(products.findPublicByBusinessId(2L)).hasSize(2);
    }

    @Test
    void resubmittedBusinessIsPendingInItsSecondRound() {
        var business = businesses.findById(8L).orElseThrow();

        assertThat(business.getStatus()).isEqualTo(BusinessStatus.PENDING);
        assertThat(business.getReviewRound()).isEqualTo(2);
        assertThat(business.getReviews()).extracting(BusinessReview::getReviewRound).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void productsCarryTheirStructuredAttributes() {
        Product jacket = products.findById(5L).orElseThrow();
        assertThat(jacket.getService().getId()).isEqualTo(6L);
        assertThat(jacket.getSize()).isEqualTo(ProductSize.M);
        assertThat(jacket.getCondition()).isEqualTo(ProductCondition.NEW);

        assertThat(products.findById(1L).orElseThrow().getOfferingType()).isEqualTo(OfferingType.REPAIR);
        assertThat(jdbc.queryForList("SELECT DISTINCT condition FROM product WHERE condition IS NOT NULL", String.class))
                .containsExactlyInAnyOrder("NEW", "GOOD", "WITH_DETAILS");
        // A product's service must be one its business offers.
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM product p
                WHERE p.service_id IS NOT NULL AND NOT EXISTS (
                    SELECT 1 FROM business_service bs WHERE bs.business_id = p.business_id AND bs.service_id = p.service_id)
                """, Integer.class)).isZero();
    }

    @Test
    void moderationQueuesHaveData() {
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM conversation_report", String.class))
                .containsExactlyInAnyOrder("OPEN", "RESOLVED", "DISMISSED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation WHERE blocked_at IS NOT NULL", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void metricsCoverEveryEventType() {
        assertThat(jdbc.queryForList("SELECT DISTINCT type FROM analytics_event", String.class))
                .containsExactlyInAnyOrder("DIRECTORY_SEARCH", "BUSINESS_VIEW", "PRODUCT_VIEW", "CONTACT_CLICK");
    }

    @Test
    void seededRejectionCarriesAStructuredReason() {
        BusinessReview rejection = businesses.findById(5L).orElseThrow().getReviews().getFirst();

        assertThat(rejection.getRejectionReason()).isEqualTo(RejectionReason.UNCLEAR_DESCRIPTION);
    }

    @Test
    void chatsAndFeedAreAvailable() {
        assertThat(conversations.findInbox(8L)).hasSize(2);
        assertThat(contents.findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(PageRequest.of(0, 20)).getTotalElements()).isEqualTo(6);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM outreach_content WHERE type = 'EVENT' AND event_at > now() AND deleted_at IS NULL",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void runningTheSeedAgainChangesNothing() throws Exception {
        String counts = "SELECT (SELECT count(*) FROM user_account) || '/' || (SELECT count(*) FROM business_service)"
                + " || '/' || (SELECT count(*) FROM message) || '/' || (SELECT count(*) FROM product_photo)"
                + " || '/' || (SELECT count(*) FROM business_review) || '/' || (SELECT count(*) FROM analytics_event)";
        String before = jdbc.queryForObject(counts, String.class);

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/seed-dev/R__demo_data.sql"));
        }

        assertThat(jdbc.queryForObject(counts, String.class)).isEqualTo(before);
    }
}
