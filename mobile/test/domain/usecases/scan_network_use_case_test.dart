import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/usecases/scan_network_use_case.dart';
import 'package:mobile/domain/entities/network_device.dart';

void main() {
  late ScanNetworkUseCase useCase;

  setUp(() {
    useCase = ScanNetworkUseCase();
  });

  test('Should return an empty list initially', () async {
    final result = await useCase.execute();
    expect(result, isA<List<NetworkDevice>>());
    expect(result, isEmpty);
  });
}
