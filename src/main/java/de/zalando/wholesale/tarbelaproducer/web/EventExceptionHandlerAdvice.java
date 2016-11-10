package de.zalando.wholesale.tarbelaproducer.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import de.zalando.wholesale.tarbelaproducer.TarbelaSnapshotProviderNotImplementedException;
import de.zalando.wholesale.tarbelaproducer.api.event.model.ProblemDTO;
import de.zalando.wholesale.tarbelaproducer.service.exception.InvalidCursorException;
import de.zalando.wholesale.tarbelaproducer.service.exception.InvalidEventIdException;
import de.zalando.wholesale.tarbelaproducer.service.exception.UnknownEventIdException;
import de.zalando.wholesale.tarbelaproducer.service.exception.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class EventExceptionHandlerAdvice {

    private final FlowIdComponent flowIdComponent;

    private static final String CONTENT_TYPE_PROBLEM = "application/problem+json";

    @Autowired
    public EventExceptionHandlerAdvice(FlowIdComponent flowIdComponent) {
        this.flowIdComponent = flowIdComponent;
    }

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity<ProblemDTO> onInvalidCursorException(final InvalidCursorException exception) {
        final ProblemDTO error = getErrorForUnProcessableEntity(
                "Invalid cursor format", exception.getMessage());
        return getErrorResponseEntity(HttpStatus.UNPROCESSABLE_ENTITY, error);
    }

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity<ProblemDTO> onInvalidEventIdException(final InvalidEventIdException exception) {
        final ProblemDTO error = getErrorForUnProcessableEntity(
                "Invalid event id format", exception.getMessage());
        return getErrorResponseEntity(HttpStatus.UNPROCESSABLE_ENTITY, error);
    }

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity<ProblemDTO> onUnknownEventIdException(final UnknownEventIdException exception) {
        final ProblemDTO error = getErrorForUnProcessableEntity(
                "No event log found", exception.getMessage());
        return getErrorResponseEntity(HttpStatus.UNPROCESSABLE_ENTITY, error);
    }

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity<ProblemDTO> onValidationException(final ValidationException exception) {
        final ProblemDTO error = getErrorForBadRequest("Validation Error", exception.getMessage());
        return getErrorResponseEntity(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity<ProblemDTO> onTarbelaSnapshotProviderNotImplemented(final TarbelaSnapshotProviderNotImplementedException exception) {
        final ProblemDTO error = getErrorForNotImplemented("Snapshot not implemented", exception.getMessage());
        return getErrorResponseEntity(HttpStatus.NOT_IMPLEMENTED, error);
    }

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity<ProblemDTO> onRuntimeException(final RuntimeException runtimeException) {
        log.error("Unknown exception occurred while processing request: ", runtimeException);

        final ProblemDTO error = getErrorForInternalServerError("Unknown Exception",
                "The request could not be processed. An unknown exception occurred when processing the request");
        return getErrorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, error);
    }

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity<ProblemDTO> onHttpMessageNotReadableException(
            final HttpMessageNotReadableException httpMessageNotReadableException) {
        log.debug("http message parsing error occurred:", httpMessageNotReadableException);

        String detailMessage = Optional.ofNullable(httpMessageNotReadableException.getMostSpecificCause())
                .filter(e -> e instanceof InvalidFormatException)
                .map(e -> (InvalidFormatException) e)
                .map(InvalidFormatException::getOriginalMessage)
                .orElse("Request body is missing or malformed");

        final ProblemDTO error = getErrorForBadRequest("Bad Request", detailMessage);
        return getErrorResponseEntity(HttpStatus.BAD_REQUEST, error);
    }

    private ProblemDTO getErrorForUnProcessableEntity(final String title, final String detail) {
        final ProblemDTO error = new ProblemDTO();
        error.setTitle(title);
        error.setDetail(detail);
        error.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        error.setType("http://httpstatus.es/422");
        error.setInstance(flowIdComponent.getXFlowIdKey() + ":" + flowIdComponent.getXFlowIdValue());
        return error;
    }

    private ProblemDTO getErrorForBadRequest(final String title, final String detail) {
        final ProblemDTO error = new ProblemDTO();
        error.setTitle(title);
        error.setDetail(detail);
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setType("http://httpstatus.es/400");
        error.setInstance(flowIdComponent.getXFlowIdKey() + ":" + flowIdComponent.getXFlowIdValue());
        return error;
    }

    private ProblemDTO getErrorForInternalServerError(final String title, final String detail) {
        final ProblemDTO error = new ProblemDTO();
        error.setTitle(title);
        error.setDetail(detail);
        error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setType("http://httpstatus.es/500");
        error.setInstance(flowIdComponent.getXFlowIdKey() + ":" + flowIdComponent.getXFlowIdValue());
        return error;
    }

    private ProblemDTO getErrorForNotImplemented(final String title, final String detail) {
        final ProblemDTO error = new ProblemDTO();
        error.setTitle(title);
        error.setDetail(detail);
        error.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
        error.setType("http://httpstatus.es/501");
        error.setInstance(flowIdComponent.getXFlowIdKey() + ":" + flowIdComponent.getXFlowIdValue());
        return error;
    }

    private ResponseEntity<ProblemDTO> getErrorResponseEntity(final HttpStatus httpStatus, final ProblemDTO error) {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.parseMediaType(CONTENT_TYPE_PROBLEM));
        return new ResponseEntity<>(error, httpHeaders, httpStatus);
    }

}
