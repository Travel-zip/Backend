package com.example.demo.auth.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException; // [추가]
import org.springframework.web.bind.MissingServletRequestParameterException; // [추가]
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException; // [추가]
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException; // [추가]
import org.springframework.web.servlet.resource.NoResourceFoundException; // [추가]
import jakarta.servlet.http.HttpServletRequest; // [추가]



// ApiException을 공통 포맷으로 내려줌
@RestControllerAdvice
@Slf4j // [추가]
public class GlobalApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handle(ApiException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ApiErrorResponse(e.getCode(), e.getMessage()));
    }

    // =====================================================
    // [추가] (보완 1) ResponseStatusException은 상태코드/사유를 그대로 살려서 내려주기
    // - 지금처럼 Exception.class가 다 잡아버리면 400이어야 할 게 500으로 뭉개질 수 있음
    // =====================================================
    @ExceptionHandler(ResponseStatusException.class) // [추가]
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException e) { // [추가]
        int status = e.getStatusCode().value(); // [추가]
        String code = mapStatusToCode(status); // [추가]
        String msg = (e.getReason() == null || e.getReason().isBlank())
                ? "요청이 올바르지 않습니다."
                : e.getReason(); // [추가]

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(code, msg)); // [추가]
    }

    // =====================================================
    // [추가] (보완 2) 스프링 MVC에서 자주 나는 예외들을 {code,message}로 통일
    // =====================================================

    // JSON 파싱 실패/요청 바디가 깨짐 등
    @ExceptionHandler(HttpMessageNotReadableException.class) // [추가]
    public ResponseEntity<ApiErrorResponse> handleBadJson(HttpMessageNotReadableException e) { // [추가]
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_JSON", "요청 JSON 형식이 올바르지 않습니다.")); // [추가]
    }

    // @RequestParam 타입 미스매치 (예: radius=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class) // [추가]
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) { // [추가]
        String name = e.getName();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_PARAMETER",
                        "파라미터 형식이 올바르지 않습니다: " + name)); // [추가]
    }

    // 필수 파라미터 누락 (예: lat/lng/radius 안 보냄)
    @ExceptionHandler(MissingServletRequestParameterException.class) // [추가]
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException e) { // [추가]
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("MISSING_PARAMETER",
                        "필수 파라미터가 누락되었습니다: " + e.getParameterName())); // [추가]
    }

    // HTTP Method 잘못 호출 (예: POST만 되는데 GET 호출)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class) // [추가]
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) { // [추가]
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiErrorResponse("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다.")); // [추가]
    }

    // =====================================================
    // [수정] Unknown 예외는 진짜 500만 내려가게 (로그는 남김)
    // =====================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(Exception e) {
        log.error("Unhandled exception", e); // [추가]
        return ResponseEntity.status(500)
                .body(new ApiErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }

    // [추가] 상태코드 -> code 매핑 (프론트가 status만으로도 처리 가능하지만, code도 깔끔하게)
    private String mapStatusToCode(int status) { // [추가]
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 405 -> "METHOD_NOT_ALLOWED";
            default -> "ERROR";
        };
    }

    // =====================================================
    // [추가] 없는 URL(라우팅 자체가 없음)도 404로 통일
    // - A 케이스가 INTERNAL_ERROR(500)로 떨어지는 걸 막아줌
    // =====================================================
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class}) // [추가]
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception e, HttpServletRequest req) { // [추가]
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("NOT_FOUND",
                        "요청한 API를 찾을 수 없습니다: " + req.getRequestURI())); // [추가]
    }




}
