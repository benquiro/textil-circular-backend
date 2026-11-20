package cl.textilcircular.api.feature.content;

import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.content.ContentDtos.Content;
import cl.textilcircular.api.feature.content.ContentDtos.ContentPage;
import cl.textilcircular.api.feature.content.ContentDtos.ContentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** News, posters, articles and events of the app's home screen ({@code /contents}). */
@RestController
@RequiredArgsConstructor
class ContentController {

    private final ContentService contents;

    @Operation(summary = "Listar contenidos de difusión", tags = "Contents",
            description = "Lista pública y paginada, del más reciente al más antiguo. No incluye el contenido eliminado.")
    @SecurityRequirements
    @GetMapping("/contents")
    ContentPage list(@Parameter(description = "Número de página, desde 0") @RequestParam(defaultValue = "0") int page,
                     @Parameter(description = "Tamaño de página, máximo 50") @RequestParam(defaultValue = "20") int size) {
        return contents.list(page, size);
    }

    @Operation(summary = "Ver un contenido", tags = "Contents", description = "Responde 404 si no existe o fue eliminado.")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: no existe o fue eliminado")
    @SecurityRequirements
    @GetMapping("/contents/{contentId}")
    Content get(@PathVariable long contentId) {
        return contents.get(contentId);
    }

    @Operation(summary = "Publicar un contenido (administrador)", tags = "Contents",
            description = "TEXT requiere body; IMAGE, imageUrl; ARTICLE, title y body; EVENT, title y eventAt.")
    @ApiResponse(responseCode = "201", description = "Publicado")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: quien llama no es administrador")
    @PostMapping("/contents")
    @ResponseStatus(HttpStatus.CREATED)
    Content publish(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @RequestBody ContentRequest request) {
        return contents.publish(Caller.of(jwt), request);
    }

    @Operation(summary = "Editar un contenido (administrador)", tags = "Contents",
            description = "Reemplazo completo, con las mismas reglas por tipo que al publicar.")
    @ApiResponse(responseCode = "200", description = "Actualizado")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: el contenido no existe")
    @ApiResponse(responseCode = "409", description = "`CONTENT_DELETED`: el contenido fue eliminado")
    @PutMapping("/contents/{contentId}")
    Content update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable long contentId,
                   @RequestBody ContentRequest request) {
        return contents.update(Caller.of(jwt), contentId, request);
    }

    @Operation(summary = "Eliminar un contenido (administrador)", tags = "Contents",
            description = "Borrado lógico: deja de verse, pero se conserva en la base de datos.")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: no existe o ya fue eliminado")
    @DeleteMapping("/contents/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable long contentId) {
        contents.delete(Caller.of(jwt), contentId);
    }
}
