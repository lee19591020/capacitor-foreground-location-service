import type { PluginListenerHandle } from '@capacitor/core';

export interface ForegroundLocation {
  lat: number;
  lng: number;
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

export interface CapacitorForegroundLocationServicePlugin {
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
