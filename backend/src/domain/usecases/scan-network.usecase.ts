import { IDeviceRepository } from '../repositories/device.repository';
import { Device } from '../entities/device.entity';

export interface INetworkScanner {
  scanNetwork(): Promise<Device[]>;
}

export class ScanNetworkUseCase {
  constructor(
    private readonly deviceRepository: IDeviceRepository,
    private readonly networkScanner: INetworkScanner,
  ) {}

  async execute(): Promise<Device[]> {
    const scannedDevices = await this.networkScanner.scanNetwork();
    const mergedDevices: Device[] = [];

    for (const device of scannedDevices) {
      const knownDevice = await this.deviceRepository.findByMacAddress(device.macAddress);

      if (knownDevice != null) {
        mergedDevices.push(
          device.copy({
            isKnown: knownDevice.isKnown,
            category: knownDevice.category,
          }),
        );
      } else {
        mergedDevices.push(device);
      }
    }

    return mergedDevices;
  }
}
