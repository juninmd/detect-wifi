import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:mobile/domain/entities/device.dart';
import 'package:mobile/domain/repositories/device_repository.dart';
import 'package:mobile/domain/usecases/classify_device_usecase.dart';

class MockDeviceRepository extends Mock implements IDeviceRepository {}

void main() {
  late ClassifyDeviceUseCase usecase;
  late MockDeviceRepository mockDeviceRepository;

  setUp(() {
    mockDeviceRepository = MockDeviceRepository();
    usecase = ClassifyDeviceUseCase(mockDeviceRepository);
  });

  test('should update device status if device exists', () async {
    // Arrange
    final device = Device(
      id: '1',
      macAddress: '00:11:22:33:44:55',
      ipAddress: '192.168.1.2',
      vendor: 'Vendor',
      isKnown: false,
      category: 'Unknown',
      lastSeen: DateTime.now(),
    );

    when(() => mockDeviceRepository.getDeviceByMac('00:11:22:33:44:55'))
        .thenAnswer((_) async => device);
    when(() => mockDeviceRepository.updateDeviceStatus('00:11:22:33:44:55', true))
        .thenAnswer((_) async => Future.value());

    // Act
    await usecase('00:11:22:33:44:55', true);

    // Assert
    verify(() => mockDeviceRepository.getDeviceByMac('00:11:22:33:44:55')).called(1);
    verify(() => mockDeviceRepository.updateDeviceStatus('00:11:22:33:44:55', true)).called(1);
  });

  test('should throw exception if device not found', () async {
    // Arrange
    when(() => mockDeviceRepository.getDeviceByMac('00:11:22:33:44:55'))
        .thenAnswer((_) async => null);

    // Act
    final call = usecase('00:11:22:33:44:55', true);

    // Assert
    expect(() => call, throwsA(isA<Exception>()));
    verify(() => mockDeviceRepository.getDeviceByMac('00:11:22:33:44:55')).called(1);
    verifyNever(() => mockDeviceRepository.updateDeviceStatus(any(), any()));
  });
}
