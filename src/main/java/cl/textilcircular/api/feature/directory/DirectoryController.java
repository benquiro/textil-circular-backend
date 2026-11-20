package cl.textilcircular.api.feature.directory;

import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.directory.DirectoryDtos.BusinessCardPage;
import cl.textilcircular.api.feature.directory.DirectoryDtos.BusinessDetail;
import cl.textilcircular.api.feature.directory.DirectoryDtos.ContactClickRequest;
import cl.textilcircular.api.feature.directory.DirectoryDtos.ProductDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public directory: search, business page and product page. The session is optional; when a token is sent
 * it is still validated, and it lets an owner see what is not public yet.
 */
@RestController
@RequiredArgsConstructor
class DirectoryController {

    private final DirectoryService directory;

    @Operation(summary = "Buscar emprendimientos (directorio)", tags = "Directory",
            description = "Solo emprendimientos aprobados, visibles y de cuentas activas. Orden pseudoaleatorio "
                    + "estable para un mismo `seed`. Registra un evento DIRECTORY_SEARCH.")
    @ApiResponse(responseCode = "200", description = "Una página de tarjetas; `totalElements` es la cantidad de resultados")
    @SecurityRequirements
    @GetMapping("/businesses")
    BusinessCardPage search(@RequestParam(required = false) Long regionId,
                            @RequestParam(required = false) Long communeId,
                            @RequestParam(required = false) Long serviceId,
                            @Parameter(description = "Búsqueda en nombre y descripción") @RequestParam(required = false) String q,
                            @Parameter(description = "Semilla del orden, la misma en todas las páginas") @RequestParam int seed,
                            @Parameter(description = "Número de página, desde 0") @RequestParam(defaultValue = "0") int page,
                            @Parameter(description = "Tamaño de página, máximo 50") @RequestParam(defaultValue = "20") int size) {
        return directory.search(q, regionId, communeId, serviceId, seed, page, size);
    }

    @Operation(summary = "Ver un emprendimiento", tags = "Directory",
            description = "Ficha pública con servicios y productos activos. Si no es pública, solo su dueño la ve. "
                    + "Registra un evento BUSINESS_VIEW.")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: no existe o no es público")
    @GetMapping("/businesses/{businessId}")
    BusinessDetail business(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable long businessId) {
        return directory.business(callerOrNull(jwt), businessId);
    }

    @Operation(summary = "Registrar un clic de contacto", tags = "Directory",
            description = "La app lo llama al tocar el botón de contacto. Registra un evento CONTACT_CLICK.")
    @ApiResponse(responseCode = "204", description = "Registrado")
    @PostMapping("/businesses/{businessId}/contact-clicks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void contactClick(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable long businessId,
                      @RequestBody(required = false) ContactClickRequest request) {
        directory.recordContactClick(callerOrNull(jwt), businessId);
    }

    @Operation(summary = "Ver un producto", tags = "Directory",
            description = "Detalle del producto con la tarjeta de su emprendimiento. Registra un evento PRODUCT_VIEW.")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: no existe o no es público")
    @GetMapping("/products/{productId}")
    ProductDetail product(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable long productId) {
        return directory.product(callerOrNull(jwt), productId);
    }

    /** Guests reach these routes without a token, so the principal may be absent. */
    private static Caller callerOrNull(Jwt jwt) {
        return jwt == null ? null : Caller.of(jwt);
    }
}
