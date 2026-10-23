import { Sighting } from '../../domain/entities/sighting.entity';
import { ISightingRepository } from '../../domain/repositories/sighting.repository';

/** Keeps sightings in memory and drops anything older than the retention period. */
export class InMemorySightingRepository implements ISightingRepository {
  private sightings: Sighting[] = [];

  constructor(
    private readonly retentionMs: number = 60 * 60 * 1000,
    private readonly clock: () => number = Date.now,
  ) {}

  async saveAll(sightings: Sighting[]): Promise<void> {
    const oldest = this.clock() - this.retentionMs;
    this.sightings = [...this.sightings, ...sightings].filter((s) => s.seenAt.getTime() >= oldest);
  }

  async findSince(since: Date): Promise<Sighting[]> {
    return this.sightings.filter((s) => s.seenAt >= since);
  }
}
