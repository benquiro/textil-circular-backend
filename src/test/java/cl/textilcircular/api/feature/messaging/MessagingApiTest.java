package cl.textilcircular.api.feature.messaging;

import cl.textilcircular.api.support.ApiTest;
import cl.textilcircular.api.support.Fixtures;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The chat contract: inbox, history, idempotent send, read state, blocking and reports. */
@ApiTest
class MessagingApiTest {

    private static final AtomicLong RUTS = new AtomicLong(1000);

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;

    ResultActions call(HttpMethod method, String path, String token, String json) throws Exception {
        var builder = request(method, "/api/v1" + path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(json == null ? builder : builder.content(json));
    }

    record Session(String access, long userId, long businessId) {
    }

    Session session(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.accessToken"),
                ((Number) JsonPath.read(body, "$.user.id")).longValue(), 0);
    }

    Session customer(String email, String alias) throws Exception {
        return session(call(HttpMethod.POST, "/auth/register/customer", null,
                "{\"email\":\"%s\",\"password\":\"Secreta123\",\"alias\":\"%s\"}".formatted(email, alias))
                .andExpect(status().isCreated()));
    }

    Session entrepreneur(String email, String alias, String firstName, long communeId, Long... serviceIds)
            throws Exception {
        String services = Arrays.stream(serviceIds).map(String::valueOf).collect(Collectors.joining(","));
        Session created = session(call(HttpMethod.POST, "/auth/register/entrepreneur", null, """
                {"email":"%s","password":"Secreta123","alias":"%s","rut":"%s","firstName":"%s","lastName":"Pérez",
                 "ageRangeId":2,"genderId":1,
                 "business":{"name":"Taller de %s","description":"%s","photoUrl":null,"communeId":%d,
                             "serviceIds":[%s]}}""".formatted(email, alias, Fixtures.rutFor(RUTS.incrementAndGet()),
                firstName, firstName, Fixtures.BUSINESS_DESCRIPTION, communeId, services))
                .andExpect(status().isCreated()));
        long businessId = jdbc.queryForObject("select business_id from business where user_id = ?",
                Long.class, created.userId());
        return new Session(created.access(), created.userId(), businessId);
    }

    /** Approves the business directly: the review feature is not part of this slice. */
    void publish(long businessId) {
        jdbc.update("update business set status = 'APPROVED', approved_at = now() where business_id = ?", businessId);
    }

