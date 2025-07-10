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
        CAPPluginMethod(name: "requestPermissionAlways", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startUpdatingLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopUpdatingLocation", returnType: CAPPluginReturnPromise)
    ]

    private var locationManager: CLLocationManager?
    private var permissionCall: CAPPluginCall?
    private var lastUpdateTime: Date?

    private struct LocationConfig {
        var accuracy: String = "high"
        var distanceFilter: Double = 0
        var updateInterval: Double = 0
        var batteryMode: String = "default"
    }

    private var config = LocationConfig()
    private var isHighFrequencyRunning = false

    public override func load() {
        locationManager = CLLocationManager()
        locationManager?.delegate = self

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidEnterBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidBecomeActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    // MARK: - App State Events
    @objc private func appDidEnterBackground() {
        print("App moved to background")
        if isHighFrequencyRunning {
            locationManager?.stopUpdatingLocation()
            print("stoping frequent location")
            locationManager?.startMonitoringSignificantLocationChanges()
            print("starting low power location request.")
        }
    }

    @objc private func appDidBecomeActive() {
        print("App became active (foreground)")
        if isHighFrequencyRunning {
            locationManager?.stopMonitoringSignificantLocationChanges()
            print("stopping low power location request")
            startLocationUpdates()
        }
    }

    // MARK: - Configuration
    @objc func initialize(_ call: CAPPluginCall) {
        applyConfigFrom(call)
        call.resolve(["message": "Configuration initialized"])
    }

    @objc func updateConfig(_ call: CAPPluginCall) {
        applyConfigFrom(call)
        call.resolve(["message": "Configuration updated"])
    }

    private func applyConfigFrom(_ call: CAPPluginCall) {
        config = LocationConfig(
            accuracy: call.getString("accuracy") ?? config.accuracy,
            distanceFilter: call.getDouble("distanceFilter") ?? config.distanceFilter,
            updateInterval: call.getDouble("updateInterval") ?? config.updateInterval,
            batteryMode: call.getString("batteryMode") ?? config.batteryMode
        )
    }

    // MARK: - Permissions
    @objc func requestPermission(_ call: CAPPluginCall) {
        guard let locationManager = locationManager else {
            call.reject("Location manager not initialized")
            return
        }

        permissionCall = call

        switch CLLocationManager.authorizationStatus() {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse:
            locationManager.requestAlwaysAuthorization()
        case .authorizedAlways:
            call.resolve(["granted": true])
            permissionCall = nil
        default:
            call.resolve(["granted": false])
            permissionCall = nil
        }
    }

    @objc func requestPermissionAlways(_ call: CAPPluginCall) {
        guard let locationManager = locationManager else {
            call.reject("Location manager not initialized")
            return
        }

        permissionCall = call
        locationManager.requestAlwaysAuthorization()
    }

    // MARK: - Location Start/Stop Methods
    @objc func startUpdatingLocation(_ call: CAPPluginCall) {
        guard locationManager != nil else {
            call.reject("Location manager not initialized")
            return
        }

        isHighFrequencyRunning = true
        locationManager?.stopMonitoringSignificantLocationChanges()
        startLocationUpdates()

        call.resolve(["status": "started"])
    }

    private func startLocationUpdates() {
        guard let locationManager = locationManager else { return }

        locationManager.desiredAccuracy = (config.accuracy == "high")
            ? kCLLocationAccuracyBest
            : kCLLocationAccuracyHundredMeters

        locationManager.distanceFilter = config.distanceFilter
        locationManager.allowsBackgroundLocationUpdates = true

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

        locationManager.startUpdatingLocation()
        print("starting high precesion location request.")
    }

    @objc func stopUpdatingLocation(_ call: CAPPluginCall) {
        locationManager?.stopUpdatingLocation()
        locationManager?.stopMonitoringSignificantLocationChanges()
        isHighFrequencyRunning = false
        call.resolve(["status": "stopped"])
    }

    // MARK: - CLLocationManagerDelegate
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }

        let now = Date()
        if let last = lastUpdateTime, config.updateInterval > 0 {
            if now.timeIntervalSince(last) < config.updateInterval {
                return
            }
        }

        lastUpdateTime = now

        notifyListeners("locationUpdate", data: [
            "lat": location.coordinate.latitude,
            "lng": location.coordinate.longitude,
            "altitude": location.altitude,
            "accuracy": location.horizontalAccuracy,
            "speed": location.speed,
            "bearing": location.course,
            "time": Int(now.timeIntervalSince1970 * 1000)
        ])
    }

    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        notifyListeners("locationError", data: [
            "error": error.localizedDescription
        ])
    }

    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        guard let call = permissionCall else { return }

        switch manager.authorizationStatus {
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
