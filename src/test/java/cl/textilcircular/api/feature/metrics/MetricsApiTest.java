package cl.textilcircular.api.feature.metrics;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.support.ApiTest;
import cl.textilcircular.api.support.Fixtures;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /admin/metrics}: conteos reales, ventanas y filtros, con la ventana anterior para comparar. */
@ApiTest
class MetricsApiTest {

    private static final AtomicLong RUTS = new AtomicLong(2000);
    private static final int THIRTY_DAYS = 30;
    private static final int NINETY_DAYS = 90;

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    PasswordEncoder encoder;

    ResultActions call(HttpMethod method, String path, String token, String json) throws Exception {
        var builder = request(method, "/api/v1" + path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(json == null ? builder : builder.content(json));
    }

    record Session(String access, long userId, long businessId) {
    }

    record Fixture(String admin, Session e1, Session e2, Session e3,
                   Session c1, Session c2, Session c3) {
    }

    Session session(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.accessToken"),
                ((Number) JsonPath.read(body, "$.user.id")).longValue(), 0);
    }

    String admin() throws Exception {
        accounts.saveAndFlush(UserAccount.registerAdministrator("admin-metrics@demo.cl", "Demo1234",
                "Admin", encoder, Instant.now()));
        String body = call(HttpMethod.POST, "/auth/login", null,
                "{\"email\":\"admin-metrics@demo.cl\",\"password\":\"Demo1234\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    Session customer(String email) throws Exception {
        return session(call(HttpMethod.POST, "/auth/register/customer", null,
                "{\"email\":\"%s\",\"password\":\"Secreta123\",\"alias\":\"Cliente\"}".formatted(email))
                .andExpect(status().isCreated()));
    }

    Session entrepreneur(String email, String firstName, String businessName, long communeId,
                          long ageRangeId, Long... serviceIds) throws Exception {
        String services = Arrays.stream(serviceIds).map(String::valueOf).collect(Collectors.joining(","));
        Session created = session(call(HttpMethod.POST, "/auth/register/entrepreneur", null, """
                {"email":"%s","password":"Secreta123","alias":"%s","rut":"%s","firstName":"%s","lastName":"Pérez",
                 "ageRangeId":%d,"genderId":1,
                 "business":{"name":"%s","description":"%s","photoUrl":null,"communeId":%d,
                             "serviceIds":[%s]}}""".formatted(email, firstName, Fixtures.rutFor(RUTS.incrementAndGet()),
                firstName, ageRangeId, businessName, Fixtures.BUSINESS_DESCRIPTION, communeId, services))
                .andExpect(status().isCreated()));
        long businessId = jdbc.queryForObject("select business_id from business where user_id = ?",
                Long.class, created.userId());
        return new Session(created.access(), created.userId(), businessId);
    }

    /** La funcionalidad de revisión queda fuera de este alcance; las métricas solo necesitan que el negocio sea utilizable. */
    void publish(long businessId) {
        jdbc.update("update business set status = 'APPROVED', approved_at = now() where business_id = ?", businessId);
    }

    void createdDaysAgo(long userId, int days) {
        Timestamp at = Timestamp.from(Instant.now().minus(Duration.ofDays(days)));
        jdbc.update("update user_account set created_at = ? where user_id = ?", at, userId);
        jdbc.update("update business set created_at = ? where user_id = ?", at, userId);
    }

    void analytics(String type, Long businessId, Long regionId, Long serviceId, int daysAgo) {
        jdbc.update("""
                insert into analytics_event (event_id, type, business_id, region_id, service_id, occurred_at)
                values (nextval('analytics_event_seq'), ?, ?, ?, ?, ?)
                """, type, businessId, regionId, serviceId,
                Timestamp.from(Instant.now().minus(Duration.ofDays(daysAgo))));
    }

    long open(Session customer, long businessId) throws Exception {
        String body = call(HttpMethod.POST, "/conversations", customer.access(),
                "{\"businessId\":%d}".formatted(businessId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    void send(Session sender, long conversationId, String content) throws Exception {
        call(HttpMethod.POST, "/conversations/%d/messages".formatted(conversationId), sender.access(),
                "{\"clientUuid\":\"%s\",\"content\":\"%s\"}".formatted(UUID.randomUUID(), content))
                .andExpect(status().isCreated());
    }

    /** Sitúa la última actividad de la conversación en la ventana solicitada. */
    void movedDaysAgo(long conversationId, int days) throws Exception {
        Timestamp at = Timestamp.from(Instant.now().minus(Duration.ofDays(days)));
        jdbc.update("update message set sent_at = ? where conversation_id = ?", at, conversationId);
        jdbc.update("update conversation set created_at = ? where conversation_id = ?", at, conversationId);
    }

    /**
     * E1 (región 1, servicio 1) tiene 10 días, E2 (región 7, servicio 2) 45 días y E3 100 días.
     * C1 se registró hace 5 días, C2 hace 50 días y C3 hace 100 días. Conversaciones: una por cliente y
     * negocio, con dos que recibieron respuesta y dos que no.
     */
    Fixture fixture() throws Exception {
        String admin = admin();
        Session e1 = entrepreneur("ana@demo.cl", "Ana", "Taller de Ana", 1L, 2L, 1L);
        Session e2 = entrepreneur("beto@demo.cl", "Beto", "Taller de Beto", 102L, 3L, 2L);
        Session e3 = entrepreneur("carla@demo.cl", "Carla", "Taller de Carla", 102L, 3L, 2L);
        publish(e1.businessId());
        publish(e2.businessId());
        publish(e3.businessId());

        Session c1 = customer("c1@demo.cl");
        Session c2 = customer("c2@demo.cl");
        Session c3 = customer("c3@demo.cl");

        createdDaysAgo(e1.userId(), 10);
        createdDaysAgo(e2.userId(), 45);
        createdDaysAgo(e3.userId(), 100);
        createdDaysAgo(c1.userId(), 5);
        createdDaysAgo(c2.userId(), 50);
        createdDaysAgo(c3.userId(), 100);

        analytics("DIRECTORY_SEARCH", null, 1L, 1L, 1);
        analytics("DIRECTORY_SEARCH", null, 1L, 1L, 2);
        analytics("DIRECTORY_SEARCH", null, 7L, 2L, 3);
        analytics("DIRECTORY_SEARCH", null, 1L, 1L, 40);
        analytics("DIRECTORY_SEARCH", null, 1L, 1L, 45);
        analytics("DIRECTORY_SEARCH", null, 1L, 1L, 100);

        analytics("BUSINESS_VIEW", e1.businessId(), null, null, 1);
        analytics("BUSINESS_VIEW", e1.businessId(), null, null, 2);
        analytics("BUSINESS_VIEW", e1.businessId(), null, null, 3);
        analytics("BUSINESS_VIEW", e2.businessId(), null, null, 4);
        analytics("BUSINESS_VIEW", e1.businessId(), null, null, 40);
        analytics("BUSINESS_VIEW", e1.businessId(), null, null, 100);

        long a1 = open(c1, e1.businessId());
        send(c1, a1, "Busco un arreglo");
        movedDaysAgo(a1, 2);

        long a2 = open(c2, e1.businessId());
        send(c2, a2, "¿Tienen hora?");
        send(e1, a2, "Sí, mañana");
        movedDaysAgo(a2, 5);

        long b1 = open(c3, e2.businessId());
        send(c3, b1, "Hola");
        send(e2, b1, "Hola");
        movedDaysAgo(b1, 40);

        long b2 = open(c1, e2.businessId());
        send(c1, b2, "Consulta");
        movedDaysAgo(b2, 1);

        return new Fixture(admin, e1, e2, e3, c1, c2, c3);
    }

    @Test
    void all_time_totals_have_no_previous_window() throws Exception {
        Fixture data = fixture();

        call(HttpMethod.GET, "/admin/metrics", data.admin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEntrepreneurs").value(3))
                .andExpect(jsonPath("$.newEntrepreneurs").value(3))
                .andExpect(jsonPath("$.searches.value").value(6))
                .andExpect(jsonPath("$.searches.previous").value(0))
                .andExpect(jsonPath("$.profileViews.value").value(6))
                .andExpect(jsonPath("$.profileViews.previous").value(0))
                .andExpect(jsonPath("$.conversations.value").value(4))
                .andExpect(jsonPath("$.conversations.previous").value(0))
                .andExpect(jsonPath("$.effectiveConversations").value(2))
                .andExpect(jsonPath("$.registrations.value").value(6))
                .andExpect(jsonPath("$.registrations.previous").value(0))
                .andExpect(jsonPath("$.byRegion", hasSize(2)))
                .andExpect(jsonPath("$.byRegion[0].label").value("Metropolitana de Santiago"))
                .andExpect(jsonPath("$.byRegion[0].value").value(2))
                .andExpect(jsonPath("$.byRegion[1].label").value("Arica y Parinacota"))
                .andExpect(jsonPath("$.byRegion[1].value").value(1))
                .andExpect(jsonPath("$.byAgeRange", hasSize(2)))
                .andExpect(jsonPath("$.byAgeRange[0].label").value("18-24"))
                .andExpect(jsonPath("$.byAgeRange[0].value").value(1))
                .andExpect(jsonPath("$.byAgeRange[1].label").value("25-34"))
                .andExpect(jsonPath("$.byAgeRange[1].value").value(2))
                .andExpect(jsonPath("$.byService[0].value").value(2))
                .andExpect(jsonPath("$.byService[1].value").value(2))
                .andExpect(jsonPath("$.mostVisitedBusinesses[0].label").value("Taller de Ana"))
                .andExpect(jsonPath("$.mostVisitedBusinesses[0].value").value(5))
                .andExpect(jsonPath("$.mostVisitedBusinesses[1].label").value("Taller de Beto"))
                .andExpect(jsonPath("$.mostVisitedBusinesses[1].value").value(1));
    }

    @Test
    void thirty_days_compare_with_the_previous_thirty() throws Exception {
        Fixture data = fixture();

        call(HttpMethod.GET, "/admin/metrics?days=" + THIRTY_DAYS, data.admin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEntrepreneurs").value(3))
                .andExpect(jsonPath("$.newEntrepreneurs").value(1))
                .andExpect(jsonPath("$.searches.value").value(3))
                .andExpect(jsonPath("$.searches.previous").value(2))
                .andExpect(jsonPath("$.profileViews.value").value(4))
                .andExpect(jsonPath("$.profileViews.previous").value(1))
                .andExpect(jsonPath("$.conversations.value").value(3))
                .andExpect(jsonPath("$.conversations.previous").value(1))
                .andExpect(jsonPath("$.effectiveConversations").value(1))
                .andExpect(jsonPath("$.registrations.value").value(2))
                .andExpect(jsonPath("$.registrations.previous").value(2))
                .andExpect(jsonPath("$.byService[0].value").value(2))
                .andExpect(jsonPath("$.byService[1].value").value(1))
                .andExpect(jsonPath("$.mostVisitedBusinesses[0].value").value(3))
                .andExpect(jsonPath("$.mostVisitedBusinesses[1].value").value(1));
    }

    @Test
    void ninety_days_include_the_previous_thirty() throws Exception {
        Fixture data = fixture();

        call(HttpMethod.GET, "/admin/metrics?days=" + NINETY_DAYS, data.admin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newEntrepreneurs").value(2))
                .andExpect(jsonPath("$.searches.value").value(5))
                .andExpect(jsonPath("$.searches.previous").value(1))
                .andExpect(jsonPath("$.profileViews.value").value(5))
                .andExpect(jsonPath("$.profileViews.previous").value(1))
                .andExpect(jsonPath("$.conversations.value").value(4))
                .andExpect(jsonPath("$.conversations.previous").value(0))
                .andExpect(jsonPath("$.effectiveConversations").value(2))
                .andExpect(jsonPath("$.registrations.value").value(4))
                .andExpect(jsonPath("$.registrations.previous").value(2))
                .andExpect(jsonPath("$.mostVisitedBusinesses[0].value").value(4));
    }

    @Test
    void region_and_service_filters_narrow_every_breakdown() throws Exception {
        Fixture data = fixture();

        call(HttpMethod.GET, "/admin/metrics?days=30&regionId=1", data.admin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEntrepreneurs").value(1))
                .andExpect(jsonPath("$.newEntrepreneurs").value(1))
                .andExpect(jsonPath("$.searches.value").value(2))
                .andExpect(jsonPath("$.searches.previous").value(2))
                .andExpect(jsonPath("$.profileViews.value").value(3))
                .andExpect(jsonPath("$.profileViews.previous").value(1))
                .andExpect(jsonPath("$.conversations.value").value(2))
                .andExpect(jsonPath("$.conversations.previous").value(0))
                .andExpect(jsonPath("$.effectiveConversations").value(1))
                .andExpect(jsonPath("$.registrations.value").value(2))
                .andExpect(jsonPath("$.byRegion", hasSize(1)))
                .andExpect(jsonPath("$.byRegion[0].label").value("Arica y Parinacota"))
                .andExpect(jsonPath("$.byService", hasSize(1)))
                .andExpect(jsonPath("$.byService[0].value").value(2))
                .andExpect(jsonPath("$.mostVisitedBusinesses", hasSize(1)))
                .andExpect(jsonPath("$.mostVisitedBusinesses[0].value").value(3));

        call(HttpMethod.GET, "/admin/metrics?days=30&serviceId=2", data.admin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEntrepreneurs").value(2))
                .andExpect(jsonPath("$.newEntrepreneurs").value(0))
                .andExpect(jsonPath("$.searches.value").value(1))
                .andExpect(jsonPath("$.searches.previous").value(0))
                .andExpect(jsonPath("$.profileViews.value").value(1))
                .andExpect(jsonPath("$.profileViews.previous").value(0))
                .andExpect(jsonPath("$.conversations.value").value(1))
                .andExpect(jsonPath("$.conversations.previous").value(1))
                .andExpect(jsonPath("$.effectiveConversations").value(0))
                .andExpect(jsonPath("$.registrations.value").value(2))
                .andExpect(jsonPath("$.byRegion", hasSize(1)))
                .andExpect(jsonPath("$.byRegion[0].label").value("Metropolitana de Santiago"))
                .andExpect(jsonPath("$.byService", hasSize(1)))
                .andExpect(jsonPath("$.byService[0].value").value(1))
                .andExpect(jsonPath("$.mostVisitedBusinesses", hasSize(1)))
                .andExpect(jsonPath("$.mostVisitedBusinesses[0].label").value("Taller de Beto"));
    }

    @Test
    void most_visited_businesses_stop_at_five() throws Exception {
        Fixture data = fixture();

        for (int i = 1; i <= 5; i++) {
            Session business = entrepreneur("top%d@demo.cl".formatted(i), "Top" + i, "Taller Top " + i,
                    1L, 2L, 1L);
            for (int view = 0; view < 11 - i; view++) {
                analytics("BUSINESS_VIEW", business.businessId(), null, null, 1);
            }
        }

        call(HttpMethod.GET, "/admin/metrics", data.admin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mostVisitedBusinesses", hasSize(5)))
                .andExpect(jsonPath("$.mostVisitedBusinesses[0].label").value("Taller Top 1"))
                .andExpect(jsonPath("$.mostVisitedBusinesses[0].value").value(10))
                .andExpect(jsonPath("$.mostVisitedBusinesses[4].label").value("Taller Top 5"))
                .andExpect(jsonPath("$.mostVisitedBusinesses[4].value").value(6));
    }

    @Test
    void only_administrators_and_the_two_windows_are_accepted() throws Exception {
        Fixture data = fixture();

        call(HttpMethod.GET, "/admin/metrics?days=7", data.admin(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[0].field").value("days"));
        call(HttpMethod.GET, "/admin/metrics", data.c1().access(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        call(HttpMethod.GET, "/admin/metrics", null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
