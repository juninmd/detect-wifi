import { Device } from '../entities/device.entity';

export interface IDeviceRepository {
  getAllDevices(): Promise<Device[]>;
  saveDevice(device: Device): Promise<void>;
  updateLastSeen(macAddress: string, date: Date): Promise<void>;
}
