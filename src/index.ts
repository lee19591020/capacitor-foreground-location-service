import { registerPlugin } from '@capacitor/core';

import type { CapacitorForegroundLocationServicePlugin } from './definitions';

const CapacitorForegroundLocationService = registerPlugin<CapacitorForegroundLocationServicePlugin>('CapacitorForegroundLocationService', {
  web: () => import('./web').then((m) => new m.CapacitorForegroundLocationServiceWeb()),
});

export * from './definitions';
export { CapacitorForegroundLocationService };
