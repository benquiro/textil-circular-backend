package cl.textilcircular.api.feature.metrics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Cuerpos de respuesta del panel de métricas, con los nombres del contrato de la API. */
final class MetricsDtos {

    private MetricsDtos() {
    }

    record Kpi(long value, @Schema(description = "La misma métrica en la ventana anterior; 0 para todo el período")
               long previous) {
    }

    @Schema(name = "MetricValue")
    record MetricValue(String label, long value) {
    }

    @Schema(name = "Metrics")
    record Metrics(long activeEntrepreneurs,
                   long newEntrepreneurs,
                   Kpi searches,
                   Kpi profileViews,
                   Kpi conversations,
                   long effectiveConversations,
                   Kpi registrations,
                   List<MetricValue> byRegion,
                   List<MetricValue> byService,
                   List<MetricValue> byAgeRange,
                   List<MetricValue> mostVisitedBusinesses) {
    }
}
