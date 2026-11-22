package cl.textilcircular.api.feature.media;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.MediaFileRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.support.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApiTest
@TestPropertySource(properties = "app.media.dir=target/test-media")
class MediaApiTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Autowired
    MockMvc mvc;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    MediaFileRepository files;
    @Autowired
    PasswordEncoder encoder;

    String token;

    @BeforeEach
    void signIn() throws Exception {
        accounts.save(UserAccount.registerAdministrator("admin@demo.cl", "Demo1234", null, encoder, Instant.now()));
        String body = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.cl\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = JsonPath.read(body, "$.accessToken");
    }

    MockMultipartHttpServletRequestBuilder upload(String type, byte[] bytes, String auth) {
        var builder = multipart("/api/v1/media").file(new MockMultipartFile("file", "foto", type, bytes));
        return auth == null ? builder : (MockMultipartHttpServletRequestBuilder) builder.header("Authorization", "Bearer " + auth);
    }

    @Test
    void uploaded_image_returns_an_absolute_url_that_serves_the_same_bytes_without_a_token() throws Exception {
        String body = mvc.perform(upload("image/png", PNG, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.matchesPattern("http://localhost/media/[0-9a-f-]{36}\\.png")))
                .andReturn().getResponse().getContentAsString();
        String path = JsonPath.<String>read(body, "$.url").replace("http://localhost", "");

        byte[] served = mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(served).isEqualTo(PNG);
        assertThat(files.count()).isEqualTo(1);
    }

    @Test
    void requires_a_session() throws Exception {
        mvc.perform(upload("image/png", PNG, null)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejects_a_type_that_is_not_jpg_png_or_webp() throws Exception {
        mvc.perform(upload("text/plain", PNG, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[0].field").value("file"));
        assertThat(files.count()).isZero();
    }

    @Test
    void rejects_bytes_that_do_not_match_the_declared_type() throws Exception {
        mvc.perform(upload("image/png", "<html></html>".getBytes(), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("file"));
        assertThat(files.count()).isZero();
    }

    @Test
    void rejects_an_image_over_5_mb() throws Exception {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        mvc.perform(upload("image/png", big, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("file"));
    }

    @Test
    void rejects_a_request_without_the_file_part() throws Exception {
        mvc.perform(multipart("/api/v1/media").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[0].field").value("file"));
    }

    @Test
    void unknown_file_is_404_and_path_traversal_is_not_served() throws Exception {
        mvc.perform(get("/media/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
        mvc.perform(get("/media/..%2Fapplication.yaml")).andExpect(status().is4xxClientError());
    }
}
