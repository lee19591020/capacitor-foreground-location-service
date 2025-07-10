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
        CAPPluginMethod(name: "stopUpdatingLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "appIsInBackground", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showLocalNotification", returnType: CAPPluginReturnPromise)
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
          locationManager?.allowsBackgroundLocationUpdates = true

          // TEMP: Start updating for a short while to ensure geofence/position
          locationManager?.startUpdatingLocation()

          // Schedule a fallback to switch to significant changes after 1-2 mins
          DispatchQueue.main.asyncAfter(deadline: .now() + 120) {
              self.locationManager?.stopUpdatingLocation()
              self.locationManager?.startMonitoringSignificantLocationChanges()
              print("Switched to low-power significant changes mode.")
          }

          print("Started high-precision background updates temporarily.")
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

        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if let error = error {
                print("Notification permission error: \(error.localizedDescription)")
            } else {
                print("Notification permission granted: \(granted)")
            }

            // Step 2: Continue with location permission
            DispatchQueue.main.async {
                let status = CLLocationManager.authorizationStatus()

                switch status {
                case .notDetermined:
                    locationManager.requestWhenInUseAuthorization()
                case .authorizedWhenInUse:
                    locationManager.requestAlwaysAuthorization()
                case .authorizedAlways:
                    call.resolve(["granted": true])
                    self.permissionCall = nil
                case .denied, .restricted:
                    call.resolve(["granted": false])
                    self.permissionCall = nil
                @unknown default:
                    call.resolve(["granted": false])
                    self.permissionCall = nil
                }
            }
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

    @objc func appIsInBackground(_ call: CAPPluginCall) {
      DispatchQueue.main.async {
          let isBackground = UIApplication.shared.applicationState == .background
          call.resolve(["isBackground": isBackground])
      }
    }

    @objc func showLocalNotification(_ call: CAPPluginCall) {
        let title = call.getString("title") ?? "Notification"
        let body = call.getString("body") ?? "No notification body"

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = UNNotificationSound.default

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )

        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                call.reject("Failed to schedule notification: \(error.localizedDescription)")
            } else {
                call.resolve()
            }
        }
    }
} // end class
