import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/network_device.dart';
import 'package:mobile/domain/usecases/classify_device_use_case.dart';

void main() {
  late ClassifyDeviceUseCase useCase;

  setUp(() {
    useCase = ClassifyDeviceUseCase();
  });

  test('Should classify Apple device as smartphone', () {
    final device = NetworkDevice(
      ip: '192.168.1.2',
      mac: '00:11:22:33:44:55',
      vendor: 'Apple, Inc.',
      lastSeen: DateTime.now(),
    );

    final result = useCase.execute(device);

    expect(result.category, DeviceCategory.smartphone);
  });

  test('Should classify Intel device as computer', () {
    final device = NetworkDevice(
      ip: '192.168.1.3',
      mac: '00:11:22:33:44:66',
      vendor: 'Intel Corporate',
      lastSeen: DateTime.now(),
    );

    final result = useCase.execute(device);

    expect(result.category, DeviceCategory.computer);
  });

  test('Should classify Espressif device as IoT', () {
    final device = NetworkDevice(
      ip: '192.168.1.4',
      mac: '00:11:22:33:44:77',
      vendor: 'Espressif Inc.',
      lastSeen: DateTime.now(),
    );

    final result = useCase.execute(device);

    expect(result.category, DeviceCategory.iot);
  });

  test('Should classify unknown vendor as unknown', () {
    final device = NetworkDevice(
      ip: '192.168.1.5',
      mac: '00:11:22:33:44:88',
      vendor: 'Unknown Vendor',
      lastSeen: DateTime.now(),
    );

    final result = useCase.execute(device);

    expect(result.category, DeviceCategory.unknown);
  });

  test('Should classify null vendor as unknown', () {
    final device = NetworkDevice(
      ip: '192.168.1.6',
      mac: '00:11:22:33:44:99',
      vendor: null,
      lastSeen: DateTime.now(),
    );

    final result = useCase.execute(device);

    expect(result.category, DeviceCategory.unknown);
  });
}
