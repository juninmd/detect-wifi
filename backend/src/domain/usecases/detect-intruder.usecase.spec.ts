import { DetectIntruderUseCase } from './detect-intruder.usecase';
import { IDeviceRepository } from '../repositories/device.repository';
import { INotificationService } from '../repositories/notification.service';
import { Device } from '../entities/device.entity';

describe('DetectIntruderUseCase', () => {
  let useCase: DetectIntruderUseCase;
  let mockDeviceRepository: jest.Mocked<IDeviceRepository>;
  let mockNotificationService: jest.Mocked<INotificationService>;

  beforeEach(() => {
    mockDeviceRepository = {
      findByMacAddress: jest.fn(),
      saveDevice: jest.fn(),
      getAllDevices: jest.fn(),
      updateLastSeen: jest.fn(),
    };
    mockNotificationService = {
      sendAlert: jest.fn(),
    };
    useCase = new DetectIntruderUseCase(mockDeviceRepository, mockNotificationService);
  });

  it('should save and alert for completely new unknown device', async () => {
    mockDeviceRepository.findByMacAddress.mockResolvedValue(null);
    const newDevice = new Device('1', '00:11:22:33:44:55', '192.168.1.1', 'Vendor', false, false, 'Unknown', new Date());

    await useCase.execute([newDevice]);

    expect(mockDeviceRepository.findByMacAddress).toHaveBeenCalledWith('00:11:22:33:44:55');
    expect(mockDeviceRepository.saveDevice).toHaveBeenCalledWith(newDevice);
    expect(mockNotificationService.sendAlert).toHaveBeenCalledWith(
      'Security Alert',
      'New unknown device detected on the network.',
      { deviceId: '1', macAddress: '00:11:22:33:44:55', level: 'CRITICAL' }
    );
  });

  it('should update and alert for previously unknown device reconnecting', async () => {
    const existingDevice = new Device('1', '00:11:22:33:44:55', '192.168.1.1', 'Vendor', false, false, 'Unknown', new Date(0));
    mockDeviceRepository.findByMacAddress.mockResolvedValue(existingDevice);

    const detectedDevice = new Device('1', '00:11:22:33:44:55', '192.168.1.1', 'Vendor', false, false, 'Unknown', new Date());

    await useCase.execute([detectedDevice]);

    expect(mockDeviceRepository.findByMacAddress).toHaveBeenCalledWith('00:11:22:33:44:55');
    expect(mockDeviceRepository.saveDevice).toHaveBeenCalled();
    expect(mockNotificationService.sendAlert).toHaveBeenCalledWith(
      'Network Notification',
      'Previously unknown device reconnected.',
      expect.objectContaining({ level: 'WARNING' })
    );
  });

  it('should just update lastSeen for known device', async () => {
    const existingDevice = new Device('1', '00:11:22:33:44:55', '192.168.1.1', 'Vendor', true, true, 'Phone', new Date(0));
    mockDeviceRepository.findByMacAddress.mockResolvedValue(existingDevice);

    const detectedDevice = new Device('1', '00:11:22:33:44:55', '192.168.1.1', 'Vendor', true, true, 'Phone', new Date());

    await useCase.execute([detectedDevice]);

    expect(mockDeviceRepository.findByMacAddress).toHaveBeenCalledWith('00:11:22:33:44:55');
    expect(mockDeviceRepository.saveDevice).toHaveBeenCalled();
    expect(mockNotificationService.sendAlert).not.toHaveBeenCalled();

    // hit copy logic
    const noOverrides = existingDevice.copy();
    expect(noOverrides.category).toBe('Phone');

    const someOverrides = existingDevice.copy({ id: '2', isApproved: false });
    expect(someOverrides.id).toBe('2');
    expect(someOverrides.isApproved).toBe(false);
  });
});
