import { InMemorySightingRepository } from '../in-memory-sighting.repository';
import { Sighting } from '../../../domain/entities/sighting.entity';

describe('InMemorySightingRepository', () => {
  it('drops sightings older than the retention and filters by since', async () => {
    let clock = 10_000;
    const repo = new InMemorySightingRepository(5_000, () => clock);
    await repo.saveAll([new Sighting('A', -1, new Date(6_000), 's'), new Sighting('B', -1, new Date(9_000), 's')]);

    clock = 12_000;
    await repo.saveAll([new Sighting('C', -1, new Date(11_000), 's')]);

    expect((await repo.findLatestPerMacSince(new Date(0))).map((s) => s.mac)).toEqual(['B', 'C']);
    expect((await repo.findLatestPerMacSince(new Date(10_000))).map((s) => s.mac)).toEqual(['C']);
  });

  it('works with default retention and clock', async () => {
    const repo = new InMemorySightingRepository();
    await repo.saveAll([new Sighting('A', -1, new Date(), 's')]);
    expect(await repo.findLatestPerMacSince(new Date(0))).toHaveLength(1);
  });

  it('returns only the most recent sighting of each MAC', async () => {
    const repo = new InMemorySightingRepository(60_000, () => 10_000);
    await repo.saveAll([
      new Sighting('A', -70, new Date(5_000), 's1'),
      new Sighting('A', -50, new Date(9_000), 's2'),
      new Sighting('A', -90, new Date(7_000), 's1'),
      new Sighting('B', -60, new Date(8_000), 's1'),
    ]);

    const latest = await repo.findLatestPerMacSince(new Date(0));

    expect(latest).toEqual([new Sighting('A', -50, new Date(9_000), 's2'), new Sighting('B', -60, new Date(8_000), 's1')]);
  });
});
