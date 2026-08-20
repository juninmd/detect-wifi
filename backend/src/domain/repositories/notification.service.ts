import { NetworkAlert } from '../entities/network-alert.entity';

export interface INotificationService {
  sendAlert(title: string, message: string, metadata?: any): Promise<void>;
}
