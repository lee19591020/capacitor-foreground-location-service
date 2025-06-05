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

export enum NotificationImportance {
  MIN = 1,
  LWO = 2,
  DEFAULT = 3,
  HIGH = 4,
  MAX = 5,
}

export interface ForegroundLocationConfiguration {
  interval: number;
  distanceFilter: number;
  notificationTitle: string;
  notificationMessage: string;
  notificationImportance: NotificationImportance;
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


export interface Endpoint {
  endPoint: string;
}
export interface GeofenceData {
  geofenceData: Geofence[]
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
  username: string,
  userId: number,
  _token: string,
}
export interface LogsEndpoint {
  logsEndpoint: string;
}

export interface SetApiOptions {
  endpoint: Endpoint;
  geofenceData: GeofenceData;
  userData: UserData;
  logsEndpoint: LogsEndpoint;
}

type CompleteOrNothing<T> = T | undefined;

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
  initialize(config: ForegroundLocationConfigurationIOS): Promise<void>;
  startUpdatingLocation(): Promise<void>;
  stopUpdatingLocation(): Promise<void>;
}
