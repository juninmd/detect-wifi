import '../entities/device.dart';

abstract class DeviceRepository {
  Future<List<Device>> getKnownDevices();
}
