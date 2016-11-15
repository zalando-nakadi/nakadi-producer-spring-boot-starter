package org.zalando.tarbelaproducer.service.exception;

import com.google.common.annotations.VisibleForTesting;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> validationErrors;

    public ValidationException(final List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    @Override
    public String getMessage() {
        final StringBuilder builder = new StringBuilder();
        final String validationErrors = String.join(", ", this.validationErrors);
        builder.append("The validation resulted in the following errors: ").append(validationErrors);
        return builder.toString();
    }

    @VisibleForTesting
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
