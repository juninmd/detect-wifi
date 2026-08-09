import { Device } from '../entities/device.entity';
import { INotificationService } from '../repositories/notification.service';
import { NetworkAlert } from '../entities/network-alert.entity';

export class ApplyDebounceRulesUseCase {
  private readonly DEBOUNCE_TIME_MS = 5 * 60 * 1000; // 5 minutes

  constructor(
    private readonly notificationService: INotificationService,
  ) {}

  async execute(device: Device, eventType: 'CONNECTED' | 'DISCONNECTED'): Promise<void> {
    const timeSinceLastSeen = new Date().getTime() - device.lastSeen.getTime();

    if (eventType === 'CONNECTED') {
      if (timeSinceLastSeen > this.DEBOUNCE_TIME_MS) {
        await this.notificationService.sendAlert(
          new NetworkAlert(
            Math.random().toString(36).substring(7),
            device.id,
            `Device ${device.vendor} (${device.macAddress}) connected.`,
            'INFO',
            new Date(),
          )
        );
      }
    } else if (eventType === 'DISCONNECTED') {
        // Simple logic for disconnected event debounce could also be added
        // In reality, this might be triggered by a cron job checking lastSeen
    }
  }
}
