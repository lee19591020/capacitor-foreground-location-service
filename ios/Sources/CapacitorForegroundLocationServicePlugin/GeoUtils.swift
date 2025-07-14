class GeoUtils {
    
    /// Calculates distance in meters between two coordinates using Haversine formula
    static func calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double) -> Double {
        let R = 6_371_000.0 // Earth's radius in meters

        let dLat = deg2rad(lat2 - lat1)
        let dLon = deg2rad(lon2 - lon1)

        let a = sin(dLat / 2) * sin(dLat / 2) +
                cos(deg2rad(lat1)) * cos(deg2rad(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        let c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private static func deg2rad(_ deg: Double) -> Double {
        return deg * .pi / 180
    }
}