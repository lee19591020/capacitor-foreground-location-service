import Foundation
import Capacitor
import CoreLocation

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CapacitorForegroundLocationServicePlugin)
public class CapacitorForegroundLocationServicePlugin: CAPPlugin, CAPBridgedPlugin, CLLocationManagerDelegate {

    public let identifier = "CapacitorForegroundLocationServicePlugin"
    public let jsName = "CapacitorForegroundLocationService"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startUpdatingLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopUpdatingLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissionAlways", returnType: CAPPluginReturnPromise),
    ]
  private var locationManager: CLLocationManager!
    public override func load() {
        print("Plugin loaded")
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
      print("App entered background")
      locationManager?.startUpdatingLocation()
  }
  @objc func initialize(_ call: CAPPluginCall) {
      print("Initialized")
      call.resolve()
  }
  
  private var permissionCall: CAPPluginCall?

  @objc func requestPermission(_ call: CAPPluginCall) {
      guard let locationManager = self.locationManager else {
          call.reject("Location manager not initialized")
          return
      }

      permissionCall = call

      let status = CLLocationManager.authorizationStatus()
      switch status {
      case .notDetermined:
          // First time asking for WhenInUse permission
          locationManager.requestWhenInUseAuthorization()
      case .authorizedWhenInUse:
          // If already have WhenInUse, request Always now
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

      locationManager.desiredAccuracy = kCLLocationAccuracyBest
      locationManager.allowsBackgroundLocationUpdates = true
      locationManager.pausesLocationUpdatesAutomatically = false
      locationManager.startUpdatingLocation()

      call.resolve(["status": "started"])
  }


  @objc func stopUpdatingLocation(_ call: CAPPluginCall) {
    locationManager.stopUpdatingLocation()
      call.resolve(["status": "stopped"])
  }
  
  
  public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
      guard let location = locations.last else { return }

      notifyListeners("locationUpdate", data: [
          "lat": location.coordinate.latitude,
          "lng": location.coordinate.longitude
      ])
  }

  public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    print("ERROR: " + error.localizedDescription)
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
          // If we just got WhenInUse, escalate to Always
          manager.requestAlwaysAuthorization()
      case .denied, .restricted:
          call.resolve(["granted": false])
          permissionCall = nil
      case .notDetermined:
          // Waiting for user decision
          break
      @unknown default:
          call.resolve(["granted": false])
          permissionCall = nil
      }
  }
    
}
