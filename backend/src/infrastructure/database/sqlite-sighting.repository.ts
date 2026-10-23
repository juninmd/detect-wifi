import { Sighting } from '../../domain/entities/sighting.entity';
import { ISightingRepository } from '../../domain/repositories/sighting.repository';

/** Minimal slice of the better-sqlite3 API, so the repository can be tested with a mock. */
export interface SqlStatement {
  run(...params: unknown[]): unknown;
  all(...params: unknown[]): unknown[];
}

export interface SqlDatabase {
  exec(sql: string): unknown;
  prepare(sql: string): SqlStatement;
  transaction<T>(fn: (items: T) => void): (items: T) => void;
}

interface SightingRow {
  mac: string;
  rssi: number;
  seen_at: number;
  sensor_id: string;
}

const SCHEMA = `
  CREATE TABLE IF NOT EXISTS sightings (
    mac TEXT NOT NULL,
    rssi INTEGER NOT NULL,
    seen_at INTEGER NOT NULL,
    sensor_id TEXT NOT NULL
  );
  CREATE INDEX IF NOT EXISTS idx_sightings_seen_at ON sightings (seen_at);
`;

/** Persists sightings in SQLite and prunes rows older than the retention period on every write. */
export class SqliteSightingRepository implements ISightingRepository {
  private readonly insert: SqlStatement;
  private readonly prune: SqlStatement;
  private readonly select: SqlStatement;
  private readonly insertAll: (items: Sighting[]) => void;

  constructor(
    db: SqlDatabase,
    private readonly retentionMs: number = 24 * 60 * 60 * 1000,
    private readonly clock: () => number = Date.now,
  ) {
    db.exec(SCHEMA);
    this.insert = db.prepare('INSERT INTO sightings (mac, rssi, seen_at, sensor_id) VALUES (?, ?, ?, ?)');
    this.prune = db.prepare('DELETE FROM sightings WHERE seen_at < ?');
    this.select = db.prepare('SELECT mac, rssi, seen_at, sensor_id FROM sightings WHERE seen_at >= ? ORDER BY seen_at');
    this.insertAll = db.transaction((items: Sighting[]) => {
      for (const s of items) this.insert.run(s.mac, s.rssi, s.seenAt.getTime(), s.sensorId);
      this.prune.run(this.clock() - this.retentionMs);
    });
  }

  async saveAll(sightings: Sighting[]): Promise<void> {
    this.insertAll(sightings);
  }

  async findSince(since: Date): Promise<Sighting[]> {
    const rows = this.select.all(since.getTime()) as SightingRow[];
    return rows.map((r) => new Sighting(r.mac, r.rssi, new Date(r.seen_at), r.sensor_id));
  }
}
