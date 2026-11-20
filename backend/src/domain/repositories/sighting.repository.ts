import { Sighting } from '../entities/sighting.entity';

export interface ISightingRepository {
  saveAll(sightings: Sighting[]): Promise<void>;
  /** Most recent sighting of each MAC seen at or after `since`. */
  findLatestPerMacSince(since: Date): Promise<Sighting[]>;
}
