import { Sighting } from '../entities/sighting.entity';
import { ISightingRepository } from '../repositories/sighting.repository';
import { normalizeMac } from '../utils/mac.util';

export interface RawSighting {
  mac: unknown;
  rssi: unknown;
}

const MIN_RSSI = -127;

/** Validates readings sent by a sensor and stores the valid ones; invalid entries are skipped. */
export class RecordSightingsUseCase {
  constructor(private readonly sightingRepository: ISightingRepository) {}

  async execute(sensorId: string, raw: RawSighting[], now: Date = new Date()): Promise<number> {
    if (typeof sensorId !== 'string' || sensorId.trim() === '') {
      throw new Error('Invalid sensorId');
    }

    const valid: Sighting[] = [];
    for (const item of raw) {
      const mac = normalizeMac(item?.mac);
      const rssi = item?.rssi;
      if (mac && typeof rssi === 'number' && rssi >= MIN_RSSI && rssi <= 0) {
        valid.push(new Sighting(mac, rssi, now, sensorId));
      }
    }

    if (valid.length > 0) {
      await this.sightingRepository.saveAll(valid);
    }
    return valid.length;
  }
}
