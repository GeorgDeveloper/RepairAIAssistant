package ru.georgdeveloper.assistantweb.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ограничивает доступ к странице импорта ОГЭ и API импорта:
 * только после успешного ввода пароля (сессия с атрибутом energy_import_authenticated).
 */
@Order(1)
public class EnergyImportAuthFilter extends OncePerRequestFilter {

    private static final String SESSION_ATTR = "energy_import_authenticated";
    private static final String LOGIN_PATH = "/energy/import/login";
    private static final String PAGE_PATH = "/energy/import";
    private static final String API_PATH = "/api/energy/import";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!isProtectedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (path.equals(LOGIN_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }
        HttpSession session = request.getSession(false);
        boolean authenticated = session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_ATTR));
        if (!authenticated) {
            if (path.equals(API_PATH)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\"}");
            } else {
                response.sendRedirect(request.getContextPath() + LOGIN_PATH);
            }
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isProtectedPath(String path) {
        return path.equals(PAGE_PATH) || path.equals(API_PATH) || path.equals(LOGIN_PATH);
    }
}
