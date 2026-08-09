export class NetworkAlert {
  constructor(
    public readonly id: string,
    public readonly deviceId: string,
    public readonly message: string,
    public readonly level: 'INFO' | 'WARNING' | 'CRITICAL',
    public readonly timestamp: Date,
  ) {}
}
