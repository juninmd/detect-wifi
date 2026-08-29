import { NetworkDevice } from '../entities/network-device.entity';

export class ScanNetworkUseCase {
  async execute(): Promise<NetworkDevice[]> {
    // Stub implementation to be replaced with real ARP/mDNS/Ping sweep logic.
    return [];
  }
}
