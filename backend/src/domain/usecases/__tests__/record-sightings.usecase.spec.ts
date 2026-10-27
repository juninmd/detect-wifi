import { RecordSightingsUseCase } from '../record-sightings.usecase';
import { ISightingRepository } from '../../repositories/sighting.repository';

describe('RecordSightingsUseCase', () => {
  let repo: jest.Mocked<ISightingRepository>;
  let useCase: RecordSightingsUseCase;
  const now = new Date('2026-01-01T00:00:00Z');

  beforeEach(() => {
    repo = { saveAll: jest.fn(), findSince: jest.fn() };
    useCase = new RecordSightingsUseCase(repo);
  });

  it('rejects oversized sensorId and batches', async () => {
    await expect(useCase.execute('x'.repeat(65), [], now)).rejects.toThrow('sensorId too long');
    const big = Array.from({ length: 501 }, () => ({ mac: 'aa:bb:cc:dd:ee:ff', rssi: -60 }));
    await expect(useCase.execute('esp-1', big, now)).rejects.toThrow('Batch too large');
    expect(repo.saveAll).not.toHaveBeenCalled();
  });

  it('stores only valid sightings with normalized MACs', async () => {
    const accepted = await useCase.execute(
      'esp-1',
      [
        { mac: 'aa:bb:cc:dd:ee:ff', rssi: -60 },
        { mac: 'bad', rssi: -60 },
        { mac: 'aa:bb:cc:dd:ee:01', rssi: 5 },
        { mac: 'aa:bb:cc:dd:ee:02', rssi: '-40' },
        null as never,
      ],
      now,
    );

    expect(accepted).toBe(1);
    expect(repo.saveAll).toHaveBeenCalledWith([expect.objectContaining({ mac: 'AA:BB:CC:DD:EE:FF', seenAt: now })]);
  });

  it('does not touch the repository when nothing is valid', async () => {
    expect(await useCase.execute('esp-1', [{ mac: 'x', rssi: -1 }])).toBe(0);
    expect(repo.saveAll).not.toHaveBeenCalled();
  });

  it('rejects an invalid sensorId', async () => {
    await expect(useCase.execute('  ', [])).rejects.toThrow('Invalid sensorId');
    await expect(useCase.execute(undefined as never, [])).rejects.toThrow('Invalid sensorId');
  });
});
