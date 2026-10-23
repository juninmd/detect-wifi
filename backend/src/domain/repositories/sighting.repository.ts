import { Sighting } from '../entities/sighting.entity';

export interface ISightingRepository {
  saveAll(sightings: Sighting[]): Promise<void>;
  findSince(since: Date): Promise<Sighting[]>;
}
