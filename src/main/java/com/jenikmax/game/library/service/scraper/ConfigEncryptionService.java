package com.jenikmax.game.library.service.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ConfigEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(ConfigEncryptionService.class);
    private static final String ENV_KEY = "SCRAPER_ENCRYPTION_KEY";
    private static final String ENC_PREFIX = "ENC:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        String envKey = System.getenv(ENV_KEY);
        if (envKey != null && !envKey.isEmpty()) {
            byte[] decoded = Base64.getDecoder().decode(envKey);
            this.secretKey = new SecretKeySpec(decoded, "AES");
            log.info("Encryption key loaded from env {}", ENV_KEY);
        } else {
            try {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256);
                this.secretKey = kg.generateKey();
                log.warn("No {} env set. Generated ephemeral AES-256 key. Encrypted keys will NOT survive restart!", ENV_KEY);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate ephemeral AES key", e);
            }
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return "";
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) return "";
        if (!ciphertext.startsWith(ENC_PREFIX)) return ciphertext;
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(ENC_PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, combined, 0, GCM_IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] plaintext = cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);
            return new String(plaintext, "UTF-8");
        } catch (Exception e) {
            log.warn("Decryption failed for value, returning as-is", e);
            return ciphertext;
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENC_PREFIX);
    }

    public String mask(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.length() > 8 ? value.substring(0, 4) + "****" : "****";
    }
}
