package cl.textilcircular.api.feature.content;

import cl.textilcircular.api.core.model.outreach.ContentType;
import cl.textilcircular.api.core.model.outreach.OutreachContent;
import cl.textilcircular.api.core.web.MediaUrls;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** Request and response bodies of {@code /contents}, as named in the API contract. */
final class ContentDtos {

    private ContentDtos() {
    }

    /**
     * Body of the publish and edit operations. Every property must be present ({@code null} when it does
     * not apply): which ones are required depends on {@code type}, and {@link OutreachContent} checks it.
     */
    @Schema(name = "ContentRequest")
    record ContentRequest(@JsonProperty(required = true) ContentType type,
                          @JsonProperty(required = true) @Schema(example = "Feria de moda circular") String title,
                          @JsonProperty(required = true) String body,
                          @JsonProperty(required = true) @Schema(description = "URL http(s) o la ruta devuelta por POST /media") String imageUrl,
                          @JsonProperty(required = true) @Schema(description = "Solo eventos") Instant eventAt,
                          @JsonProperty(required = true) @Schema(description = "Solo eventos, opcional") String eventLocation) {
    }

    @Schema(name = "Content")
    record Content(long id, ContentType type, String title, String body, String imageUrl, Instant eventAt,
                   String eventLocation, Instant createdAt) {

        static Content of(OutreachContent content) {
            return new Content(content.getId(), content.getType(), content.getTitle(), content.getBody(),
                    MediaUrls.absolute(content.getImageUrl()), content.getEventAt(), content.getEventLocation(),
                    content.getCreatedAt());
        }
    }

    @Schema(name = "ContentPage")
    record ContentPage(List<Content> content, @Schema(description = "Base cero") int page, int size,
                       long totalElements, @Schema(description = "Verdadero en la última página") boolean last) {
    }
}
