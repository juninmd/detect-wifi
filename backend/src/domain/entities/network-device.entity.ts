export enum DeviceCategory {
  SMARTPHONE = 'smartphone',
  IOT = 'iot',
  COMPUTER = 'computer',
  UNKNOWN = 'unknown',
}

export enum DeviceStatus {
  KNOWN = 'known',
  UNKNOWN = 'unknown',
  BLOCKED = 'blocked',
}

export class NetworkDevice {
  constructor(
    public readonly ip: string,
    public readonly mac: string,
    public readonly lastSeen: Date,
    public readonly vendor?: string,
    public readonly category: DeviceCategory = DeviceCategory.UNKNOWN,
    public readonly status: DeviceStatus = DeviceStatus.UNKNOWN,
  ) {}

  copyWith(params: Partial<NetworkDevice>): NetworkDevice {
    return new NetworkDevice(
      params.ip ?? this.ip,
      params.mac ?? this.mac,
      params.lastSeen ?? this.lastSeen,
      params.vendor !== undefined ? params.vendor : this.vendor,
      params.category ?? this.category,
      params.status ?? this.status,
    );
  }
}
