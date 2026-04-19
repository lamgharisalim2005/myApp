package com.example.myapp.exceptions;

import lombok.Getter;

import java.util.List;
@Getter
public class GlobalResponse<T> {
    private final static String SUCCESS = "success";
    private final static String ERROR = "error";
    private final String status;
    private final T data;
    private final List<ErrorItem> errors;

    public GlobalResponse(List<ErrorItem> errors) {
        this.errors = errors;
        this.status = ERROR;
        this.data = null;

    }

    public GlobalResponse(T data) {
        this.data = data;
        this.status = SUCCESS;
        this.errors = null;

    }

    public record ErrorItem(String message) {
    }
}
