import '../entities/network_device.dart';

class ClassifyDeviceUseCase {
  NetworkDevice execute(NetworkDevice device) {
    if (device.vendor != null) {
      final vendor = device.vendor!.toLowerCase();
      if (vendor.contains('apple') ||
          vendor.contains('samsung') ||
          vendor.contains('motorola')) {
        return device.copyWith(category: DeviceCategory.smartphone);
      } else if (vendor.contains('intel') ||
          vendor.contains('dell') ||
          vendor.contains('hp')) {
        return device.copyWith(category: DeviceCategory.computer);
      } else if (vendor.contains('espressif') ||
          vendor.contains('tuya') ||
          vendor.contains('philips')) {
        return device.copyWith(category: DeviceCategory.iot);
      }
    }
    return device.copyWith(category: DeviceCategory.unknown);
  }
}
