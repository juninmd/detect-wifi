import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:mobile/domain/entities/device.dart';
import 'package:mobile/domain/repositories/device_repository.dart';
import 'package:mobile/domain/repositories/network_repository.dart';
import 'package:mobile/domain/usecases/scan_network_usecase.dart';

class MockNetworkRepository extends Mock implements INetworkRepository {}
class MockDeviceRepository extends Mock implements IDeviceRepository {}

void main() {
  late ScanNetworkUseCase usecase;
  late MockNetworkRepository mockNetworkRepository;
  late MockDeviceRepository mockDeviceRepository;

  setUp(() {
    mockNetworkRepository = MockNetworkRepository();
    mockDeviceRepository = MockDeviceRepository();
    usecase = ScanNetworkUseCase(mockDeviceRepository, mockNetworkRepository);
  });

  test('should return a list of merged devices', () async {
    // Arrange
    final lastSeen = DateTime.now();
    final scannedDevice1 = Device(id: '1', ipAddress: '192.168.1.2', macAddress: '00:11:22:33:44:55', vendor: 'Vendor1', isKnown: false, category: 'Unknown', lastSeen: lastSeen);
    final scannedDevice2 = Device(id: '2', ipAddress: '192.168.1.3', macAddress: 'aa:bb:cc:dd:ee:ff', vendor: 'Vendor2', isKnown: false, category: 'Unknown', lastSeen: lastSeen);

    final knownDevice = Device(id: '1', ipAddress: '192.168.1.2', macAddress: '00:11:22:33:44:55', vendor: 'Vendor1', isKnown: true, category: 'Phone', lastSeen: lastSeen);

    when(() => mockNetworkRepository.scanNetwork()).thenAnswer((_) async => [scannedDevice1, scannedDevice2]);
    when(() => mockDeviceRepository.getDeviceByMac('00:11:22:33:44:55')).thenAnswer((_) async => knownDevice);
    when(() => mockDeviceRepository.getDeviceByMac('aa:bb:cc:dd:ee:ff')).thenAnswer((_) async => null);

    // Act
    final result = await usecase.call();

    // Assert
    expect(result.length, 2);
    expect(result[0].isKnown, true);
    expect(result[0].category, 'Phone');
    expect(result[1].isKnown, false);
    expect(result[1].category, 'Unknown');

    // Testing copyWith fully uncovered lines
    final copiedDevice = result[0].copyWith();
    expect(copiedDevice.category, 'Phone');
    expect(copiedDevice.id, '1');
    expect(copiedDevice.macAddress, '00:11:22:33:44:55');
    expect(copiedDevice.ipAddress, '192.168.1.2');
    expect(copiedDevice.vendor, 'Vendor1');
    expect(copiedDevice.isKnown, true);
    expect(copiedDevice.lastSeen, lastSeen);

    verify(() => mockNetworkRepository.scanNetwork()).called(1);
    verify(() => mockDeviceRepository.getDeviceByMac('00:11:22:33:44:55')).called(1);
    verify(() => mockDeviceRepository.getDeviceByMac('aa:bb:cc:dd:ee:ff')).called(1);
  });
}
