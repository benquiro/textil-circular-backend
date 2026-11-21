package cl.textilcircular.api.feature.messaging;

import cl.textilcircular.api.core.realtime.RealtimeSessions;
import cl.textilcircular.api.support.Fixtures;
import cl.textilcircular.api.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los eventos del contrato por WebSocket: {@code message.created} llega solo al otro participante
 * cuando se guarda un mensaje, y {@code message.read} al remitente cuando la contraparte lee.
 * Necesita un puerto real porque MockMvc no puede subir una conexión.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Sql(statements = "TRUNCATE user_account CASCADE", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MessagingRealtimeTest {

    private static final AtomicLong RUTS = new AtomicLong(2000);

    @LocalServerPort
    int port;
    @Autowired
    RealtimeSessions realtime;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JsonMapper json;

    final HttpClient http = HttpClient.newHttpClient();

    record Session(String access, long userId, long businessId) {
    }

    /** Recopila cada frame completo que empuja el servidor. */
    static class Collector implements WebSocket.Listener {
        final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        final StringBuilder partial = new StringBuilder();

        @Override
        public void onOpen(WebSocket socket) {
            socket.request(1);
        }

        /** El siguiente frame completo, o {@code null} si no llega nada en el tiempo de espera. */
        String poll(long timeout, TimeUnit unit) throws InterruptedException {
            return frames.poll(timeout, unit);
        }

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                frames.add(partial.toString());
                partial.setLength(0);
            }
            socket.request(1);
            return null;
        }
    }

    HttpResponse<String> post(String path, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode bodyOf(HttpResponse<String> response) {
        return json.readTree(response.body());
    }

    Session customer(String email, String alias) throws Exception {
        JsonNode body = bodyOf(post("/api/v1/auth/register/customer",
                "{\"email\":\"%s\",\"password\":\"Secreta123\",\"alias\":\"%s\"}".formatted(email, alias), null));
        return new Session(body.get("accessToken").asString(), body.get("user").get("id").asLong(), 0);
    }

    Session owner(String email, String alias, String firstName) throws Exception {
        JsonNode body = bodyOf(post("/api/v1/auth/register/entrepreneur", """
                {"email":"%s","password":"Secreta123","alias":"%s","rut":"%s","firstName":"%s","lastName":"Pérez",
                 "ageRangeId":2,"genderId":1,
                 "business":{"name":"Taller de %s","description":"%s","photoUrl":null,"communeId":1,
                             "serviceIds":[1]}}""".formatted(email, alias, Fixtures.rutFor(RUTS.incrementAndGet()),
                firstName, firstName, Fixtures.BUSINESS_DESCRIPTION), null));
        long userId = body.get("user").get("id").asLong();
        long businessId = jdbc.queryForObject("select business_id from business where user_id = ?", Long.class, userId);
        jdbc.update("update business set status = 'APPROVED', approved_at = now() where business_id = ?", businessId);
        return new Session(body.get("accessToken").asString(), userId, businessId);
    }

    long openConversation(Session customer, long businessId) throws Exception {
        JsonNode body = bodyOf(post("/api/v1/conversations", "{\"businessId\":%d}".formatted(businessId),
                customer.access()));
        return body.get("id").asLong();
    }

    long sendMessage(Session sender, long conversationId, String clientUuid, String content) throws Exception {
        JsonNode body = bodyOf(post("/api/v1/conversations/%d/messages".formatted(conversationId),
                "{\"clientUuid\":\"%s\",\"content\":\"%s\"}".formatted(clientUuid, content), sender.access()));
        return body.get("id").asLong();
    }

    WebSocket connect(Session session, Collector collector) throws Exception {
        WebSocket socket = http.newWebSocketBuilder()
                .header("Authorization", "Bearer " + session.access())
                .buildAsync(URI.create("ws://localhost:" + port + "/api/v1/ws"), collector).get(5, TimeUnit.SECONDS);
        awaitTrue(() -> realtime.connectionCount(session.userId()) == 1);
        return socket;
    }

    static void awaitTrue(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met within 5 s");
            }
            Thread.sleep(50);
        }
    }

    @Test
    void sending_a_message_pushes_message_created_to_the_other_participant_only() throws Exception {
        Session customer = customer("ws-cliente@demo.cl", "Cami");
        Session owner = owner("ws-duena@demo.cl", "Anita", "Ana");
        long conversationId = openConversation(customer, owner.businessId());
        Collector ownerFrames = new Collector();
        Collector customerFrames = new Collector();
        WebSocket ownerSocket = connect(owner, ownerFrames);
        WebSocket customerSocket = connect(customer, customerFrames);
        String clientUuid = "3f6b1a2c-9d84-4f2e-a1b7-6c5e4d3f2a10";

        long messageId = sendMessage(customer, conversationId, clientUuid, "Hola, ¿tienen la chaqueta?");

        JsonNode frame = json.readTree(ownerFrames.poll(5, TimeUnit.SECONDS));
        assertThat(frame.get("type").asString()).isEqualTo("message.created");
        JsonNode data = frame.get("data");
        assertThat(data.get("id").asLong()).isEqualTo(messageId);
        assertThat(data.get("conversationId").asLong()).isEqualTo(conversationId);
        assertThat(data.get("clientUuid").asString()).isEqualTo(clientUuid);
        assertThat(data.get("senderId").asLong()).isEqualTo(customer.userId());
        assertThat(data.get("content").asString()).isEqualTo("Hola, ¿tienen la chaqueta?");
        assertThat(data.get("sentAt").asString()).isNotBlank();
        assertThat(data.get("readAt").isNull()).isTrue();
        assertThat(data.get("senderName").asString()).isEqualTo("Cami");
        // El contrato lo envía solo al otro participante: el remitente no recibe el evento.
        assertThat(customerFrames.poll(300, TimeUnit.MILLISECONDS)).isNull();

        ownerSocket.abort();
        customerSocket.abort();
    }

    @Test
    void marking_the_conversation_read_pushes_message_read_to_the_remitter() throws Exception {
        Session customer = customer("ws-cliente2@demo.cl", "Cami");
        Session owner = owner("ws-duena2@demo.cl", "Anita", "Ana");
        long conversationId = openConversation(customer, owner.businessId());
        sendMessage(customer, conversationId, "0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9", "¿La reservo?");
        Collector customerFrames = new Collector();
        Collector ownerFrames = new Collector();
        WebSocket customerSocket = connect(customer, customerFrames);
        WebSocket ownerSocket = connect(owner, ownerFrames);

        assertThat(post("/api/v1/conversations/%d/read".formatted(conversationId), null, owner.access()).statusCode())
                .isEqualTo(204);

        JsonNode frame = json.readTree(customerFrames.poll(5, TimeUnit.SECONDS));
        assertThat(frame.get("type").asString()).isEqualTo("message.read");
        JsonNode data = frame.get("data");
        assertThat(data.get("conversationId").asLong()).isEqualTo(conversationId);
        assertThat(data.get("readAt").asString()).isNotBlank();
        // El lector no recibe nada: el aviso va al remitente.
        assertThat(ownerFrames.poll(300, TimeUnit.MILLISECONDS)).isNull();

        customerSocket.abort();
        ownerSocket.abort();
    }
}
