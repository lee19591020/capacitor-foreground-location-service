import { registerPlugin } from '@capacitor/core';

import type { CapacitorForegroundLocationServicePlugin } from './definitions';

const CapacitorForegroundLocationService = registerPlugin<CapacitorForegroundLocationServicePlugin>('CapacitorForegroundLocationService');

export * from './definitions';
export { CapacitorForegroundLocationService };


