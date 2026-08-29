import { NetworkDevice, DeviceCategory } from '../entities/network-device.entity';

export class ClassifyDeviceUseCase {
  execute(device: NetworkDevice): NetworkDevice {
    if (device.vendor) {
      const vendor = device.vendor.toLowerCase();
      if (vendor.includes('apple') || vendor.includes('samsung') || vendor.includes('motorola')) {
        return device.copyWith({ category: DeviceCategory.SMARTPHONE });
      } else if (vendor.includes('intel') || vendor.includes('dell') || vendor.includes('hp')) {
        return device.copyWith({ category: DeviceCategory.COMPUTER });
      } else if (vendor.includes('espressif') || vendor.includes('tuya') || vendor.includes('philips')) {
        return device.copyWith({ category: DeviceCategory.IOT });
      }
    }
    return device.copyWith({ category: DeviceCategory.UNKNOWN });
  }
}
