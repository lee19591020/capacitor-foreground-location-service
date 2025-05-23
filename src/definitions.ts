export interface CapacitorForegroundLocationServicePlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
