package io.github.vivianagh.flightapp.utils;

public class LocationUtils {

    private static final double HOME_LAT = 51.4757;
    private static final double HOME_LON = 0.3252;
    private static final double RADIUS_KM = 10.0;

    public static boolean isNearHome(double lat, double lon) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat - HOME_LAT);
        double dLon = Math.toRadians(lon - HOME_LON);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(HOME_LAT)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;
        return distance <= RADIUS_KM;
    }
}
