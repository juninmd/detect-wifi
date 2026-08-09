export class Device {
  constructor(
    public readonly id: string,
    public readonly macAddress: string,
    public readonly ipAddress: string,
    public readonly vendor: string,
    public readonly isKnown: boolean,
    public readonly category: string,
    public readonly lastSeen: Date,
  ) {}

  copy(overrides?: Partial<Device>): Device {
    return new Device(
      overrides?.id ?? this.id,
      overrides?.macAddress ?? this.macAddress,
      overrides?.ipAddress ?? this.ipAddress,
      overrides?.vendor ?? this.vendor,
      overrides?.isKnown ?? this.isKnown,
      overrides?.category ?? this.category,
      overrides?.lastSeen ?? this.lastSeen,
    );
  }
}
