import '../entities/device.dart';

abstract class NetworkRepository {
  Future<List<Device>> scanLocalNetwork();
}
