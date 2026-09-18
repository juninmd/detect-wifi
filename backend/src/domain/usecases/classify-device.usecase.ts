import { IDeviceRepository } from '../repositories/device.repository';
import { Device } from '../entities/device.entity';
import { DeviceCategory } from '../entities/network-device.entity';

export class ClassifyDeviceUseCase {
  constructor(private readonly deviceRepository: IDeviceRepository) {}

  async execute(macAddress: string): Promise<Device> {
    const device = await this.deviceRepository.findByMacAddress(macAddress);
    if (!device) {
      throw new Error('Device not found');
    }

    let category = DeviceCategory.UNKNOWN;
    if (device.vendor) {
      const vendorLower = device.vendor.toLowerCase();
      if (vendorLower.includes('apple') || vendorLower.includes('samsung') || vendorLower.includes('motorola')) {
        category = DeviceCategory.SMARTPHONE;
      } else if (vendorLower.includes('intel') || vendorLower.includes('dell') || vendorLower.includes('hp')) {
        category = DeviceCategory.COMPUTER;
      } else if (vendorLower.includes('espressif') || vendorLower.includes('tuya') || vendorLower.includes('philips')) {
        category = DeviceCategory.IOT;
      }
    }

    const updatedDevice = device.copy({ category });
    await this.deviceRepository.saveDevice(updatedDevice);
    return updatedDevice;
  }
}
