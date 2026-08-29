import { ScanNetworkUseCase } from '../scan-network.usecase';

describe('ScanNetworkUseCase', () => {
  let useCase: ScanNetworkUseCase;

  beforeEach(() => {
    useCase = new ScanNetworkUseCase();
  });

  it('should return an empty array initially', async () => {
    const result = await useCase.execute();
    expect(Array.isArray(result)).toBe(true);
    expect(result.length).toBe(0);
  });
});
