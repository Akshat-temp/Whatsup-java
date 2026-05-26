package util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class OTPService {

    private static final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private static final Random random = new Random();


    public static boolean generateAndSendOTP(String phone, String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStore.put(phone, otp);

        String apiKey = System.getenv("BREVO_API_KEY");
        String fromEmail = System.getenv("BREVO_FROM");

        if (apiKey == null || apiKey.isBlank()) {
            // Dev fallback — no SendGrid configured
            System.out.println("╔══════════════════════════════╗");
            System.out.println("║  [DEV] OTP for " + phone + ": " + otp + "  ║");
            System.out.println("╚══════════════════════════════╝");
            return true;  
        }

        return sendViabrevo(apiKey, fromEmail, email, phone, otp);
    }

    
    public static boolean verifyOTP(String phone, String enteredOtp) {
        String stored = otpStore.get(phone);
        return stored != null && stored.equals(enteredOtp.trim());
    }

    
    public static void clearOTP(String phone) {
        otpStore.remove(phone);
    }

    private static boolean sendViabrevo(String apiKey, String from,
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

            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 201) {
                System.out.println("[OTP] Sent to " + to);
                return true;
            } else {
                System.err.println("[OTP] brevo returned HTTP " + code);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[OTP] Failed to send email: " + e.getMessage());
            return false;
        }
    }
}
