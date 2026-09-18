import { ScanNetworkUseCase, INetworkScanner } from '../scan-network.usecase';
import { IDeviceRepository } from '../../repositories/device.repository';
import { Device } from '../../entities/device.entity';

describe('ScanNetworkUseCase', () => {
  let useCase: ScanNetworkUseCase;
  let mockDeviceRepository: jest.Mocked<IDeviceRepository>;
  let mockNetworkScanner: jest.Mocked<INetworkScanner>;

  beforeEach(() => {
    mockDeviceRepository = {
      findByMacAddress: jest.fn(),
      saveDevice: jest.fn(),
      getAllDevices: jest.fn(),
      updateLastSeen: jest.fn(),
    };
    mockNetworkScanner = {
      scanNetwork: jest.fn(),
    };
    useCase = new ScanNetworkUseCase(mockDeviceRepository, mockNetworkScanner);
  });

  it('should return merged devices', async () => {
    const scannedDevice1 = new Device('1', '00:11:22:33:44:55', '192.168.1.1', 'Vendor', false, false, 'unknown', new Date());
    const scannedDevice2 = new Device('2', 'aa:bb:cc:dd:ee:ff', '192.168.1.2', 'Vendor', false, false, 'unknown', new Date());

    mockNetworkScanner.scanNetwork.mockResolvedValue([scannedDevice1, scannedDevice2]);

    const knownDevice = new Device('1', '00:11:22:33:44:55', '192.168.1.1', 'Vendor', true, false, 'smartphone', new Date());
    mockDeviceRepository.findByMacAddress.mockImplementation(async (mac: string) => {
      if (mac === '00:11:22:33:44:55') return knownDevice;
      return null;
    });

    const result = await useCase.execute();

    expect(result.length).toBe(2);
    expect(result[0].isKnown).toBe(true);
    expect(result[0].category).toBe('smartphone');
    expect(result[1].isKnown).toBe(false);
    expect(result[1].category).toBe('unknown');
  });
});
