import { Device } from '../entities/device.entity';
import { IDeviceRepository } from '../repositories/device.repository';
import { INotificationService } from '../repositories/notification.service';

export class DetectIntruderUseCase {
  constructor(
    private readonly deviceRepository: IDeviceRepository,
    private readonly notificationService: INotificationService,
  ) {}

  async execute(detectedDevices: Device[]): Promise<void> {
    for (const detected of detectedDevices) {
      const existing = await this.deviceRepository.findByMacAddress(detected.macAddress);

      if (!existing) {
        // New device, completely unknown
        await this.deviceRepository.saveDevice(detected);
        await this.triggerAlert(detected, 'CRITICAL', 'New unknown device detected on the network.');
      } else if (!existing.isKnown) {
        // Device exists but is marked as unknown (maybe re-connected)
        // We'll update lastSeen (simplified here)
        const updatedDevice = existing.copy({ lastSeen: new Date() });
        await this.deviceRepository.saveDevice(updatedDevice);
        await this.triggerAlert(updatedDevice, 'WARNING', 'Previously unknown device reconnected.');
      } else {
        // Known device, just update last seen
        const updatedDevice = existing.copy({ lastSeen: new Date() });
        await this.deviceRepository.saveDevice(updatedDevice);
      }
    }
  }

  private async triggerAlert(device: Device, level: 'INFO' | 'WARNING' | 'CRITICAL', message: string): Promise<void> {
    await this.notificationService.sendAlert(
      level === 'CRITICAL' ? 'Security Alert' : 'Network Notification',
      message,
      { deviceId: device.id, macAddress: device.macAddress, level }
    );
  }
}
