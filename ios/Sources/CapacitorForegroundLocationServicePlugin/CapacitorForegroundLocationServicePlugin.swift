import Foundation
import Capacitor
import CoreLocation

@objc(CapacitorForegroundLocationServicePlugin)
public class CapacitorForegroundLocationServicePlugin: CAPPlugin, CAPBridgedPlugin, CLLocationManagerDelegate {

    public let identifier = "CapacitorForegroundLocationServicePlugin"
    public let jsName = "CapacitorForegroundLocationService"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "updateConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startUpdatingLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopUpdatingLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissionAlways", returnType: CAPPluginReturnPromise),
    ]

    private var locationManager: CLLocationManager!
    private var permissionCall: CAPPluginCall?
    private var lastUpdateTime: Date?

    private struct LocationConfig {
        var accuracy: String = "high"
        var distanceFilter: Double = 0
        var updateInterval: Double = 0
        var batteryMode: String = "default"
    }

    private var config = LocationConfig()

    public override func load() {
        locationManager = CLLocationManager()
        locationManager.delegate = self

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidEnterBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
    }

    @objc private func appDidEnterBackground() {
        locationManager?.startUpdatingLocation()
    }

    @objc func initialize(_ call: CAPPluginCall) {
        applyConfigFrom(call)
        call.resolve(["message": "Configuration initialized"])
    }

    @objc func updateConfig(_ call: CAPPluginCall) {
        applyConfigFrom(call)
        call.resolve(["message": "Configuration updated"])
    }

    private func applyConfigFrom(_ call: CAPPluginCall) {
        let accuracy = call.getString("accuracy") ?? config.accuracy
        let distanceFilter = call.getDouble("distanceFilter") ?? config.distanceFilter
        let updateInterval = call.getDouble("updateInterval") ?? config.updateInterval
        let batteryMode = call.getString("batteryMode") ?? config.batteryMode

        config = LocationConfig(
            accuracy: accuracy,
            distanceFilter: distanceFilter,
            updateInterval: updateInterval,
            batteryMode: batteryMode
        )
    }

    @objc func requestPermission(_ call: CAPPluginCall) {
        guard let locationManager = self.locationManager else {
            call.reject("Location manager not initialized")
            return
        }

        permissionCall = call

        let status = CLLocationManager.authorizationStatus()
        switch status {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse:
            locationManager.requestAlwaysAuthorization()
        case .authorizedAlways:
            call.resolve(["granted": true])
            permissionCall = nil
        case .denied, .restricted:
            call.resolve(["granted": false])
            permissionCall = nil
        @unknown default:
            call.resolve(["granted": false])
            permissionCall = nil
        }
    }

    @objc func startUpdatingLocation(_ call: CAPPluginCall) {
        guard let locationManager = self.locationManager else {
            call.reject("Location manager not initialized")
            return
        }

        locationManager.desiredAccuracy = config.accuracy == "high"
            ? kCLLocationAccuracyBest
            : kCLLocationAccuracyHundredMeters

        locationManager.distanceFilter = config.distanceFilter

        switch config.batteryMode {
        case "fitness":
            locationManager.activityType = .fitness
            locationManager.pausesLocationUpdatesAutomatically = true
        case "navigation":
            locationManager.activityType = .automotiveNavigation
            locationManager.pausesLocationUpdatesAutomatically = false
        case "lowPower":
            locationManager.activityType = .otherNavigation
            locationManager.pausesLocationUpdatesAutomatically = true
        default:
            locationManager.activityType = .other
            locationManager.pausesLocationUpdatesAutomatically = false
        }

        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.startUpdatingLocation()

        call.resolve(["status": "started"])
    }

    @objc func stopUpdatingLocation(_ call: CAPPluginCall) {
        locationManager.stopUpdatingLocation()
        call.resolve(["status": "stopped"])
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }

        let now = Date()
        if let lastTime = lastUpdateTime, config.updateInterval > 0 {
            let elapsed = now.timeIntervalSince(lastTime)
            if elapsed < config.updateInterval {
                return // skip sending update
            }
        }

        lastUpdateTime = now

        notifyListeners("locationUpdate", data: [
            "lat": location.coordinate.latitude,
            "lng": location.coordinate.longitude
        ])
    }

    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        notifyListeners("locationError", data: [
            "error": error.localizedDescription
        ])
    }

    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        guard let call = permissionCall else { return }

        let status = manager.authorizationStatus
        switch status {
        case .authorizedAlways:
            call.resolve(["granted": true])
            permissionCall = nil
        case .authorizedWhenInUse:
            manager.requestAlwaysAuthorization()
        case .denied, .restricted:
            call.resolve(["granted": false])
            permissionCall = nil
        case .notDetermined:
            break
        @unknown default:
            call.resolve(["granted": false])
            permissionCall = nil
        }
    }
}
