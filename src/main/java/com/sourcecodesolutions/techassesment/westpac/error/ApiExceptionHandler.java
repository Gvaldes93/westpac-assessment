package com.sourcecodesolutions.techassesment.westpac.error;

import com.sourcecodesolutions.techassesment.westpac.account.AccountNotFoundException;
import com.sourcecodesolutions.techassesment.westpac.account.CustomerAccountLimitExceededException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps exceptions to RFC 9457 problem details. Framework errors (malformed JSON, bad path variables) come from the base class. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    static final String ERRORS_PROPERTY = "errors";
    static final String VALIDATION_FAILED_DETAIL = "Request validation failed";
    static final String SERVICE_UNAVAILABLE_DETAIL = "The service is temporarily unavailable, please retry later";
    static final String INTERNAL_ERROR_DETAIL = "An unexpected error occurred";
    static final String RETRY_AFTER_SECONDS = "30";
    // SQL standard class 08 = connection exception.
    private static final String CONNECTION_EXCEPTION_SQL_STATE_CLASS = "08";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ex.getBody();
        problem.setDetail(VALIDATION_FAILED_DETAIL);
        problem.setProperty(ERRORS_PROPERTY, fieldErrors(ex));
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CustomerAccountLimitExceededException.class)
    ProblemDetail handleAccountLimitExceeded(CustomerAccountLimitExceededException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    /**
     * Database down or unreachable: tell clients to retry instead of returning a generic 500. A connection
     * lost mid-transaction can surface as an uncategorised error (e.g. a failed rollback), so the cause
     * chain is inspected rather than relying on the exception type alone.
     */
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ResponseEntity<ProblemDetail> handlePersistenceFailure(Exception ex) {
        if (isDatabaseUnavailable(ex)) {
            log.error("Database unavailable", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE_DETAIL));
        }
        return ResponseEntity.internalServerError().body(handleUnexpected(ex));
    }

    /** Last resort: log the cause, but never leak internals to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_DETAIL);
    }

    private static boolean isDatabaseUnavailable(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (isConnectionFailure(current)) {
                return true;
            }
            Throwable cause = current.getCause();
            // Guard against self-referencing cause chains.
            current = cause == current ? null : cause;
        }
        return false;
    }

    private static boolean isConnectionFailure(Throwable ex) {
        if (ex instanceof CannotCreateTransactionException
                || ex instanceof DataAccessResourceFailureException
                || ex instanceof TransientDataAccessException
                || ex instanceof SQLTransientConnectionException
                || ex instanceof SQLNonTransientConnectionException) {
            return true;
        }
        if (isTransactionControlFailureWithoutSqlState(ex)) {
            return true;
        }
        return ex instanceof SQLException sqlException
                && sqlException.getSQLState() != null
                && sqlException.getSQLState().startsWith(CONNECTION_EXCEPTION_SQL_STATE_CLASS);
    }

    /**
     * When the connection dies mid-transaction, the rollback fails on the pool's closed-connection proxy
     * with a SQLException carrying no SQLState, and Spring reports that instead of the original I/O error.
     * Genuine SQL errors at begin/commit/rollback (e.g. deferred constraints) always carry a SQLState.
     */
    private static boolean isTransactionControlFailureWithoutSqlState(Throwable ex) {
        return ex instanceof org.hibernate.TransactionException
                && ex.getCause() instanceof SQLException sqlException
                && sqlException.getSQLState() == null;
    }

    private static Map<String, List<String>> fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        TreeMap::new,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));
    }
}
