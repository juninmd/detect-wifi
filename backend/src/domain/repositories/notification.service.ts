import { NetworkAlert } from '../entities/network-alert.entity';

export interface INotificationService {
  sendAlert(alert: NetworkAlert): Promise<void>;
}
