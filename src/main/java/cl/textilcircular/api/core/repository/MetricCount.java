package cl.textilcircular.api.core.repository;

/** Una fila con etiqueta de un desglose de métricas, p. ej. una región y cuántos emprendedores activos tiene. */
public record MetricCount(String label, long value) {
}
