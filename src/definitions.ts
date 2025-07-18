import type { PluginListenerHandle } from '@capacitor/core';
export interface ForegroundLocation {
  lat: number;
  lng: number;
  altitude: number;
  accuracy: number;
  speed: number;
  bearing: number;
  time: number;
}

export interface ForegroundLocationConfiguration {
  interval: number;
  distanceFilter: number;
  notificationTitle: string;
  notificationMessage: string;
  notificationImportance: number;
  notificationChannelId: number;
}
export interface ForegroundLocationConfigurationIOS {
  accuracy: 'high' | 'low';
  distanceFilter: number;
  updateInterval: number;
  batteryMode: 'default' | 'fitness' | 'navigation' | 'lowPower';
}
export interface PermissionResponse {
  granted: boolean;
}
export interface OptionResponse {
  result: string;
}
export interface ServiceRunningResponse {
  running: boolean;
}
export interface Endpoint {
  endPoint: string;
}
export interface GeofenceData {
  geofenceData: Geofence[];
}
export interface Geofence {
  clockNumber: number;
  clockDescription: string;
  locationCode: string;
  locationDescription: string;
  lat: number;
  lng: number;
  radius: number;
}
export interface UserData {
  username: string;
  userId: number;
  _token: string;
}
export interface LogsEndpoint {
  logsEndpoint: string;
}
export interface SetApiOptions {
  endpoint: Endpoint;
  geofenceData: GeofenceData;
  userData: UserData;
  logsEndpoint: LogsEndpoint;
  allowNotification: NotificationEnabled;
}
export interface NotificationEnabled {
  allowNotification: boolean;
}
export interface SetClockHistoryResponse {
  status: string;
}
export interface SetClockHistoryPayload {
  clockNumber: number;
  clockingType: 'in' | 'out';
  timestamp: number;
}
declare type CompleteOrNothing<T> = T | undefined;
export interface BackgroundNotification {
  isBackground: boolean;
}
export interface ClockHistory {
  result: boolean;
}
export interface FSClockParam {
  clockNumber: number;
  timeStamp: number;
}
export interface NotificationOptionsiOs {
  title: string;
  body: string;
}
export interface ClockingDataResponse {
  status: string;
}

export interface GetClockingDataResponse {
  logs: ClockingDataParameter[] | null;
}
export interface ClockingDataParameter {
  failedEndpoint: string;
  payload: FSAutoClockingPayload;
  error: string;
}
export interface FSAutoClockingPayload {
  empId: string;
  lat: number;
  lng: number;
  isInside: boolean;
  geofence: FSGeofenceInformationDistance;
  timeStamp: number;
}
export interface FSGeofenceInformationDistance {
  lat: number;
  lng: number;
  radius: number;
  clockDescription: string;
  clockNumber: number;
  locationCode: string;
  locationDescription: string;
}
export interface CapacitorForegroundLocationServicePlugin {
  setApiOptions(apiOptions: CompleteOrNothing<SetApiOptions>): Promise<OptionResponse>;
  config(config: ForegroundLocationConfiguration): Promise<void>;
  requestPermission(): Promise<PermissionResponse>;
  startService(): Promise<void>;
  stopService(): Promise<void>;
  addListener(
    eventName: 'locationUpdate',
    listenerFunc: (location: ForegroundLocation) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
  isLocationServiceRunning(): Promise<ServiceRunningResponse>;
  getStoredValue(): Promise<CompleteOrNothing<SetApiOptions>>;
  getApiOptions(): Promise<ForegroundLocationConfiguration>;
  initialize(config: ForegroundLocationConfigurationIOS): Promise<void>;
  startUpdatingLocation(): Promise<void>;
  stopUpdatingLocation(): Promise<void>;
  appIsInBackground(): Promise<BackgroundNotification>;
  showLocalNotification(options: NotificationOptionsiOs): Promise<void>;
  setClockHistory(clockHistory: SetClockHistoryPayload): Promise<SetClockHistoryResponse>;
  hasClockedIn(payload: FSClockParam): Promise<ClockHistory>;
  hasClockedOut(payload: FSClockParam): Promise<ClockHistory>;
  saveAutoClockData(clockData: ClockingDataParameter): Promise<ClockingDataResponse>;
  getAutoClockData(): Promise<GetClockingDataResponse>;
  eraseAutoClockingData(): Promise<ClockingDataResponse>;
}
