package com.example.demo.auth.application;

import com.example.demo.auth.api.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

//  Authorization: Bearer <token> 을 읽어서 request attribute에 userId/loginId 저장
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper om = new ObjectMapper();

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        //  인증/프리플라이트는 필터 적용 제외
        return path.startsWith("/api/auth") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring("Bearer ".length()).trim();
            try {
                Map<String, Object> claims = jwtTokenProvider.validateAndGetClaims(token);

                // sub에 userId가 들어있음(문자열/숫자 모두 대응)
                Object sub = claims.get("sub");
                Long userId = Long.parseLong(String.valueOf(sub));
                String loginId = String.valueOf(claims.get("loginId"));

                request.setAttribute(AuthRequestAttr.USER_ID, userId);
                request.setAttribute(AuthRequestAttr.LOGIN_ID, loginId);

            } catch (Exception e) {
                // 토큰이 있는데 invalid이면 401로 종료
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                // [수정] Map 대신 ApiErrorResponse로 통일
                om.writeValue(response.getWriter(),
                        new ApiErrorResponse("UNAUTHORIZED", "유효하지 않은 토큰입니다.")); // [수정]
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
