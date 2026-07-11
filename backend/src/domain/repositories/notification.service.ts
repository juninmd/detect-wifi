export interface INotificationService {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  sendAlert(title: string, body: string, data?: any): Promise<void>;
}
