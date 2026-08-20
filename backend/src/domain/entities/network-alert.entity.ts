export class NetworkAlert {
  constructor(
    public readonly id: string,
    public readonly deviceId: string,
    public readonly title: string,
    public readonly message: string,
    public readonly metadata: any,
    public readonly level: 'INFO' | 'WARNING' | 'CRITICAL',
    public readonly timestamp: Date,
  ) {}
}
