# Integración de Entrepreneur en main

Este ZIP conserva el backend Spring Boot completo de `main` y añade los controladores, DTO, servicio y una migración para `serviceId`.

## Endpoints
- GET/PUT `/api/v1/me/business`; PUT `/api/v1/me/business/visibility`
- GET/POST `/api/v1/me/products`; GET/PUT/DELETE `/api/v1/me/products/{productId}`
- PUT `/api/v1/me/products/{productId}/visibility`
- GET `/api/v1/reviews/pending`; POST `/api/v1/businesses/{businessId}/reviews`

El usuario se determina desde el JWT. El modelo compartido sigue siendo la fuente de reglas de negocio.

## Cambios sobre main
- `src/main/java/cl/textilcircular/api/feature/entrepreneur/` (3 archivos nuevos)
- `src/main/java/cl/textilcircular/api/core/model/product/Product.java` (asociación opcional `service`)
- `src/main/resources/db/migration/V11__product_service.sql` (columna + índice)
- `src/test/java/cl/textilcircular/api/feature/entrepreneur/EntrepreneurServiceTest.java` (5 pruebas unitarias iniciales; no equivalen a cobertura del 80%)

## Ejecutar
Requiere JDK 25 y Docker. Desde la raíz del proyecto: `./mvnw test` y `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (en Windows, `mvnw.cmd`).

**Estado:** integración de código; no se pudo ejecutar la suite ni validar el arranque en este entorno. Antes de mergear, ejecutar tests y revisar que `V11` no choque con nuevas migraciones del equipo. Este ZIP no integra el proyecto Android ni reemplaza el trabajo de las otras branches. Las URLs de imágenes deben provenir de la API de medios (`POST /media`); aquí se reciben como URLs absolutas.
