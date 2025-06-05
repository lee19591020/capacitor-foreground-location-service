package com.fusion5.capacitorforegroundlocationservice;

public class GeoUtils {

    // Calculates distance in meters between two lat/lng points using Haversine formula
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Radius of the Earth in meters

        double dLat = deg2rad(lat2 - lat1);
        double dLon = deg2rad(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return distance;
    }

    private static double deg2rad(double deg) {
        return deg * (Math.PI / 180);
    }
}
