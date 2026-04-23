package com.example.myapp.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@ControllerAdvice
public class ExceptionController {

    // 404 — Resource non trouvée
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalResponse<?>> gereNoResourceFoundException(
            NoResourceFoundException exception) {
        List<GlobalResponse.ErrorItem> errors = List.of(
                new GlobalResponse.ErrorItem("Resource non trouvée")
        );
        return new ResponseEntity<>(new GlobalResponse<>(errors), HttpStatus.NOT_FOUND);
    }

    //400 bad request
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GlobalResponse<String>> gereDataIntegrityViolationException(DataIntegrityViolationException e) {
        var errors = List.of(
                new GlobalResponse.ErrorItem("Ce resource deja existe")
        );
        return new ResponseEntity<>(new GlobalResponse<>(errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GlobalResponse<?>> gereRunTimeException(
            RuntimeException exception) {
        List<GlobalResponse.ErrorItem> errors = List.of(
                new GlobalResponse.ErrorItem(exception.getMessage())
        );

        // 404 — Resource non trouvée
        if (exception.getMessage() != null &&
                exception.getMessage().contains("non trouvé")) {
            return new ResponseEntity<>(new GlobalResponse<>(errors), HttpStatus.NOT_FOUND);
        }

        // 403 — Accès refusé
        if (exception.getMessage() != null && (
                exception.getMessage().contains("appartient pas") ||
                        exception.getMessage().contains("n'êtes pas") ||
                        exception.getMessage().contains("ne vous appartient pas"))) {
            return new ResponseEntity<>(new GlobalResponse<>(errors), HttpStatus.FORBIDDEN);
        }

        // 400 — Mauvaise requête
        return new ResponseEntity<>(new GlobalResponse<>(errors), HttpStatus.BAD_REQUEST);
    }
}