    long open(Session customer, long businessId) throws Exception {
        String body = call(HttpMethod.POST, "/conversations", customer.access(),
                "{\"businessId\":%d}".formatted(businessId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    ResultActions send(Session sender, long conversationId, String uuid, String content) throws Exception {
        return call(HttpMethod.POST, "/conversations/%d/messages".formatted(conversationId), sender.access(),
                "{\"clientUuid\":\"%s\",\"content\":\"%s\"}".formatted(uuid, content));
    }

    long sentId(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void opening_a_conversation_is_idempotent_and_each_side_sees_its_counterpart() throws Exception {
        Session customer = customer("cliente@demo.cl", "Cami");
        Session owner = entrepreneur("duena@demo.cl", "Anita", "Ana", 1L, 1L);
        publish(owner.businessId());

        String body = call(HttpMethod.POST, "/conversations", customer.access(),
                "{\"businessId\":%d}".formatted(owner.businessId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessId").value(owner.businessId()))
                .andExpect(jsonPath("$.businessName").value("Taller de Ana"))
                .andExpect(jsonPath("$.counterpartName").value("Taller de Ana"))
                .andExpect(jsonPath("$.lastMessage").value(nullValue()))
                .andExpect(jsonPath("$.lastSentAt").value(nullValue()))
                .andExpect(jsonPath("$.lastSenderId").value(nullValue()))
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andReturn().getResponse().getContentAsString();
        long conversationId = ((Number) JsonPath.read(body, "$.id")).longValue();

        call(HttpMethod.POST, "/conversations", customer.access(),
                "{\"businessId\":%d}".formatted(owner.businessId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId));

        call(HttpMethod.GET, "/conversations", customer.access(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(conversationId))
                .andExpect(jsonPath("$[0].counterpartName").value("Taller de Ana"));

        call(HttpMethod.GET, "/conversations", owner.access(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(conversationId))
                .andExpect(jsonPath("$[0].counterpartName").value("Cami"));
    }

    @Test
    void sending_is_idempotent_and_history_supports_after_id() throws Exception {
        Session customer = customer("cliente@demo.cl", "Cami");
        Session owner = entrepreneur("duena@demo.cl", "Anita", "Ana", 1L, 1L);
        publish(owner.businessId());
        long conversationId = open(customer, owner.businessId());
        String uuid = UUID.randomUUID().toString();

        ResultActions first = send(customer, conversationId, uuid, "Hola")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId").value(conversationId))
                .andExpect(jsonPath("$.clientUuid").value(uuid))
                .andExpect(jsonPath("$.senderId").value(customer.userId()))
                .andExpect(jsonPath("$.content").value("Hola"))
                .andExpect(jsonPath("$.readAt").value(nullValue()));
        long firstId = sentId(first);

        send(customer, conversationId, uuid, "Hola")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId));

        long replyId = sentId(send(owner, conversationId, UUID.randomUUID().toString(), "Respuesta")
                .andExpect(status().isCreated()));

        call(HttpMethod.GET, "/conversations/%d/messages".formatted(conversationId), customer.access(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(firstId))
                .andExpect(jsonPath("$[1].id").value(replyId))
                .andExpect(jsonPath("$[1].content").value("Respuesta"));

        call(HttpMethod.GET, "/conversations/%d/messages?afterId=%d".formatted(conversationId, firstId),
                customer.access(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(replyId));

        call(HttpMethod.GET, "/conversations", customer.access(), null)
                .andExpect(jsonPath("$[0].lastMessage").value("Respuesta"))
                .andExpect(jsonPath("$[0].lastSenderId").value(owner.userId()))
                .andExpect(jsonPath("$[0].unreadCount").value(1));
    }

    @Test
    void the_same_uuid_from_the_other_sender_is_a_conflict() throws Exception {
        Session customer = customer("cliente@demo.cl", "Cami");
        Session owner = entrepreneur("duena@demo.cl", "Anita", "Ana", 1L, 1L);
        publish(owner.businessId());
        long conversationId = open(customer, owner.businessId());
        String uuid = UUID.randomUUID().toString();

        send(customer, conversationId, uuid, "Hola").andExpect(status().isCreated());

        send(owner, conversationId, uuid, "Mío")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MESSAGE_ID_CONFLICT"));
    }

    @Test
    void reading_marks_only_what_the_other_side_sent() throws Exception {
        Session customer = customer("cliente@demo.cl", "Cami");
        Session owner = entrepreneur("duena@demo.cl", "Anita", "Ana", 1L, 1L);
        publish(owner.businessId());
        long conversationId = open(customer, owner.businessId());

        send(owner, conversationId, UUID.randomUUID().toString(), "Primero")
                .andExpect(status().isCreated());
        send(owner, conversationId, UUID.randomUUID().toString(), "Segundo")
                .andExpect(status().isCreated());
        send(customer, conversationId, UUID.randomUUID().toString(), "Mío")
                .andExpect(status().isCreated());

        call(HttpMethod.POST, "/conversations/%d/read".formatted(conversationId), customer.access(), null)
                .andExpect(status().isNoContent());
        call(HttpMethod.POST, "/conversations/%d/read".formatted(conversationId), customer.access(), null)
                .andExpect(status().isNoContent());

        call(HttpMethod.GET, "/conversations/%d/messages".formatted(conversationId), customer.access(), null)
                .andExpect(jsonPath("$[0].readAt").isNotEmpty())
                .andExpect(jsonPath("$[1].readAt").isNotEmpty())
                .andExpect(jsonPath("$[2].readAt").value(nullValue()));
        call(HttpMethod.GET, "/conversations", customer.access(), null)
                .andExpect(jsonPath("$[0].unreadCount").value(0));

        call(HttpMethod.GET, "/conversations", owner.access(), null)
                .andExpect(jsonPath("$[0].unreadCount").value(1));
    }

    @Test
    void blocking_stops_both_sides_until_it_is_lifted() throws Exception {
        Session customer = customer("cliente@demo.cl", "Cami");
        Session owner = entrepreneur("duena@demo.cl", "Anita", "Ana", 1L, 1L);
        publish(owner.businessId());
        long conversationId = open(customer, owner.businessId());

        call(HttpMethod.PUT, "/conversations/%d/block".formatted(conversationId), customer.access(), null)
                .andExpect(status().isNoContent());
        call(HttpMethod.PUT, "/conversations/%d/block".formatted(conversationId), owner.access(), null)
                .andExpect(status().isNoContent());

        send(customer, conversationId, UUID.randomUUID().toString(), "Hola")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONVERSATION_BLOCKED"));
        send(owner, conversationId, UUID.randomUUID().toString(), "Hola")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONVERSATION_BLOCKED"));

        call(HttpMethod.DELETE, "/conversations/%d/block".formatted(conversationId), owner.access(), null)
                .andExpect(status().isNoContent());
        call(HttpMethod.DELETE, "/conversations/%d/block".formatted(conversationId), owner.access(), null)
                .andExpect(status().isNoContent());

        send(customer, conversationId, UUID.randomUUID().toString(), "Hola")
                .andExpect(status().isCreated());
    }

    @Test
    void outsiders_get_the_status_the_contract_declares() throws Exception {
        Session customer = customer("cliente@demo.cl", "Cami");
        Session owner = entrepreneur("duena@demo.cl", "Anita", "Ana", 1L, 1L);
        Session outsider = customer("outsider@demo.cl", "Out");
        publish(owner.businessId());
        long conversationId = open(customer, owner.businessId());

        call(HttpMethod.GET, "/conversations/%d/messages".formatted(conversationId), outsider.access(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        call(HttpMethod.GET, "/conversations", outsider.access(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        send(outsider, conversationId, UUID.randomUUID().toString(), "Hola")
                .andExpect(status().isForbidden());
        call(HttpMethod.POST, "/conversations/%d/read".formatted(conversationId), outsider.access(), null)
                .andExpect(status().isForbidden());
        call(HttpMethod.PUT, "/conversations/%d/block".formatted(conversationId), outsider.access(), null)
                .andExpect(status().isForbidden());
        call(HttpMethod.DELETE, "/conversations/%d/block".formatted(conversationId), outsider.access(), null)
                .andExpect(status().isNotFound());
    }

    @Test
    void opening_requires_a_public_business_and_a_customer() throws Exception {
        Session customer = customer("cliente@demo.cl", "Cami");
        Session pending = entrepreneur("pendiente@demo.cl", "Pendiente", "Pia", 1L, 1L);

        call(HttpMethod.POST, "/conversations", customer.access(),
                "{\"businessId\":%d}".formatted(pending.businessId()))
                .andExpect(status().isNotFound());

        publish(pending.businessId());
        call(HttpMethod.POST, "/conversations", pending.access(),
                "{\"businessId\":%d}".formatted(pending.businessId()))
                .andExpect(status().isForbidden());

        Session otherOwner = entrepreneur("otra@demo.cl", "Otra", "Olivia", 1L, 1L);
        publish(otherOwner.businessId());
        call(HttpMethod.POST, "/conversations", otherOwner.access(),
                "{\"businessId\":%d}".formatted(pending.businessId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reports_are_open_once_per_reporter_and_message() throws Exception {
        Session customer = customer("cliente@demo.cl", "Cami");
        Session owner = entrepreneur("duena@demo.cl", "Anita", "Ana", 1L, 1L);
        Session other = customer("otra-cliente@demo.cl", "Otra");
        publish(owner.businessId());
        long conversationId = open(customer, owner.businessId());
        long messageId = sentId(send(owner, conversationId, UUID.randomUUID().toString(), "Hola")
                .andExpect(status().isCreated()));

        long otherConversation = open(other, owner.businessId());
        long foreignMessage = sentId(send(other, otherConversation, UUID.randomUUID().toString(), "Hola")
                .andExpect(status().isCreated()));
        call(HttpMethod.POST, "/conversations/%d/reports".formatted(conversationId), customer.access(),
                "{\"reason\":\"OTHER\",\"messageId\":%d}".formatted(foreignMessage))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("messageId"));

        call(HttpMethod.POST, "/conversations/%d/reports".formatted(conversationId), customer.access(),
                "{\"reason\":\"SPAM\",\"detail\":\"Publicidad\",\"messageId\":%d}".formatted(messageId))
                .andExpect(status().isCreated());
        call(HttpMethod.POST, "/conversations/%d/reports".formatted(conversationId), customer.access(),
                "{\"reason\":\"HARASSMENT\",\"detail\":null,\"messageId\":null}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_OPEN"));
        call(HttpMethod.POST, "/conversations/%d/reports".formatted(conversationId), owner.access(),
                "{\"reason\":\"SCAM\",\"detail\":null,\"messageId\":null}")
                .andExpect(status().isCreated());

        call(HttpMethod.POST, "/conversations/%d/reports".formatted(otherConversation), other.access(),
                "{\"reason\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    void message_content_and_client_uuid_are_validated() throws Exception {
        Session customer = customer("cliente@demo.cl", "Cami");
        Session owner = entrepreneur("duena@demo.cl", "Anita", "Ana", 1L, 1L);
        publish(owner.businessId());
        long conversationId = open(customer, owner.businessId());

        send(customer, conversationId, UUID.randomUUID().toString(), "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("content"));
        send(customer, conversationId, UUID.randomUUID().toString(), "x".repeat(2001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("content"));
        call(HttpMethod.POST, "/conversations/%d/messages".formatted(conversationId), customer.access(),
                "{\"content\":\"Hola\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("clientUuid"));
    }
}
