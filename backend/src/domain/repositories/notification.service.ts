export interface INotificationService {
  sendAlert(title: string, body: string, data?: any): Promise<void>;
}
