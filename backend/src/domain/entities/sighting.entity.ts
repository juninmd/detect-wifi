/** A single Wi-Fi frame observed by a passive sensor (e.g. an ESP32 in promiscuous mode). */
export class Sighting {
  constructor(
    public readonly mac: string,
    public readonly rssi: number,
    public readonly seenAt: Date,
    public readonly sensorId: string,
  ) {}
}

export interface PresentDevice {
  readonly mac: string;
  readonly rssi: number;
  readonly lastSeen: Date;
  readonly vendor?: string;
  readonly category?: string;
}

/** Snapshot of who is nearby. Randomized MACs cannot be identified, so they are only counted. */
export interface PresenceSnapshot {
  readonly windowSeconds: number;
  readonly known: PresentDevice[];
  readonly unknown: PresentDevice[];
  readonly anonymousCount: number;
}
