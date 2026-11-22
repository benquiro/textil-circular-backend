package cl.textilcircular.api.feature.media;

import cl.textilcircular.api.core.error.UnauthorizedException;
import cl.textilcircular.api.core.model.media.MediaFile;
import cl.textilcircular.api.core.repository.MediaFileRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.core.web.MediaUrls;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/** Simulated object storage: bytes go to local disk ({@code app.media.dir}), metadata to {@code media_file}. */
@Service
@RequiredArgsConstructor
class MediaService {

    record Uploaded(String url) {
    }

    private final MediaFileRepository files;
    private final UserAccountRepository accounts;
    private final Clock clock;

    @Value("${app.media.dir:media-storage}")
    private Path dir;

    /** Row first: if writing the file fails, the transaction rolls the row back. */
    @Transactional
    Uploaded upload(Caller caller, MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        MediaFile stored = MediaFile.register(accounts.findById(caller.userId()).orElseThrow(UnauthorizedException::new),
                file.getContentType(), bytes.length, clock.instant());
        if (!matchesType(stored.getContentType(), bytes)) {
            throw new ValidationException("file", "El contenido no corresponde a una imagen " + stored.getContentType());
        }
        files.saveAndFlush(stored);
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(stored.getStoragePath()), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Uploaded(MediaUrls.absolute(stored.publicPath()));
    }

    /** The declared type is client-controlled, so the file signature must agree with it. */
    private static boolean matchesType(String type, byte[] b) {
        return switch (type) {
            case "image/jpeg" -> b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "image/png" -> b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "image/webp" -> b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }
}
