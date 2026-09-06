package tn.esprit.smartforma.auth.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;

final class SecurityErrorWriter {

    private SecurityErrorWriter() {}

    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String safeMessage = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{\"status\":" + status
                + ",\"message\":\"" + safeMessage + "\""
                + ",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
        response.getWriter().write(json);
    }
}
