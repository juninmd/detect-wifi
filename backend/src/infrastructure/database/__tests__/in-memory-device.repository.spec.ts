import { InMemoryDeviceRepository } from '../in-memory-device.repository';

describe('InMemoryDeviceRepository', () => {
  it('seeds known devices from the KNOWN_MACS format and ignores invalid entries', async () => {
    const repo = new InMemoryDeviceRepository('aa:bb:cc:dd:ee:ff=Ana phone, 11-22-33-44-55-66 ,bad=x');

    const devices = await repo.getAllDevices();
    expect(devices.map((d) => [d.macAddress, d.vendor, d.isKnown])).toEqual([
      ['AA:BB:CC:DD:EE:FF', 'Ana phone', true],
      ['11:22:33:44:55:66', 'known', true],
    ]);
  });

  it('starts empty by default', async () => {
    expect(await new InMemoryDeviceRepository().getAllDevices()).toEqual([]);
  });

  it('finds, saves and updates devices', async () => {
    const repo = new InMemoryDeviceRepository('AA:BB:CC:DD:EE:FF=Ana');
    const device = await repo.findByMacAddress('AA:BB:CC:DD:EE:FF');
    expect(await repo.findByMacAddress('00:00:00:00:00:00')).toBeNull();

    await repo.saveDevice(device.copy({ category: 'phone' }));
    await repo.updateLastSeen('AA:BB:CC:DD:EE:FF', new Date(5));
    await repo.updateLastSeen('00:00:00:00:00:00', new Date(5));

    const updated = await repo.findByMacAddress('AA:BB:CC:DD:EE:FF');
    expect(updated.category).toBe('phone');
    expect(updated.lastSeen).toEqual(new Date(5));
  });
});
