package hp.soft.config;

import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleConflict(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(IntegrityConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegrityConstraintViolation(IntegrityConstraintViolationException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("pm_idempotency_key")) {
            log.warn("Duplicate idempotency key detected");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Duplicate idempotency key"));
        }
        log.error("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Data integrity violation"));
    }
}
