package com.jenikmax.game.library.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Фильтр кодировки запросов.
 * Исправляет кракозябры (ISO-8859-1 → UTF-8) пришедшие от Thymeleaf-форм.
 * Пропускает /api/tracker/ без изменений (бинарные данные BitTorrent).
 */
@Component
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest
                && !httpRequest.getRequestURI().contains("/api/tracker/")) {
            chain.doFilter(new EncodingRequestWrapper(httpRequest), response);
            return;
        }
        chain.doFilter(request, response);
    }

    /** Обёртка запроса, перекодирующая параметры из ISO-8859-1 в UTF-8. */
    private static class EncodingRequestWrapper extends HttpServletRequestWrapper {

        public EncodingRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            return fixEncoding(super.getParameter(name));
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] fixed = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                fixed[i] = fixEncoding(values[i]);
            }
            return fixed;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> map = super.getParameterMap();
            Map<String, String[]> fixed = new HashMap<>();
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                String[] values = entry.getValue();
                String[] fixedValues = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    fixedValues[i] = fixEncoding(values[i]);
                }
                fixed.put(entry.getKey(), fixedValues);
            }
            return fixed;
        }

        /** Перекодировка строки из ISO-8859-1 в UTF-8. */
        private static String fixEncoding(String input) {
            if (input == null || input.isEmpty()) return input;
            return new String(input.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }
    }
}
