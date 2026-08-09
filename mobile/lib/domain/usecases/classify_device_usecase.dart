import '../entities/device.dart';
import '../repositories/device_repository.dart';

class ClassifyDeviceUseCase {
  final IDeviceRepository _deviceRepository;

  ClassifyDeviceUseCase(this._deviceRepository);

  Future<void> call(String macAddress, bool isKnown) async {
    final device = await _deviceRepository.getDeviceByMac(macAddress);
    if (device != null) {
      await _deviceRepository.updateDeviceStatus(macAddress, isKnown);
    } else {
      throw Exception('Device not found');
    }
  }
}
