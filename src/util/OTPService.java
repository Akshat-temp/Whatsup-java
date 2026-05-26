package util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OTPService — generates a 6-digit OTP, sends it via SendGrid, and verifies it.
 *
 * Setup (one-time):
 *   1. Create a free account at https://sendgrid.com
 *   2. Go to Settings → API Keys → Create API Key (Mail Send permission)
 *   3. Verify a sender email in Settings → Sender Authentication
 *   4. Set the two env vars below (never hard-code keys in source)
 *
 * Environment variables:
 *   SENDGRID_API_KEY  — your SendGrid API key (starts with SG.)
 *   SENDGRID_FROM     — verified sender email, e.g. whatsup@yourdomain.com
 *
 * During local dev without these vars, the OTP is printed to the console.
 */
public class OTPService {

    private static final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    // ── Public API ─────────────────────────────────────────────────────────

    /** Generate OTP, store it, send via SendGrid. Returns true if email was sent. */
    public static boolean generateAndSendOTP(String phone, String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStore.put(phone, otp);

        String apiKey  = System.getenv("SENDGRID_API_KEY");
        String fromEmail = System.getenv("SENDGRID_FROM");

        if (apiKey == null || apiKey.isBlank()) {
            // Dev fallback — no SendGrid configured
            System.out.println("╔══════════════════════════════╗");
            System.out.println("║  [DEV] OTP for " + phone + ": " + otp + "  ║");
            System.out.println("╚══════════════════════════════╝");
            return true;  // pretend it was "sent" so UI flow continues
        }

        return sendViaSendGrid(apiKey, fromEmail, email, phone, otp);
    }

    /** Verify an OTP entered by the user. */
    public static boolean verifyOTP(String phone, String enteredOtp) {
        String stored = otpStore.get(phone);
        return stored != null && stored.equals(enteredOtp.trim());
    }

    /** Call after successful registration to remove the OTP. */
    public static void clearOTP(String phone) {
        otpStore.remove(phone);
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private static boolean sendViaSendGrid(String apiKey, String from,
                                           String to, String phone, String otp) {
        try {
            String body = """
                {
                  "personalizations": [{"to": [{"email": "%s"}]}],
                  "from": {"email": "%s", "name": "Whatsup App"},
                  "subject": "Your Whatsup OTP Code",
                  "content": [{
                    "type": "text/html",
                    "value": "<h2 style='color:#25D366'>Whatsup</h2><p>Your OTP is:</p><h1 style='letter-spacing:6px'>%s</h1><p>Valid for this session only. Do not share it.</p>"
                  }]
                }""".formatted(to, from, otp);

            URL url = new URL("https://api.sendgrid.com/v3/mail/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 202) {
                System.out.println("[OTP] Sent to " + to);
                return true;
            } else {
                System.err.println("[OTP] SendGrid returned HTTP " + code);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[OTP] Failed to send email: " + e.getMessage());
            return false;
        }
    }
}
