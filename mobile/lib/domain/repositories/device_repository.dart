import '../entities/device.dart';

abstract class IDeviceRepository {
  Future<List<Device>> getAllDevices();
  Future<Device?> getDeviceByMac(String macAddress);
  Future<void> saveDevice(Device device);
  Future<void> updateDeviceStatus(String macAddress, bool isKnown);
}
