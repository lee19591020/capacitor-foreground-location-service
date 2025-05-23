import type { PluginListenerHandle } from "@capacitor/core";

export interface ForegroundLocation {
    lat: number;
    lng: number;
}

export interface CapacitorForegroundLocationServicePlugin {
  startService(): Promise<void>;
  stopService(): Promise<void>;
  addListener(
    eventName: 'locationUpdate',
    listenerFunc: (location: ForegroundLocation) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}