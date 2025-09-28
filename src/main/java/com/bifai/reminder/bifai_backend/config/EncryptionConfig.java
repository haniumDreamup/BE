package com.bifai.reminder.bifai_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * 암호화 설정
 * EmergencyContact 전화번호 암호화용
 */
@Configuration
public class EncryptionConfig {

    @Value("${app.encryption.password:bifai-default-password}")
    private String encryptionPassword;

    @Value("${app.encryption.salt:deadbeef}")
    private String encryptionSalt;

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(encryptionPassword, encryptionSalt);
    }
}