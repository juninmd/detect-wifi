import { Device } from '../entities/device.entity';

export interface IDeviceRepository {
  getAllDevices(): Promise<Device[]>;
  findByMacAddress(macAddress: string): Promise<Device | null>;
  saveDevice(device: Device): Promise<void>;
  updateLastSeen(macAddress: string, lastSeen: Date): Promise<void>;
}
