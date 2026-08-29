import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/network_device.dart';

void main() {
  test('NetworkDevice copyWith updates status', () {
    final device = NetworkDevice(
      ip: '192.168.1.1',
      mac: 'AA:BB:CC',
      lastSeen: DateTime.now(),
    );

    final copied = device.copyWith(status: DeviceStatus.known);

    expect(copied.status, DeviceStatus.known);
    expect(copied.ip, '192.168.1.1');
  });

  test('NetworkDevice copyWith missing parameters', () {
    final device = NetworkDevice(
      ip: '192.168.1.1',
      mac: 'AA:BB:CC',
      lastSeen: DateTime.now(),
    );

    final copied = device.copyWith();

    expect(copied.status, DeviceStatus.unknown);
    expect(copied.ip, '192.168.1.1');
  });
}
