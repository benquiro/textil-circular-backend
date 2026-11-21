package cl.textilcircular.api.core.web;

import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.DomainException;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.NotFoundException;
import cl.textilcircular.api.core.error.UnauthorizedException;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.model.account.AccountErrors;
import cl.textilcircular.api.core.model.account.AccountNotActiveException;
import cl.textilcircular.api.core.model.account.InvalidCredentialsException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Mapea cada fallo al cuerpo de problema del contrato. Los mensajes de dominio pasan sin cambios. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    ProblemDetail validation(ValidationException e) {
        return Problems.validation(e.getField(), e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException e) {
        return Problems.of(HttpStatus.CONFLICT, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return Problems.of(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage());
    }

    @ExceptionHandler(AccountNotActiveException.class)
    ProblemDetail accountNotActive(AccountNotActiveException e) {
        ProblemDetail problem = Problems.of(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", e.getMessage());
        problem.setProperty("accountStatus", e.getStatus());
        return problem;
    }

    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail unauthorized(UnauthorizedException e) {
        return Problems.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail forbidden(ForbiddenException e) {
        return Problems.of(HttpStatus.FORBIDDEN, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail otherDomain(DomainException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    /**
     * Claves únicas golpeadas por una petición concurrente después de que la propia verificación
     * del servicio pasara. Las dos para las que la aplicación tiene mensaje conservan su código;
     * solo se registra el nombre de la restricción, nunca el valor en conflicto.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataIntegrity(DataIntegrityViolationException e) {
        String constraint = constraintName(e);
        log.info("Rejected by a database constraint: {}", constraint);
        return switch (constraint == null ? "" : constraint) {
            case "uk_user_account_email" -> conflict(AccountErrors.emailTaken());
            case "uk_entrepreneur_rut" -> conflict(AccountErrors.rutTaken());
            case "uk_message_client_uuid" -> conflict(new ConflictException("MESSAGE_ID_CONFLICT",
                    "Ese identificador de mensaje ya fue usado"));
            case "uk_conversation_report_open" -> conflict(new ConflictException("REPORT_ALREADY_OPEN",
                    "Ya tienes un reporte abierto en esta conversación"));
            default -> Problems.of(HttpStatus.CONFLICT, "CONFLICT",
                    "Los datos cambiaron mientras guardabas. Inténtalo de nuevo");
        };
    }

    private static @Nullable String constraintName(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail optimisticLock(OptimisticLockingFailureException e) {
        return Problems.of(HttpStatus.CONFLICT, "CONFLICT", "Los datos cambiaron mientras guardabas. Inténtalo de nuevo");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeMismatch(MethodArgumentTypeMismatchException e) {
        return Problems.validation(e.getName(), "El valor no es válido");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("Unexpected error", e);
        return Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Algo salió mal. Inténtalo de nuevo");
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = missingProperty(ex)
                .map(field -> Problems.validation(field, Problems.MISSING_PROPERTY_MESSAGE))
                .orElseGet(() -> Problems.of(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", Problems.VALIDATION_DETAIL));
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Cómo formula Jackson el mensaje de una propiedad {@code @JsonProperty(required = true)}
     * omitida en un cuerpo. Jackson no tiene un tipo de excepción propio para esto (los tipos
     * incorrectos lanzan la misma {@link MismatchedInputException}), así que el mensaje es la
     * única señal. ProfileApiTest y AuthApiTest fallan si una actualización cambia su redacción.
     */
    private static final String JACKSON_MISSING_PROPERTY = "Missing required creator property";

    /**
     * Ruta con puntos de la propiedad que un cuerpo omitió ("business.photoUrl"), cuando ese es el
     * fallo. Se omiten los índices de lista: el contrato nombra un campo de lista sin su índice.
     */
    private static Optional<String> missingProperty(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()
                    && String.valueOf(mismatch.getOriginalMessage()).startsWith(JACKSON_MISSING_PROPERTY)) {
                return Optional.of(mismatch.getPath().stream()
                        .map(ref -> ref.getPropertyName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(".")));
            }
        }
        return Optional.empty();
    }

    /** Los errores propios de Spring MVC (404, 405, 415...) también reciben la extensión {@code code}. */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body == null && ex instanceof ErrorResponse error) {
            // Algunos manejadores (404 para una ruta desconocida) dejan el cuerpo para que lo construya la superclase.
            body = error.getBody();
        }
        if (body instanceof ProblemDetail problem && (problem.getProperties() == null
                || !problem.getProperties().containsKey("code"))) {
            problem.setProperty("code", Problems.codeFor(statusCode));
            if (statusCode.value() == 404) {
                problem.setDetail(new NotFoundException().getMessage());
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
