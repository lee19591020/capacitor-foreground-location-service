import type { PluginListenerHandle } from '@capacitor/core';

export interface ForegroundLocation {
  lat: number;
  lng: number;
}
export interface ForegroundLocationConfiguration {
  interval: number;
  distanceFilter: number;
  notificationTitle: string;
  notificationMessage: string;
}
export interface PermissionResponse {
  granted: boolean;
}

export interface CapacitorForegroundLocationServicePlugin {
  config(config: ForegroundLocationConfiguration): Promise<void>;
  requestPermission(): Promise<PermissionResponse>;
  startService(): Promise<void>;
  stopService(): Promise<void>;
  addListener(
    eventName: 'locationUpdate',
    listenerFunc: (location: ForegroundLocation) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
  initialize(): Promise<void>;
  startUpdatingLocation(): Promise<void>;
  stopUpdatingLocation(): Promise<void>;
}
