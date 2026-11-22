package cl.textilcircular.api.feature.content;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.OutreachContentRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.support.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApiTest
class ContentApiTest {

    static final String EVENT = """
            {"type":"EVENT","title":"Feria de moda circular","body":null,"imageUrl":null,
             "eventAt":"2026-12-05T14:00:00Z","eventLocation":"Plaza de Armas"}""";

    @Autowired
    MockMvc mvc;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    OutreachContentRepository contents;
    @Autowired
    PasswordEncoder encoder;

    String admin;

    @BeforeEach
    void signIn() throws Exception {
        accounts.save(UserAccount.registerAdministrator("admin@demo.cl", "Demo1234", null, encoder, Instant.now()));
        admin = login("admin@demo.cl");
    }

    ResultActions call(HttpMethod method, String path, String token, String json) throws Exception {
        var builder = request(method, "/api/v1" + path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(json == null ? builder : builder.content(json));
    }

    String login(String email) throws Exception {
        String body = call(HttpMethod.POST, "/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"Demo1234\"}".formatted(email))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    long publish(String json) throws Exception {
        String body = call(HttpMethod.POST, "/contents", admin, json)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void administrator_publishes_and_guests_read_it() throws Exception {
        long id = publish(EVENT);

        call(HttpMethod.GET, "/contents/" + id, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EVENT"))
                .andExpect(jsonPath("$.title").value("Feria de moda circular"))
                .andExpect(jsonPath("$.eventAt").value("2026-12-05T14:00:00Z"))
                .andExpect(jsonPath("$.eventLocation").value("Plaza de Armas"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void list_is_paged_and_newest_first() throws Exception {
        publish("""
                {"type":"TEXT","title":null,"body":"Primero","imageUrl":null,"eventAt":null,"eventLocation":null}""");
        publish("""
                {"type":"TEXT","title":null,"body":"Segundo","imageUrl":null,"eventAt":null,"eventLocation":null}""");

        call(HttpMethod.GET, "/contents?page=0&size=1", null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].body").value("Segundo"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void required_fields_depend_on_the_type() throws Exception {
        call(HttpMethod.POST, "/contents", admin, """
                {"type":"ARTICLE","title":"Sin texto","body":null,"imageUrl":null,"eventAt":null,"eventLocation":null}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[0].field").value("body"));
        call(HttpMethod.POST, "/contents", admin, """
                {"type":"IMAGE","title":null,"body":null,"imageUrl":"javascript:alert(1)","eventAt":null,"eventLocation":null}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("imageUrl"));
        // A PUT-style body: a missing property is an error, not an empty value.
        call(HttpMethod.POST, "/contents", admin, "{\"type\":\"TEXT\",\"body\":\"Hola\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void stored_media_paths_come_back_absolute() throws Exception {
        long id = publish("""
                {"type":"IMAGE","title":null,"body":null,"imageUrl":"/media/afiche.png","eventAt":null,"eventLocation":null}""");

        call(HttpMethod.GET, "/contents/" + id, null, null)
                .andExpect(jsonPath("$.imageUrl").value(startsWith("http://")))
                .andExpect(jsonPath("$.imageUrl").value(endsWith("/media/afiche.png")));
    }

    @Test
    void update_replaces_every_field() throws Exception {
        long id = publish(EVENT);

        call(HttpMethod.PUT, "/contents/" + id, admin, """
                {"type":"TEXT","title":null,"body":"La feria se cambió de fecha","imageUrl":null,
                 "eventAt":"2026-12-05T14:00:00Z","eventLocation":"Plaza de Armas"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.title").value(nullValue()))
                // Event data is dropped when the content is no longer an event.
                .andExpect(jsonPath("$.eventAt").value(nullValue()))
                .andExpect(jsonPath("$.eventLocation").value(nullValue()));
    }

    @Test
    void deleted_content_disappears_but_stays_in_the_database() throws Exception {
        long id = publish(EVENT);

        call(HttpMethod.DELETE, "/contents/" + id, admin, null).andExpect(status().isNoContent());

        call(HttpMethod.GET, "/contents/" + id, null, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        call(HttpMethod.GET, "/contents", null, null).andExpect(jsonPath("$.totalElements").value(0));
        call(HttpMethod.PUT, "/contents/" + id, admin, EVENT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONTENT_DELETED"));
        call(HttpMethod.DELETE, "/contents/" + id, admin, null).andExpect(status().isNotFound());
        assertThat(contents.findById(id)).hasValueSatisfying(c -> assertThat(c.isDeleted()).isTrue());
    }

    @Test
    void unknown_content_is_not_found() throws Exception {
        call(HttpMethod.GET, "/contents/999999", null, null).andExpect(status().isNotFound());
        call(HttpMethod.PUT, "/contents/999999", admin, EVENT).andExpect(status().isNotFound());
    }
}
