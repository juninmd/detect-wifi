import { NetworkDevice, DeviceCategory, DeviceStatus } from '../network-device.entity';

describe('NetworkDevice Entity', () => {
  it('should construct with default values', () => {
    const device = new NetworkDevice('192.168.1.1', '00:11:22:33:44:55', new Date());
    expect(device.category).toBe(DeviceCategory.UNKNOWN);
    expect(device.status).toBe(DeviceStatus.UNKNOWN);
  });

  it('should copy with provided values', () => {
    const lastSeen = new Date();
    const device = new NetworkDevice('192.168.1.1', '00:11:22:33:44:55', lastSeen);

    const copied = device.copyWith({
      ip: '10.0.0.1',
      mac: 'AA:BB:CC:DD:EE:FF',
      vendor: 'Test Vendor',
      category: DeviceCategory.IOT,
      status: DeviceStatus.KNOWN
    });

    expect(copied.ip).toBe('10.0.0.1');
    expect(copied.mac).toBe('AA:BB:CC:DD:EE:FF');
    expect(copied.vendor).toBe('Test Vendor');
    expect(copied.category).toBe(DeviceCategory.IOT);
    expect(copied.status).toBe(DeviceStatus.KNOWN);
  });

  it('should fall back to original values when copyWith parameters are undefined', () => {
    const lastSeen = new Date();
    const device = new NetworkDevice(
      '192.168.1.1',
      '00:11:22:33:44:55',
      lastSeen,
      'Test Vendor',
      DeviceCategory.IOT,
      DeviceStatus.KNOWN
    );

    const copied = device.copyWith({});

    expect(copied.ip).toBe('192.168.1.1');
    expect(copied.mac).toBe('00:11:22:33:44:55');
    expect(copied.lastSeen).toBe(lastSeen);
    expect(copied.vendor).toBe('Test Vendor');
    expect(copied.category).toBe(DeviceCategory.IOT);
    expect(copied.status).toBe(DeviceStatus.KNOWN);
  });
});
