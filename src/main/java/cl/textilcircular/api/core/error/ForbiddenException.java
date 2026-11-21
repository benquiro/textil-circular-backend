package cl.textilcircular.api.core.error;

import lombok.Getter;

/** Falló una verificación de participación, o el estado prohíbe la operación (p. ej. una conversación bloqueada). */
@Getter
public class ForbiddenException extends DomainException {

    private final String code;

    public ForbiddenException() {
        this("FORBIDDEN", "No tienes permiso para hacer esto");
    }

    public ForbiddenException(String message) {
        this("FORBIDDEN", message);
    }

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }
}
