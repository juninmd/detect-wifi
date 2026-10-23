import { Device } from '../../domain/entities/device.entity';
import { IDeviceRepository } from '../../domain/repositories/device.repository';
import { normalizeMac } from '../../domain/utils/mac.util';

/**
 * Device registry kept in memory. Seed it with `KNOWN_MACS` ("AA:BB:..=Ana phone,CC:DD:..=TV"):
 * these devices count as "known" (trusted household devices).
 */
export class InMemoryDeviceRepository implements IDeviceRepository {
  private readonly devices = new Map<string, Device>();

  constructor(knownMacs: string = '') {
    for (const entry of knownMacs.split(',')) {
      const [rawMac, label = 'known'] = entry.split('=');
      const mac = normalizeMac(rawMac.trim());
      if (mac) {
        this.devices.set(mac, new Device(mac, mac, '', label.trim(), true, true, 'known', new Date(0)));
      }
    }
  }

  async getAllDevices(): Promise<Device[]> {
    return [...this.devices.values()];
  }

  async findByMacAddress(macAddress: string): Promise<Device | null> {
    return this.devices.get(macAddress) ?? null;
  }

  async saveDevice(device: Device): Promise<void> {
    this.devices.set(device.macAddress, device);
  }

  async updateLastSeen(macAddress: string, lastSeen: Date): Promise<void> {
    const device = this.devices.get(macAddress);
    if (device) this.devices.set(macAddress, device.copy({ lastSeen }));
  }
}
