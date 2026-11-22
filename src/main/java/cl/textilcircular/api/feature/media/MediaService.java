package cl.textilcircular.api.feature.media;

import cl.textilcircular.api.core.error.UnauthorizedException;
import cl.textilcircular.api.core.model.media.MediaFile;
import cl.textilcircular.api.core.repository.MediaFileRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.core.web.MediaUrls;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/** Simulated object storage: bytes go to local disk ({@code app.media.dir}), metadata to {@code media_file}. */
@Slf4j
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

    /**
     * Row first: if writing the file fails, the transaction rolls the row back. The file is deleted whenever
     * the transaction does not commit (failed write, failed commit), so no row and no file outlive each other.
     */
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
        Path target = dir.resolve(stored.getStoragePath());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException e) {
                        log.warn("Could not remove the orphan file {}", target, e);
                    }
                }
            }
        });
        try {
            Files.createDirectories(dir);
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Uploaded(MediaUrls.absolute(stored.publicPath()));
    }

    /** The declared type is client-controlled, so the file signature must agree with it. */
    private static boolean matchesType(String type, byte[] b) {
        return switch (type) {
            case "image/jpeg" -> startsWith(b, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(b, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/webp" -> startsWith(b, 'R', 'I', 'F', 'F') && b.length >= 12
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }

    private static boolean startsWith(byte[] b, int... signature) {
        if (b.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((b[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
