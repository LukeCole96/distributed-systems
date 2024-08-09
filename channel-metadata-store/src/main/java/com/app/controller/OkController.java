package com.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OkController {

    @GetMapping(value = "/ok")
    public ResponseEntity<?> ok() {
        return ResponseEntity.ok().build();
    }
}