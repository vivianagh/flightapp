package io.github.vivianagh.flightapp.service;

import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.Map;

import static java.util.Map.entry;

@Service
public class AirlineLookupService {

    private static final Map<String, String> ICAO_TO_NAME = Map.ofEntries(
            entry("BAW","British Airways"), entry("VIR","Virgin Atlantic"),
            entry("EZY","easyJet"), entry("RYR","Ryanair"),
            entry("AFR","Air France"), entry("KLM","KLM"), entry("DLH","Lufthansa"),
            entry("SWR","SWISS"), entry("AUA","Austrian"), entry("IBE","Iberia"),
            entry("VLG","Vueling"), entry("AEA","Air Europa"), entry("SAS","Scandinavian Airlines"),
            entry("FIN","Finnair"), entry("LOT","LOT Polish Airlines"), entry("BEL","Brussels Airlines"),
            entry("THY","Turkish Airlines"), entry("QTR","Qatar Airways"), entry("UAE","Emirates"),
            entry("ETD","Etihad"), entry("AAL","American Airlines"), entry("UAL","United Airlines"),
            entry("DAL","Delta Air Lines"), entry("ACA","Air Canada"), entry("JBU","JetBlue"),
            entry("AMX","Aeroméxico"), entry("LAN","LATAM Airlines"), entry("AVA","Avianca"),
            entry("KAL","Korean Air"), entry("AEE","Aegean Airlines"), entry("CFE","BA Cityflyer"),
            entry("EXS","Jet2.com"), entry("ITY","ITA Airways"), entry("TOM","TUI Airways"),
            entry("VJT","VistaJet")
    );

    private static final Map<String, String> IATA_TO_NAME = Map.ofEntries(
            entry("BA","British Airways"), entry("U2","easyJet"), entry("FR","Ryanair"),
            entry("AF","Air France"), entry("KL","KLM"), entry("LH","Lufthansa"),
            entry("LX","SWISS"), entry("OS","Austrian"), entry("IB","Iberia"),
            entry("VY","Vueling"), entry("UX","Air Europa"), entry("SK","Scandinavian Airlines"),
            entry("AY","Finnair"), entry("LO","LOT Polish Airlines"), entry("SN","Brussels Airlines"),
            entry("TK","Turkish Airlines"), entry("QR","Qatar Airways"), entry("EK","Emirates"),
            entry("EY","Etihad"), entry("AA","American Airlines"), entry("UA","United Airlines"),
            entry("DL","Delta Air Lines"), entry("AC","Air Canada"), entry("B6","JetBlue"),
            entry("AM","Aeroméxico"), entry("LA","LATAM Airlines"), entry("AV","Avianca"),
            entry("A3","Aegean Airlines"), entry("CJ","BA Cityflyer"), entry("LS","Jet2.com"),
            entry("AZ","ITA Airways"), entry("BY","TUI Airways"), entry("KE","Korean Air")
    );

    public String getAirlineName(@Nullable String callsign, @Nullable String icao24) {
        if (callsign != null) {
            String letters = leadingLetters(callsign);
            if (letters.length() >= 3) {
                String name = ICAO_TO_NAME.get(letters.substring(0,3));
                if (name != null) return name;
            }
            if (letters.length() >= 2) {
                String name = IATA_TO_NAME.get(letters.substring(0,2));
                if (name != null) return name;
            }
        }
        return "Unknown";
    }

    private String leadingLetters(String cs) {
        String s = cs.trim().toUpperCase();
        int i = 0; while (i < s.length() && s.charAt(i) >= 'A' && s.charAt(i) <= 'Z') i++;
        return s.substring(0, i);
    }

    public @Nullable String lookupByCallsignPrefix(String callsign) {
        if (callsign == null) return null;
        String cs = callsign.trim().toUpperCase(Locale.ROOT);
        if (cs.isEmpty()) return null;

        // ICAO: 3 letras
        if (cs.length() >= 3
                && Character.isLetter(cs.charAt(0))
                && Character.isLetter(cs.charAt(1))
                && Character.isLetter(cs.charAt(2))) {
            String p3 = cs.substring(0, 3);
            String name = ICAO_TO_NAME.get(p3);
            if (name != null) return name;
        }

        // IATA: 2 alfanuméricos (letra o dígito)
        if (cs.length() >= 2
                && Character.isLetterOrDigit(cs.charAt(0))
                && Character.isLetterOrDigit(cs.charAt(1))) {
            String p2 = cs.substring(0, 2);
            String name = IATA_TO_NAME.get(p2);
            if (name != null) return name;
        }

        return null;
    }
}
