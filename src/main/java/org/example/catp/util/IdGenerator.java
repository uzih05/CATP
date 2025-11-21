package org.example.catp.util;

import java.security.SecureRandom;

public class IdGenerator {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ID_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    // 8자리 랜덤 ID 생성 (예: "k3m9x2a7")
    public static String generate() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}