package cl.textilcircular.api.core.error;

import lombok.Getter;

/** A participant check failed, or the state forbids the operation (e.g. a blocked conversation). */
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
