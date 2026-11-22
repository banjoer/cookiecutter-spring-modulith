package {{ cookiecutter.package_name }}.infrastructure.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import {{ cookiecutter.package_name }}.shared.error.ConflictException;
import {{ cookiecutter.package_name }}.shared.error.DomainException;
import {{ cookiecutter.package_name }}.shared.error.DomainValidationException;
import {{ cookiecutter.package_name }}.shared.error.NotFoundException;

/**
 * Translates expected failures into RFC 9457 problem responses without leaking stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request);
    }

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail validation(DomainValidationException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", exception.getMessage(), request);
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Request failed", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail beanValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid.", request);
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Data integrity violation", exception);
        return problem(HttpStatus.CONFLICT, "Conflict", "The request conflicts with existing data.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "Access is denied.", request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled request failure", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "The request could not be completed.", request);
    }

    private static ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }
}
