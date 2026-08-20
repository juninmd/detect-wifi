export interface INotificationService {
  sendAlert(title: string, message: string, metadata?: Record<string, unknown>): Promise<void>;
}
