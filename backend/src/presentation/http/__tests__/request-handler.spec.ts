import { RequestHandler, HttpRequest } from '../request-handler';
import { RecordSightingsUseCase } from '../../../domain/usecases/record-sightings.usecase';
import { GetPresenceUseCase } from '../../../domain/usecases/get-presence.usecase';

describe('RequestHandler', () => {
  const KEY = 'secret-key-123456';
  let record: jest.Mocked<Pick<RecordSightingsUseCase, 'execute'>>;
  let presence: jest.Mocked<Pick<GetPresenceUseCase, 'execute'>>;
  let handler: RequestHandler;

  const req = (over: Partial<HttpRequest>): HttpRequest => ({
    method: 'GET',
    path: '/presence',
    query: new URLSearchParams(),
    headers: { 'x-api-key': KEY },
    body: undefined,
    ...over,
  });

  beforeEach(() => {
    record = { execute: jest.fn() };
    presence = { execute: jest.fn() };
    handler = new RequestHandler(KEY, record as never, presence as never);
  });

  it('serves /health without auth', async () => {
    expect(await handler.handle(req({ path: '/health', headers: {} }))).toEqual({ status: 200, body: { status: 'ok' } });
  });

  it.each([[{}], [{ 'x-api-key': 'wrong' }], [{ 'x-api-key': ['a'] }], [{ 'x-api-key': 'x'.repeat(KEY.length) }]])(
    'rejects bad credentials %p',
    async (headers) => {
      expect((await handler.handle(req({ headers }))).status).toBe(401);
    },
  );

  it('returns 404 for unknown routes', async () => {
    expect((await handler.handle(req({ path: '/nope' }))).status).toBe(404);
  });

  it('records sightings', async () => {
    record.execute.mockResolvedValue(2);
    const body = { sensorId: 'esp-1', sightings: [{}, {}] };
    expect(await handler.handle(req({ method: 'POST', path: '/sightings', body }))).toEqual({
      status: 202,
      body: { accepted: 2 },
    });
    expect(record.execute).toHaveBeenCalledWith('esp-1', body.sightings);
  });

  it('validates the sightings payload and maps use case errors to 400', async () => {
    expect((await handler.handle(req({ method: 'POST', path: '/sightings' }))).status).toBe(400);
    record.execute.mockRejectedValue(new Error('Invalid sensorId'));
    const res = await handler.handle(req({ method: 'POST', path: '/sightings', body: { sightings: [] } }));
    expect(res).toEqual({ status: 400, body: { error: 'Invalid sensorId' } });
  });

  it('returns presence using default and custom windows', async () => {
    presence.execute.mockResolvedValue({ windowSeconds: 300, known: [], unknown: [], anonymousCount: 0 });
    await handler.handle(req({}));
    expect(presence.execute).toHaveBeenLastCalledWith(300);
    const res = await handler.handle(req({ query: new URLSearchParams('window=60') }));
    expect(res.status).toBe(200);
    expect(presence.execute).toHaveBeenLastCalledWith(60);
  });

  it.each(['abc', '0', '3601', '1.5'])('rejects invalid window %s', async (window) => {
    expect((await handler.handle(req({ query: new URLSearchParams({ window }) }))).status).toBe(400);
  });
});
