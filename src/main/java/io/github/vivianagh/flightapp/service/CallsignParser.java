package io.github.vivianagh.flightapp.service;


import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public final class CallsignParser {
    private CallsignParser() {}

    public static Optional<String> extractPrefix(String callsign) {
        if (callsign == null) return Optional.empty();
        String t = callsign.trim().toUpperCase(Locale.ROOT);
        if (t.length() < 2) return Optional.empty();

        if (t.length() >= 3 && isLetters(t.substring(0, 3))) {
            return Optional.of(t.substring(0, 3)); // ICAO
        }
        if (isLettersOrDigits(t.substring(0, 2))) {
            return Optional.of(t.substring(0, 2)); // IATA
        }
        return Optional.empty();
    }

    private static boolean isLetters(String s) {
        for (int i = 0; i < s.length(); i++) if (!Character.isLetter(s.charAt(i))) return false;
        return true;
    }
    private static boolean isLettersOrDigits(String s) {
        for (int i = 0; i < s.length(); i++) if (!Character.isLetterOrDigit(s.charAt(i))) return false;
        return true;
    }
}
