package com.example.demo.auth.api;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class ApiErrorResponse {
    private String code;
    private String message;
}
