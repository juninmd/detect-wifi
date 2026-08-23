import '../entities/device.dart';

abstract class INetworkRepository {
  Future<List<Device>> scanNetwork();
}
