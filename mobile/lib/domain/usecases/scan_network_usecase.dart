import '../entities/device.dart';
import '../repositories/network_repository.dart';
import '../repositories/device_repository.dart';

/// Use case responsável por escanear a rede e retornar os dispositivos encontrados,
/// classificando-os conforme histórico conhecido.
class ScanNetworkUseCase {
  final NetworkRepository networkRepository;
  final DeviceRepository deviceRepository;

  ScanNetworkUseCase({
    required this.networkRepository,
    required this.deviceRepository,
  });

  /// Executa o scan da rede e mescla os dados com os dispositivos salvos.
  Future<List<Device>> execute() async {
    // 1. Busca dispositivos ativos na rede atual (ARP/mDNS)
    final activeDevices = await networkRepository.scanLocalNetwork();

    // 2. Busca lista de dispositivos salvos/conhecidos do banco local
    final knownDevices = await deviceRepository.getKnownDevices();

    // 3. Classifica os dispositivos e aplica regras de whitelist
    final classifiedDevices = activeDevices.map((activeDevice) {
      final knownMatch = knownDevices.where(
        (known) => known.macAddress == activeDevice.macAddress
      ).firstOrNull;

      if (knownMatch != null) {
        return activeDevice.copyWith(
          isKnown: knownMatch.isKnown,
          customName: knownMatch.customName,
          category: knownMatch.category,
        );
      }

      // Se não conhece, por padrão é suspeito e não conhecido
      return activeDevice.copyWith(isKnown: false);
    }).toList();

    return classifiedDevices;
  }
}
