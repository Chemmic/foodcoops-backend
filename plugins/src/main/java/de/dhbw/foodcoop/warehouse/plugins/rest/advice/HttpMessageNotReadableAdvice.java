package de.dhbw.foodcoop.warehouse.plugins.rest.advice;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

@ControllerAdvice
public class HttpMessageNotReadableAdvice {

    private static final Logger log =
            LoggerFactory.getLogger(HttpMessageNotReadableAdvice.class);

    @ResponseBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {

            if (cause instanceof JsonMappingException jsonMappingException) {

                String fieldPath =
                        jsonMappingException
                                .getPath()
                                .stream()
                                .map(reference -> {
                                    if (reference.getFieldName() != null) {
                                        return reference.getFieldName();
                                    }

                                    return "[" + reference.getIndex() + "]";
                                })
                                .collect(Collectors.joining("."));

                String message =
                        jsonMappingException.getOriginalMessage();

                log.error(
                        "JSON-Fehler beim Deserialisieren. Feld: {}, Ursache: {}",
                        fieldPath,
                        message
                );

                return "Ungültiges JSON. Feld: "
                        + fieldPath
                        + " - "
                        + message;
            }

            cause = cause.getCause();
        }

        log.error(
                "JSON konnte nicht gelesen werden",
                exception
        );

        return exception.getMessage();
    }
}