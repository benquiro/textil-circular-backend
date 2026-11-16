package cl.textilcircular.api.feature.metrics;

import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.metrics.MetricsDtos.Metrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Metrics panel for administrators ({@code GET /admin/metrics}). */
@RestController
@RequiredArgsConstructor
class MetricsController {

    private final MetricsService metrics;

    @Operation(summary = "Ver métricas (administrador)", tags = "Admin",
            description = "Emprendedores activos y nuevos, búsquedas, visitas, conversaciones, registros y desgloses. "
                    + "Cada KPI trae la ventana anterior; `days` acepta 30 o 90.")
    @ApiResponse(responseCode = "200", description = "Resumen de métricas")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: quien llama no es administrador")
    @GetMapping("/admin/metrics")
    Metrics get(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                @Parameter(description = "Ventana en días: 30 o 90; omitir para todo el período")
                @RequestParam(required = false) Integer days,
                @Parameter(description = "Solo emprendimientos de esta región")
                @RequestParam(required = false) Long regionId,
                @Parameter(description = "Solo emprendimientos con este servicio")
                @RequestParam(required = false) Long serviceId) {
        return metrics.get(Caller.of(jwt), days, regionId, serviceId);
    }
}
