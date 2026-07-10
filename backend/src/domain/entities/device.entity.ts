export interface Device {
  ipAddress: string;
  macAddress: string;
  vendor?: string;
  isApproved: boolean;
  lastSeen: Date;
}
