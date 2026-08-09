import '../entities/device.dart';
import '../repositories/device_repository.dart';
import '../repositories/network_repository.dart';

class ScanNetworkUseCase {
  final IDeviceRepository _deviceRepository;
  final INetworkRepository _networkRepository;

  ScanNetworkUseCase(this._deviceRepository, this._networkRepository);

  Future<List<Device>> call() async {
    final scannedDevices = await _networkRepository.scanNetwork();

    List<Device> mergedDevices = [];

    for (var device in scannedDevices) {
      final knownDevice = await _deviceRepository.getDeviceByMac(device.macAddress);

      if (knownDevice != null) {
        mergedDevices.add(device.copyWith(
          isKnown: knownDevice.isKnown,
          category: knownDevice.category,
        ));
      } else {
        mergedDevices.add(device);
      }
    }

    return mergedDevices;
  }
}
