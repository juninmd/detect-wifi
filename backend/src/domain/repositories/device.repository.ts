import { Device } from '../entities/device.entity';

export interface IDeviceRepository {
  findAll(): Promise<Device[]>;
  findByMacAddress(macAddress: string): Promise<Device | null>;
  save(device: Device): Promise<void>;
  updateStatus(macAddress: string, isKnown: boolean): Promise<void>;
}
