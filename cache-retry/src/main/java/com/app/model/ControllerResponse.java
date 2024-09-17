package com.app.model;

import lombok.Data;

@Data
public class ControllerResponse {
    private String statusCode;
    private String message;

    public ControllerResponse(String statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}