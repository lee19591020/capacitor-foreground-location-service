import { WebPlugin } from '@capacitor/core';

import type { CapacitorForegroundLocationServicePlugin } from './definitions';

export class CapacitorForegroundLocationServiceWeb extends WebPlugin implements CapacitorForegroundLocationServicePlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
