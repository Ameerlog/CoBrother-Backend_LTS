package com.cobrother.web.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;

public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Writes the cookie via a raw Set-Cookie response header so that
     * SameSite=None; Secure is guaranteed — jakarta.servlet.Cookie.setAttribute()
     * is not reliably honoured by every embedded Tomcat version.
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        String header = name + "=" + value
                + "; Path=/"
                + "; Max-Age=" + maxAge
                + "; HttpOnly"
                + "; Secure"
                + "; SameSite=None";
        response.addHeader("Set-Cookie", header);
    }

    /**
     * Expires the cookie.  Must also carry SameSite=None; Secure, otherwise
     * the browser won't match and remove the original cross-site cookie.
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    String header = name + "="
                            + "; Path=/"
                            + "; Max-Age=0"
                            + "; HttpOnly"
                            + "; Secure"
                            + "; SameSite=None";
                    response.addHeader("Set-Cookie", header);
                }
            }
        }
    }

    public static String serialize(Object object) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos)) {
                oos.writeObject(object);
                return Base64.getUrlEncoder().encodeToString(bos.toByteArray());
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes))) {
                return cls.cast(ois.readObject());
            }
        } catch (Exception e) {
            return null;
        }
    }
}
