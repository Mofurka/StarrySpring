package irden.space.proxy.plugin.irden.test;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DiscordApplicationTokenService {

    private static final int VERSION = 1;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final Pattern TAG_PATTERN = Pattern.compile("\\|\\|-# <tag:v1:(.*?)>\\|\\|");
    private final SecretKey discordApplicationTokenKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public static String wrapToken(String token) {
        return "||-# <tag:v1:" + token + ">||";
    }

    public static Optional<String> extractToken(String wrappedToken) {
        var matcher = TAG_PATTERN.matcher(wrappedToken);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public String encrypt(long applicationId, long messageId) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    discordApplicationTokenKey,
                    new GCMParameterSpec(128, iv)
            );

            cipher.updateAAD(Long.toString(messageId)
                    .getBytes(StandardCharsets.UTF_8));

            byte[] encrypted = cipher.doFinal(
                    Long.toString(applicationId)
                            .getBytes(StandardCharsets.UTF_8)
            );

            ByteBuffer buffer = ByteBuffer.allocate(
                    1 + iv.length + encrypted.length
            );

            buffer.put((byte) VERSION);
            buffer.put(iv);
            buffer.put(encrypted);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(buffer.array());

        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt application id", e);
        }
    }

    public long decrypt(String token, long messageId) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(token);

            ByteBuffer buffer = ByteBuffer.wrap(payload);

            int version = Byte.toUnsignedInt(buffer.get());
            if (version != VERSION) {
                throw new IllegalArgumentException(
                        "Unsupported token version: " + version
                );
            }

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    discordApplicationTokenKey,
                    new GCMParameterSpec(TAG_LENGTH, iv)
            );

            cipher.updateAAD(Long.toString(messageId)
                    .getBytes(StandardCharsets.UTF_8));

            byte[] decrypted = cipher.doFinal(encrypted);

            return Long.parseLong(
                    new String(decrypted, StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid application token", e);
        }
    }

}