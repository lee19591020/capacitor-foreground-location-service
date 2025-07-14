import Foundation
import Capacitor
import CoreLocation

@objc(CapacitorForegroundLocationServicePlugin)
public class CapacitorForegroundLocationServicePlugin: CAPPlugin, CAPBridgedPlugin, CLLocationManagerDelegate {

    public let identifier = "CapacitorForegroundLocationServicePlugin"
    public let jsName = "CapacitorForegroundLocationService"

    private var appInBackground = false

    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setApiOptions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "updateConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissionAlways", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startUpdatingLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopUpdatingLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "appIsInBackground", returnType: CAPPluginReturnPromise),
    ]

    private var locationManager: CLLocationManager?
    private var permissionCall: CAPPluginCall?
    private var lastUpdateTime: Date?
    private var backgroundTask: UIBackgroundTaskIdentifier = .invalid

    private struct LocationConfig {
        var accuracy: String = "high"
        var distanceFilter: Double = 5
        var updateInterval: Double = 0
        var batteryMode: String = "navigation"
    }

    private struct LocationPayload {
        let lat: Double
        let lng: Double
        let altitude: Double
        let accuracy: Double
        let speed: Double
        let bearing: Double
        let time: Int
    }

    struct GeofenceInformation: Codable {
        let lat: Double
        let lng: Double
        let radius: Double
        let clockDescription: String
        let clockNumber: Int
        let locationCode: String
        let locationDescription: String
    }

    struct GeofenceInformationDistance {
        let distance: Double
        let geofence: GeofenceInformation
    }

    struct User {
        let userId: Int
        let username: String?
        let token: String?
    }

    struct LogsPayload: Codable {
        let empId: String
        let lat: String
        let lng: String
        let description: String
        let timestamp: Int
    }

    struct AutoClockingPayload: Codable {
        let empId: String
        let lat: Double
        let lng: Double
        let isInside: Bool
        let geofence: GeofenceInformation
        let timeStamp: Int

        var isClockIn: Bool { isInside }

        func clockType() -> String {
            return isInside ? "in" : "out"
        }

        var dateTime: String {
            let date = Date(timeIntervalSince1970: TimeInterval(timeStamp) / 1000)
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            return formatter.string(from: date)
        }
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
        self.appInBackground = true
        if isHighFrequencyRunning {
            locationManager?.allowsBackgroundLocationUpdates = true
            locationManager?.startUpdatingLocation()
            print("Continued background high-accuracy updates.")

            backgroundTask = UIApplication.shared.beginBackgroundTask(withName: "LocationTracking") {
                UIApplication.shared.endBackgroundTask(self.backgroundTask)
                self.backgroundTask = .invalid
            }
        }
    }

    @objc private func appDidBecomeActive() {
        print("App became active (foreground)")
        self.appInBackground = true
        if isHighFrequencyRunning {
          locationManager?.startUpdatingLocation()
            print("Resumed high-accuracy updates.")
        }

        if backgroundTask != .invalid {
            UIApplication.shared.endBackgroundTask(backgroundTask)
            backgroundTask = .invalid
        }
    }

    // MARK: - Configuration
    @objc func initialize(_ call: CAPPluginCall) {
        applyConfigFrom(call)
        call.resolve(["message": "Configuration initialized"])
    }
  struct Endpoint: Codable {
      let endPoint: String
  }

  struct Geofence: Codable {
      let clockDescription: String
      let clockNumber: Int
      let lat: Double
      let lng: Double
      let locationCode: String
      let locationDescription: String
      let radius: Int
  }

  struct GeofenceData: Codable {
      let geofenceData: [Geofence]
  }

  struct UserData: Codable {
      let _token: String
      let userId: Int
      let username: String
  }

  struct LogsEndpoint: Codable {
      let logsEndpoint: String
  }

  struct NotificationEnabled: Codable {
      let allowNotification: Bool
  }
  @objc public func setApiOptions(_ call: CAPPluginCall) {
      // 1) Pull raw JS objects out of the call
      guard
          let endpointObj           = call.getObject("endpoint"),
          let geofenceDataObj       = call.getObject("geofenceData"),
          let userDataObj           = call.getObject("userData"),
          let logsEndpointObj       = call.getObject("logsEndpoint"),
          let allowNotificationObj  = call.getObject("allowNotification")
      else {
          call.reject("If passing apiOptions, all fields (endpoint, geofenceData, userData, logsEndpoint, allowNotification) must be provided.")
          return
      }

      do {
          let decoder = JSONDecoder()

          // 2) Turn each dictionary back into Data so we can decode into our structs
          let endpointData          = try JSONSerialization.data(withJSONObject: endpointObj)

          let logsEndpointData      = try JSONSerialization.data(withJSONObject: logsEndpointObj)
          let allowNotificationData = try JSONSerialization.data(withJSONObject: allowNotificationObj)

          // 3) Decode to validate shape
          let endpoint       = try decoder.decode(Endpoint.self, from: endpointData)
          let logsEndpoint   = try decoder.decode(LogsEndpoint.self, from: logsEndpointData)
          let notification   = try decoder.decode(NotificationEnabled.self, from: allowNotificationData)
        
          let geoJSONData = try JSONSerialization.data(withJSONObject: geofenceDataObj)
            guard let geoJSONString = String(data: geoJSONData, encoding: .utf8) else {
                call.reject("Failed to convert geofenceData to JSON string")
                return
            }
          let userJSONData = try JSONSerialization.data(withJSONObject: userDataObj)
            guard let userJSONString = String(data: userJSONData, encoding: .utf8) else {
                call.reject("Failed to convert geofenceData to JSON string")
                return
            }

          // 4) Persist into UserDefaults
          let prefs = UserDefaults.standard
          prefs.set(endpoint.endPoint,          forKey: "endpoint")
          prefs.set(geoJSONString,      forKey: "geofenceData")
          prefs.set(userJSONString,          forKey: "userData")
          prefs.set(logsEndpoint.logsEndpoint,      forKey: "logsEndpoint")
          prefs.set(notification.allowNotification, forKey: "allowNotification")

          call.resolve([
              "result": "saved successfully"
          ])

      } catch {
          call.reject("Error in setApiOptions: \(error.localizedDescription)")
      }
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

    @objc func startUpdatingLocation(_ call: CAPPluginCall) {
        guard let locationManager = locationManager else {
            call.reject("Location manager not initialized")
            return
        }

        isHighFrequencyRunning = true

        // Apply desired accuracy
        switch config.accuracy.lowercased() {
        case "low":
            locationManager.desiredAccuracy = kCLLocationAccuracyKilometer
        case "medium":
            locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        case "high":
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
        default:
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
        }

        // Apply activity type (batteryMode)
        switch config.batteryMode.lowercased() {
        case "fitness":
            locationManager.activityType = .fitness
        case "other":
            locationManager.activityType = .other
        case "automotive":
            locationManager.activityType = .automotiveNavigation
        case "navigation":
            locationManager.activityType = .automotiveNavigation
        default:
            locationManager.activityType = .automotiveNavigation
        }

        locationManager.distanceFilter = config.distanceFilter
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.showsBackgroundLocationIndicator = true
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.startUpdatingLocation()

        call.resolve(["status": "started"])
    }
    
    @objc func stopUpdatingLocation(_ call: CAPPluginCall) {
        locationManager?.stopUpdatingLocation()
        locationManager?.stopMonitoringSignificantLocationChanges()
        isHighFrequencyRunning = false

        if backgroundTask != .invalid {
            UIApplication.shared.endBackgroundTask(backgroundTask)
            backgroundTask = .invalid
        }
        self.clearClockHistory()
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

        if(self.appInBackground){
            let payload = LocationPayload(
                lat: location.coordinate.latitude,
                lng: location.coordinate.longitude,
                altitude: location.altitude,
                accuracy: location.horizontalAccuracy,
                speed: location.speed,
                bearing: location.course,
                time: Int(now.timeIntervalSince1970 * 1000)
            )
          self.sendUpdatesToServer(locationPayload: payload)
        } else {
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

    // MARK: - App Status Helpers
    @objc func appIsInBackground(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            let isBackground = UIApplication.shared.applicationState == .background
            call.resolve(["isBackground": isBackground])
        }
    }

    // MARK: - Local Notifications
    private func showLocalNotification(title: String, message: String) {

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = message
        content.sound = UNNotificationSound.default

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )

        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Failed to schedule notification: \(error.localizedDescription)")
            }
        }
    }

  private func sendUpdatesToServer(locationPayload: LocationPayload) {
        let prefs = UserDefaults.standard

        guard
          let endpointStr      = prefs.string(forKey: "endpoint"),        // lowercase “endpoint”
          let geofenceDataStr  = prefs.string(forKey: "geofenceData"),
          let userDataStr      = prefs.string(forKey: "userData"),
          let logsEndpointStr  = prefs.string(forKey: "logsEndpoint")
        else {
          print("Missing stored values.")
          return
        }

        do {
            guard
                let geofenceDataJSON = try JSONSerialization.jsonObject(with: Data(geofenceDataStr.utf8)) as? [String: Any],
                let geofenceArray = geofenceDataJSON["geofenceData"] as? [[String: Any]],
                let userJSON = try JSONSerialization.jsonObject(with: Data(userDataStr.utf8)) as? [String: Any]
            else {
                print("Failed to parse stored JSON")
                return
            }

            var geofenceList: [GeofenceInformation] = []

            for obj in geofenceArray {
                let geo = GeofenceInformation(
                    lat: obj["lat"] as? Double ?? 0.0,
                    lng: obj["lng"] as? Double ?? 0.0,
                    radius: obj["radius"] as? Double ?? 0.0,
                    clockDescription: obj["clockDescription"] as? String ?? "",
                    clockNumber: obj["clockNumber"] as? Int ?? 0,
                    locationCode: obj["locationCode"] as? String ?? "",
                    locationDescription: obj["locationDescription"] as? String ?? ""
                )
                geofenceList.append(geo)
            }

            let user = User(
                userId: userJSON["userId"] as? Int ?? 0,
                username: userJSON["username"] as? String,
                token: userJSON["_token"] as? String
            )

          self.calculateAndSend(geofenceList: geofenceList, user: user, logsEndpoint: logsEndpointStr, locationPayload: locationPayload, endPoint: endpointStr)

        } catch {
            print("Error parsing stored values or building objects: \(error.localizedDescription)")
        }
    }

    private func calculateAndSend(
        geofenceList: [GeofenceInformation],
        user: User,
        logsEndpoint: String,
        locationPayload: LocationPayload,
        endPoint: String
    ) {

        var closest: GeofenceInformationDistance?

        for geo in geofenceList {
            let distance = GeoUtils.calculateDistance(
              lat1: locationPayload.lat,
              lon1: locationPayload.lng,
                lat2: geo.lat,
                lon2: geo.lng
            )
            let item = GeofenceInformationDistance(distance: distance, geofence: geo)

            if distance < geo.radius {
                // Inside geofence
                let logPayload = LogsPayload(
                    empId: "\(user.userId)",
                    lat: String(format: "%.5f", locationPayload.lat),
                    lng: String(format: "%.5f", locationPayload.lng),
                    description: "Employee just entered the geofence \(geo.clockDescription)",
                    timestamp: Int(Date().timeIntervalSince1970 * 1000)
                )
                sendLog(token: user.token ?? "", url: logsEndpoint, payload: logPayload)

                let autoClockPayload = AutoClockingPayload(
                    empId: "\(user.userId)",
                    lat: locationPayload.lat,
                    lng: locationPayload.lng,
                    isInside: true,
                    geofence: geo,
                    timeStamp: Int(Date().timeIntervalSince1970 * 1000)
                )
                sendAutoClocking(token: user.token ?? "", url: endPoint, payload: autoClockPayload)
                return
            }

            if closest == nil || distance < closest!.distance {
                closest = item
            }
        }

        if let closest = closest {
            let distanceToClock = closest.distance - closest.geofence.radius

            let logPayload = LogsPayload(
                empId: "\(user.userId)",
                lat: String(format: "%.5f", locationPayload.lat),
                lng: String(format: "%.5f", locationPayload.lng),
                description: "Employee is \(distanceToClock) closer to \(closest.geofence.clockDescription)",
                timestamp: Int(Date().timeIntervalSince1970 * 1000)
            )
            sendLog(token: user.token ?? "", url: logsEndpoint, payload: logPayload)

            let autoClockPayload = AutoClockingPayload(
                empId: "\(user.userId)",
                lat: locationPayload.lat,
                lng: locationPayload.lng,
                isInside: false,
                geofence: closest.geofence,
                timeStamp: Int(Date().timeIntervalSince1970 * 1000)
            )
            sendAutoClocking(token: user.token ?? "", url: endPoint, payload: autoClockPayload)
        }
    }


    private func sendLog(token: String, url: String, payload: LogsPayload) {
        guard let endpoint = URL(string: url) else {
            print("sendLog: Invalid URL")
            return
        }

        do {
            let jsonData = try JSONEncoder().encode(payload)

            var request = URLRequest(url: endpoint)
            request.httpMethod = "POST"
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = jsonData

            let task = URLSession.shared.dataTask(with: request) { data, response, error in
                if let error = error {
                    print("sendLog: Request failed - \(error.localizedDescription)")
                    return
                }

                if let httpResponse = response as? HTTPURLResponse {
                    if (200...299).contains(httpResponse.statusCode) {
                        print("sendLog: Log sent successfully")
                    } else {
                        print("sendLog: Unexpected response - Code \(httpResponse.statusCode)")
                    }
                }
            }

            task.resume()

        } catch {
            print("sendLog: JSON encoding error - \(error.localizedDescription)")
        }
    }
    private func sendAutoClocking(token: String, url: String, payload: AutoClockingPayload) {
        retrySendingAutoClocking(token: token)

        let clockNumber = payload.geofence.clockNumber
        let timestamp = payload.timeStamp

        print("Has clockin: \(hasClockedIn(clockNumber: clockNumber, timeStamp: timestamp))")
        print("Has clockout: \(hasClockedOut(clockNumber: clockNumber, timeStamp: timestamp))")

        if hasClockedIn(clockNumber: clockNumber, timeStamp: timestamp) &&
            hasClockedOut(clockNumber: clockNumber, timeStamp: timestamp) {
            print("Already clocked \(payload.isClockIn ? "in" : "out") today for \(payload.geofence.clockDescription)")
            return
        }

        guard let endpoint = URL(string: url) else {
            print("Invalid endpoint URL")
            return
        }

        do {
            let jsonData = try JSONEncoder().encode(payload)

            var request = URLRequest(url: endpoint)
            request.httpMethod = "POST"
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = jsonData
            request.timeoutInterval = 5 // seconds

            print("Requesting Clocking \(payload.isClockIn ? "IN" : "OUT")")

            let task = URLSession.shared.dataTask(with: request) { data, response, error in
                if let error = error {
                    print("Clocking \(payload.isClockIn ? "IN" : "OUT") failed - saving offline: \(error.localizedDescription)")
                    self.logFailedPostRequest(url: url, payload: payload, errorMessage: "Something went wrong: \(error.localizedDescription)")
                    return
                }

                guard
                    let data = data,
                    let httpResponse = response as? HTTPURLResponse,
                    (200...299).contains(httpResponse.statusCode)
                else {
                    print("Clocking response was unsuccessful")
                    return
                }

                do {
                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                        let status = json["status"] as? Bool ?? false
                        let message = json["message"] as? String ?? "No message received"

                        if status {
                          self.saveClockHistory(clockNumber: clockNumber, type: payload.clockType(), timeStamp: TimeInterval(timestamp))
                            let autoClockNotification = "\(message) Time: \(payload.dateTime)"
                            self.showLocalNotification(title: "MyWorkplace", message: autoClockNotification)
                        }
                    }
                } catch {
                    print("Response JSON parsing error: \(error.localizedDescription)")
                }
            }

            task.resume()

        } catch {
            print("JSON encoding error: \(error.localizedDescription)")
        }
    }
    private func logFailedPostRequest(url: String, payload: AutoClockingPayload, errorMessage: String) {
        let clockType = payload.clockType()
        let clockNumber = payload.geofence.clockNumber
        let timeStamp = payload.timeStamp

        let alreadyClockedIn = hasClockedIn(clockNumber: clockNumber, timeStamp: timeStamp)
        let alreadyClockedOut = hasClockedOut(clockNumber: clockNumber, timeStamp: timeStamp)

        if alreadyClockedIn && alreadyClockedOut {
            print("Already done clocking in and out today")
            return
        }

        if (clockType == "in" && !alreadyClockedIn) ||
            (clockType == "out" && alreadyClockedIn) {
            
            let actionText = clockType == "in" ? "Clocking IN" : "Clocking OUT"
            let notificationMessage = "\(actionText) initiated to Clock: \(payload.geofence.clockDescription) Time: \(payload.dateTime)"
          self.showLocalNotification(title: "MyWorkplace is offline", message: notificationMessage)
          saveClockHistory(clockNumber: clockNumber, type: clockType, timeStamp: TimeInterval(timeStamp))

            var logData: [String: Any] = [
                "failedEndpoint": url,
                "error": errorMessage
            ]
            
            do {
                let payloadJSON = try JSONEncoder().encode(payload)
                if let json = try JSONSerialization.jsonObject(with: payloadJSON) as? [String: Any] {
                    logData["payload"] = json
                }
            } catch {
                print("Error encoding payload for log: \(error.localizedDescription)")
                return
            }

            var logsArray = UserDefaults.standard.array(forKey: "failedLogs") as? [[String: Any]] ?? []
            logsArray.append(logData)
            UserDefaults.standard.set(logsArray, forKey: "failedLogs")
        } else {
            print("No clocking needed for current state.")
        }
    }
    private func retrySendingAutoClocking(token: String) {
        let logs = UserDefaults.standard.array(forKey: "failedLogs") as? [[String: Any]] ?? []
        print("EXISTING: \(logs)")

        if logs.isEmpty { return }

        var remainingLogs: [[String: Any]] = []
        let dispatchGroup = DispatchGroup()

        for log in logs {
            guard
                let url = log["failedEndpoint"] as? String,
                let payloadDict = log["payload"] as? [String: Any],
                let payloadData = try? JSONSerialization.data(withJSONObject: payloadDict),
                let payload = try? JSONDecoder().decode(AutoClockingPayload.self, from: payloadData)
            else {
                continue
            }

            dispatchGroup.enter()

            var request = URLRequest(url: URL(string: url)!)
            request.httpMethod = "POST"
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try? JSONEncoder().encode(payload)
            request.timeoutInterval = 5

            let task = URLSession.shared.dataTask(with: request) { data, response, error in
                defer { dispatchGroup.leave() }

                if let error = error {
                    print("Retry failed: \(error.localizedDescription)")
                    remainingLogs.append(log)
                    return
                }

                guard let data = data,
                    let response = response as? HTTPURLResponse,
                    (200...299).contains(response.statusCode) else {
                    remainingLogs.append(log)
                    return
                }

                do {
                    let jsonResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any]
                    print("Retry Success Response: \(jsonResponse ?? [:])")
                } catch {
                    print("Response JSON parsing error: \(error.localizedDescription)")
                }
            }

            task.resume()
        }

        dispatchGroup.notify(queue: .main) {
            if remainingLogs.isEmpty {
                UserDefaults.standard.removeObject(forKey: "failedLogs")
                print("All logs sent successfully. Clearing failedLogs.")
              self.showLocalNotification(title: "MyWorkplace Syncing", message: "Offline clocking has been synced successfully.")
            } else {
                UserDefaults.standard.set(remainingLogs, forKey: "failedLogs")
            }
        }
    }
    private func timeStampToLocalDate(_ timeStamp: Int) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timeStamp) / 1000)
        let formatter = DateFormatter()
        formatter.locale = Locale.current
        formatter.dateFormat = "MMMM dd, yyyy HH:mm:ss"
        return formatter.string(from: date)
    }

    private func hasClockedIn(clockNumber: Int, timeStamp: Int) -> Bool {
        let prefs = UserDefaults.standard
        let logsJson = prefs.string(forKey: "clockHistory") ?? "{}"

        do {
            guard let data = logsJson.data(using: .utf8),
                let history = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return false
            }

            let dateKey = formatDate(timeStamp)
            guard let dayEntry = history[dateKey] as? [String: Any],
                let geofenceEntry = dayEntry["\(clockNumber)"] as? [String: Any] else {
                return false
            }

            return geofenceEntry["in"] as? Bool ?? false
        } catch {
            print("ClockHistory error reading: \(error.localizedDescription)")
            return false
        }
    }

    private func hasClockedOut(clockNumber: Int, timeStamp: Int) -> Bool {
        let prefs = UserDefaults.standard
        let logsJson = prefs.string(forKey: "clockHistory") ?? "{}"

        do {
            guard let data = logsJson.data(using: .utf8),
                let history = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return false
            }

            let dateKey = formatDate(timeStamp)
            guard let dayEntry = history[dateKey] as? [String: Any],
                let geofenceEntry = dayEntry["\(clockNumber)"] as? [String: Any] else {
                return false
            }

            return geofenceEntry["out"] as? Bool ?? false
        } catch {
            print("ClockHistory error reading: \(error.localizedDescription)")
            return false
        }
    }

    private func saveClockHistory(clockNumber: Int, type: String, timeStamp: TimeInterval) {
        let prefs = UserDefaults.standard
        let date = Date(timeIntervalSince1970: timeStamp / 1000) // Convert ms to seconds
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let dateKey = formatter.string(from: date)

        // Load existing clock history
        var newHistory: [String: Any] = [:]
        var dayEntry: [String: Any] = [:]

        if let logsJson = prefs.string(forKey: "clockHistory"),
        let data = logsJson.data(using: .utf8),
        let existingHistory = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
        let existingDayEntry = existingHistory[dateKey] as? [String: Any] {
            dayEntry = existingDayEntry
        }

        // Update or create geofence entry
        let clockKey = String(clockNumber)
        var geofenceEntry: [String: Any] = dayEntry[clockKey] as? [String: Any] ?? [:]

        if type == "in" {
            geofenceEntry["in"] = true
        }
        if type == "out" {
            geofenceEntry["out"] = true
        }

        // Put updated geofence entry into day entry
        dayEntry[clockKey] = geofenceEntry

        // Only keep today’s entry
        newHistory[dateKey] = dayEntry

        // Save back to UserDefaults
        if let jsonData = try? JSONSerialization.data(withJSONObject: newHistory, options: []),
        let jsonString = String(data: jsonData, encoding: .utf8) {
            prefs.set(jsonString, forKey: "clockHistory")
            print("ClockHistory: Saved clock history: \(jsonString)")
        } else {
            print("ClockHistory: Failed to serialize history")
        }
    }
    private func clearClockHistory() {
        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: "clockHistory")
        print("ClockHistory: cleared clock history")
    }

    private func formatDate(_ timeStamp: Int) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timeStamp) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale.current
        return formatter.string(from: date)
    }
}// end class
