package ar.edu.utn.frc.tup.piii.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class RefreshTokenCookieService {

    private final String cookieName;
    private final String cookiePath;
    private final String sameSite;
    private final boolean secure;
    private final long maxAgeSeconds;

    public RefreshTokenCookieService(
            @Value("${app.auth.refresh-cookie.name:refreshToken}") String cookieName,
            @Value("${app.auth.refresh-cookie.path:/api/auth}") String cookiePath,
            @Value("${app.auth.refresh-cookie.same-site:Lax}") String sameSite,
            @Value("${app.auth.refresh-cookie.secure:false}") boolean secure,
            @Value("${app.jwt.refresh-token-expiration-seconds:604800}") long maxAgeSeconds) {
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.sameSite = sameSite;
        this.secure = secure;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshToken, maxAgeSeconds).toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString());
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        jakarta.servlet.http.Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return null;
        }

        return cookie.getValue();
    }

    private ResponseCookie buildCookie(String value, long maxAge) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(maxAge)
                .build();
    }
}
