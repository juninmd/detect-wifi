import { ClassifyDeviceUseCase } from '../classify-device.usecase';
import { IDeviceRepository } from '../../repositories/device.repository';
import { DeviceCategory } from '../../entities/network-device.entity';
import { Device } from '../../entities/device.entity';

describe('ClassifyDeviceUseCase', () => {
  let useCase: ClassifyDeviceUseCase;
  let mockDeviceRepository: jest.Mocked<IDeviceRepository>;

  beforeEach(() => {
    mockDeviceRepository = {
      findByMacAddress: jest.fn(),
      saveDevice: jest.fn(),
      getAllDevices: jest.fn(),
      updateLastSeen: jest.fn(),
    };
    useCase = new ClassifyDeviceUseCase(mockDeviceRepository);
  });

  it('should throw an error if device is not found', async () => {
    mockDeviceRepository.findByMacAddress.mockResolvedValue(null);
    await expect(useCase.execute('00:00:00:00:00:00')).rejects.toThrow('Device not found');
  });

  it('should classify Apple device as smartphone', async () => {
    const device = new Device('1', '00:11:22:33:44:55', '192.168.1.1', 'Apple, Inc.', false, false, 'unknown', new Date());
    mockDeviceRepository.findByMacAddress.mockResolvedValue(device);

    const result = await useCase.execute('00:11:22:33:44:55');

    expect(result.category).toBe(DeviceCategory.SMARTPHONE);
    expect(mockDeviceRepository.saveDevice).toHaveBeenCalledWith(expect.objectContaining({ category: DeviceCategory.SMARTPHONE }));
  });

  it('should classify Intel device as computer', async () => {
    const device = new Device('2', '00:11:22:33:44:66', '192.168.1.2', 'Intel Corporate', false, false, 'unknown', new Date());
    mockDeviceRepository.findByMacAddress.mockResolvedValue(device);

    const result = await useCase.execute('00:11:22:33:44:66');

    expect(result.category).toBe(DeviceCategory.COMPUTER);
  });

  it('should classify Espressif device as IoT', async () => {
    const device = new Device('3', '00:11:22:33:44:77', '192.168.1.3', 'Espressif Inc.', false, false, 'unknown', new Date());
    mockDeviceRepository.findByMacAddress.mockResolvedValue(device);

    const result = await useCase.execute('00:11:22:33:44:77');

    expect(result.category).toBe(DeviceCategory.IOT);
  });

  it('should classify unknown vendor as unknown', async () => {
    const device = new Device('4', '00:11:22:33:44:88', '192.168.1.4', 'Unknown Vendor', false, false, 'unknown', new Date());
    mockDeviceRepository.findByMacAddress.mockResolvedValue(device);

    const result = await useCase.execute('00:11:22:33:44:88');

    expect(result.category).toBe(DeviceCategory.UNKNOWN);
  });
});
