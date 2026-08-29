import { ClassifyDeviceUseCase } from '../classify-device.usecase';
import { NetworkDevice, DeviceCategory } from '../../entities/network-device.entity';

describe('ClassifyDeviceUseCase', () => {
  let useCase: ClassifyDeviceUseCase;

  beforeEach(() => {
    useCase = new ClassifyDeviceUseCase();
  });

  it('should classify Apple device as smartphone', () => {
    const device = new NetworkDevice('192.168.1.2', '00:11:22:33:44:55', new Date(), 'Apple, Inc.');
    const result = useCase.execute(device);
    expect(result.category).toBe(DeviceCategory.SMARTPHONE);
  });

  it('should classify Intel device as computer', () => {
    const device = new NetworkDevice('192.168.1.3', '00:11:22:33:44:66', new Date(), 'Intel Corporate');
    const result = useCase.execute(device);
    expect(result.category).toBe(DeviceCategory.COMPUTER);
  });

  it('should classify Espressif device as IoT', () => {
    const device = new NetworkDevice('192.168.1.4', '00:11:22:33:44:77', new Date(), 'Espressif Inc.');
    const result = useCase.execute(device);
    expect(result.category).toBe(DeviceCategory.IOT);
  });

  it('should classify unknown vendor as unknown', () => {
    const device = new NetworkDevice('192.168.1.5', '00:11:22:33:44:88', new Date(), 'Unknown Vendor');
    const result = useCase.execute(device);
    expect(result.category).toBe(DeviceCategory.UNKNOWN);
  });

  it('should classify null/undefined vendor as unknown', () => {
    const device = new NetworkDevice('192.168.1.6', '00:11:22:33:44:99', new Date());
    const result = useCase.execute(device);
    expect(result.category).toBe(DeviceCategory.UNKNOWN);
  });
});
