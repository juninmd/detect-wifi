import { GetPresenceUseCase } from '../get-presence.usecase';
import { ISightingRepository } from '../../repositories/sighting.repository';
import { IDeviceRepository } from '../../repositories/device.repository';
import { Sighting } from '../../entities/sighting.entity';
import { Device } from '../../entities/device.entity';

describe('GetPresenceUseCase', () => {
  let sightings: jest.Mocked<ISightingRepository>;
  let devices: jest.Mocked<IDeviceRepository>;
  let useCase: GetPresenceUseCase;
  const now = new Date('2026-01-01T00:10:00Z');
  const at = (s: number) => new Date(now.getTime() - s * 1000);

  beforeEach(() => {
    sightings = { saveAll: jest.fn(), findSince: jest.fn() };
    devices = {
      findByMacAddress: jest.fn(),
      saveDevice: jest.fn(),
      getAllDevices: jest.fn(),
      updateLastSeen: jest.fn(),
    };
    useCase = new GetPresenceUseCase(sightings, devices);
  });

  it('splits known, unknown and anonymous devices using the latest sighting per MAC', async () => {
    sightings.findSince.mockResolvedValue([
      new Sighting('00:11:22:33:44:55', -70, at(60), 's1'),
      new Sighting('00:11:22:33:44:55', -50, at(10), 's1'),
      new Sighting('00:11:22:33:44:55', -90, at(120), 's1'),
      new Sighting('00:AA:BB:CC:DD:EE', -80, at(5), 's1'),
      new Sighting('02:AA:BB:CC:DD:EE', -80, at(5), 's1'),
    ]);
    devices.findByMacAddress.mockImplementation(async (mac) =>
      mac === '00:11:22:33:44:55' ? new Device('1', mac, '', 'Ana phone', true, true, 'known', at(0)) : null,
    );

    const snapshot = await useCase.execute(300, now);

    expect(sightings.findSince).toHaveBeenCalledWith(at(300));
    expect(snapshot.known).toEqual([
      { mac: '00:11:22:33:44:55', rssi: -50, lastSeen: at(10), vendor: 'Ana phone', category: 'known' },
    ]);
    expect(snapshot.unknown).toEqual([
      { mac: '00:AA:BB:CC:DD:EE', rssi: -80, lastSeen: at(5), vendor: undefined, category: undefined },
    ]);
    expect(snapshot.anonymousCount).toBe(1);
  });

  it('treats a stored but not-known device as unknown', async () => {
    sightings.findSince.mockResolvedValue([new Sighting('00:11:22:33:44:55', -50, at(1), 's1')]);
    devices.findByMacAddress.mockResolvedValue(new Device('1', '00:11:22:33:44:55', '', 'V', false, false, 'x', at(0)));

    const snapshot = await useCase.execute(60, now);

    expect(snapshot.known).toHaveLength(0);
    expect(snapshot.unknown[0].vendor).toBe('V');
  });

  it('defaults the reference time to now', async () => {
    sightings.findSince.mockResolvedValue([]);
    await useCase.execute(60);
    const since: Date = sightings.findSince.mock.calls[0][0];
    expect(Date.now() - since.getTime()).toBeGreaterThanOrEqual(59_000);
  });
});
