import { PresentDevice, PresenceSnapshot } from '../entities/sighting.entity';
import { IDeviceRepository } from '../repositories/device.repository';
import { ISightingRepository } from '../repositories/sighting.repository';
import { isRandomizedMac } from '../utils/mac.util';

/** Builds a presence snapshot from the sightings inside the time window. */
export class GetPresenceUseCase {
  constructor(
    private readonly sightingRepository: ISightingRepository,
    private readonly deviceRepository: IDeviceRepository,
  ) {}

  async execute(windowSeconds: number, now: Date = new Date()): Promise<PresenceSnapshot> {
    const since = new Date(now.getTime() - windowSeconds * 1000);
    const latest = await this.sightingRepository.findLatestPerMacSince(since);

    const known: PresentDevice[] = [];
    const unknown: PresentDevice[] = [];
    let anonymousCount = 0;

    for (const sighting of latest) {
      if (isRandomizedMac(sighting.mac)) {
        anonymousCount++;
        continue;
      }
      const device = await this.deviceRepository.findByMacAddress(sighting.mac);
      const present: PresentDevice = {
        mac: sighting.mac,
        rssi: sighting.rssi,
        lastSeen: sighting.seenAt,
        vendor: device?.vendor,
        category: device?.category,
      };
      (device?.isKnown ? known : unknown).push(present);
    }

    return { windowSeconds, known, unknown, anonymousCount };
  }
}
