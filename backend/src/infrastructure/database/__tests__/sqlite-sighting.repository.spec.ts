import { SqliteSightingRepository, SqlDatabase, SqlStatement } from '../sqlite-sighting.repository';
import Database = require('better-sqlite3');
import { Sighting } from '../../../domain/entities/sighting.entity';

describe('SqliteSightingRepository', () => {
  let statements: Record<string, jest.Mocked<SqlStatement>>;
  let db: jest.Mocked<SqlDatabase>;

  const build = (retentionMs?: number, clock?: () => number) => new SqliteSightingRepository(db, retentionMs, clock);

  beforeEach(() => {
    statements = {};
    db = {
      exec: jest.fn(),
      prepare: jest.fn((sql: string) => (statements[sql.split(' ')[0]] = { run: jest.fn(), all: jest.fn() })),
      transaction: jest.fn((fn) => fn),
    } as never;
  });

  it('creates the schema and prepares statements once', () => {
    build();
    expect(db.exec).toHaveBeenCalledWith(expect.stringContaining('CREATE TABLE IF NOT EXISTS sightings'));
    expect(db.prepare).toHaveBeenCalledTimes(3);
  });

  it('inserts every sighting and prunes old rows in one transaction', async () => {
    const repo = build(1000, () => 5000);
    await repo.saveAll([new Sighting('AA', -40, new Date(4500), 's1'), new Sighting('BB', -70, new Date(4600), 's2')]);

    expect(db.transaction).toHaveBeenCalledTimes(1);
    expect(statements.INSERT.run).toHaveBeenNthCalledWith(1, 'AA', -40, 4500, 's1');
    expect(statements.INSERT.run).toHaveBeenNthCalledWith(2, 'BB', -70, 4600, 's2');
    expect(statements.DELETE.run).toHaveBeenCalledWith(4000);
  });

  it('uses default retention and clock', async () => {
    const repo = build();
    await repo.saveAll([]);
    const cutoff = statements.DELETE.run.mock.calls[0][0] as number;
    expect(Date.now() - cutoff).toBeGreaterThanOrEqual(24 * 60 * 60 * 1000 - 1000);
  });

  it('maps rows back to sightings', async () => {
    const repo = build();
    statements.SELECT.all.mockReturnValue([{ mac: 'AA', rssi: -50, seen_at: 1234, sensor_id: 's1' }]);

    const result = await repo.findLatestPerMacSince(new Date(1000));

    expect(statements.SELECT.all).toHaveBeenCalledWith(1000);
    expect(result).toEqual([new Sighting('AA', -50, new Date(1234), 's1')]);
  });

  it('returns the latest row of each MAC from a real SQLite database', async () => {
    const repo = new SqliteSightingRepository(new Database(':memory:') as unknown as SqlDatabase, 60_000, () => 10_000);
    await repo.saveAll([
      new Sighting('AA', -70, new Date(5_000), 's1'),
      new Sighting('AA', -50, new Date(9_000), 's2'),
      new Sighting('AA', -90, new Date(7_000), 's1'),
      new Sighting('BB', -60, new Date(8_000), 's1'),
      new Sighting('CC', -60, new Date(1_000), 's1'),
    ]);

    const latest = await repo.findLatestPerMacSince(new Date(2_000));

    expect(latest.sort((a, b) => a.mac.localeCompare(b.mac))).toEqual([
      new Sighting('AA', -50, new Date(9_000), 's2'),
      new Sighting('BB', -60, new Date(8_000), 's1'),
    ]);
  });
});
