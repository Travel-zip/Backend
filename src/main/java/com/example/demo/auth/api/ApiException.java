package com.example.demo.auth.api;

import org.springframework.http.HttpStatus;

//API 에러를 상태코드 + code + message로 통일하기 위한 예외
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
