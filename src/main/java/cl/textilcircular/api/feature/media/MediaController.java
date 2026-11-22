package cl.textilcircular.api.feature.media;

import cl.textilcircular.api.core.security.Caller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Image upload ({@code POST /media}); the files are served from {@code GET /media/**}, outside the API prefix. */
@RestController
@RequiredArgsConstructor
class MediaController {

    private final MediaService media;

    @Operation(summary = "Subir una imagen", tags = "Media",
            description = "JPG, PNG o WEBP de hasta 5 MB. Devuelve la URL absoluta para usar en photoUrl, photoUrls o imageUrl.")
    @ApiResponse(responseCode = "201", description = "Guardada")
    @ApiResponse(responseCode = "400", description = "`VALIDATION`: falta el archivo, no es JPG, PNG o WEBP, está vacío o pesa más de 5 MB")
    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    MediaService.Uploaded upload(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                 @RequestPart("file") MultipartFile file) {
        return media.upload(Caller.of(jwt), file);
    }
}
