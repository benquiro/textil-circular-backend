package cl.textilcircular.api.core.web;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Image links leave the API absolute (README: convenciones). The database stores either an external
 * {@code http(s)://} link or a file of the local media storage ({@code /media/...}); the second one is
 * completed with the scheme, host and port of the current request.
 */
public final class MediaUrls {

    private MediaUrls() {
    }

    /** {@code null} stays {@code null}; absolute links are returned as they are. */
    public static String absolute(String stored) {
        if (stored == null || !stored.startsWith("/")) {
            return stored;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(stored).toUriString();
    }
}
