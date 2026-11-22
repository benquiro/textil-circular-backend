package cl.textilcircular.api.feature.media;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.MediaFileRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.support.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The media directory is a regular file, so the disk write fails after the row was flushed. */
@ApiTest
@TestPropertySource(properties = "app.media.dir=target/media-dir-is-a-file")
class MediaWriteFailureTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    MediaFileRepository files;
    @Autowired
    PasswordEncoder encoder;

    @BeforeAll
    static void makeTheDirectoryAFile() throws IOException {
        Path blocker = Path.of("target/media-dir-is-a-file");
        Files.createDirectories(blocker.getParent());
        Files.writeString(blocker, "not a directory");
    }

    @Test
    void a_failed_disk_write_rolls_the_row_back() throws Exception {
        accounts.save(UserAccount.registerAdministrator("admin@demo.cl", "Demo1234", null, encoder, Instant.now()));
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.cl\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.accessToken");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

        mvc.perform(multipart("/api/v1/media").file(new MockMultipartFile("file", "f", "image/png", png))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());

        assertThat(files.count()).isZero();
    }
}
