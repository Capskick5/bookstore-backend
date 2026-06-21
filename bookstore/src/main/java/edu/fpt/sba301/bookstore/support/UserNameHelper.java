package edu.fpt.sba301.bookstore.support;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class UserNameHelper {

    private UserNameHelper() {
    }

    public static String deriveFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "BookVerse User";
        }

        String localPart = email.substring(0, email.indexOf('@')).trim();
        if (localPart.isBlank()) {
            return "BookVerse User";
        }

        String derived = Arrays.stream(localPart.split("[._\\-+]+"))
                .filter(part -> !part.isBlank())
                .map(UserNameHelper::capitalizeToken)
                .collect(Collectors.joining(" "));

        return derived.isBlank() ? "BookVerse User" : derived;
    }

    private static String capitalizeToken(String token) {
        if (token.length() == 1) {
            return token.toUpperCase();
        }
        return Character.toUpperCase(token.charAt(0)) + token.substring(1).toLowerCase();
    }
}
