package br.edu.ifro.ji.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> problema = new LinkedHashMap<>();
        problema.put("type", "https://ji.local/erros/sessao-invalida");
        problema.put("title", "Autenticação necessária");
        problema.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        problema.put("detail", "A sessão é inválida ou expirou. Faça login novamente.");
        problema.put("instance", request.getRequestURI());
        problema.put("codigo", "sessao-invalida");
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
