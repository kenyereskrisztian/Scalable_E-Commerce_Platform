package com.ecommerce.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Kinyeri az aktuális kérés Authorization fejlécét, hogy a mikroszolgáltatások
 * tovább tudják adni (forwardolni) a felhasználó JWT-jét egymásnak.
 */
public final class RequestTokenUtils {

    private RequestTokenUtils() {
    }

    /**
     * @return az aktuális kérés "Authorization" fejléce (pl. "Bearer ..."),
     *         vagy {@code null}, ha nincs (nem HTTP-kérésből hívták).
     */
    public static String getBearerToken() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header;
    }
}
