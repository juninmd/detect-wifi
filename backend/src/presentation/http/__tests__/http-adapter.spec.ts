import { IncomingMessage, ServerResponse } from 'http';
import { Readable } from 'stream';
import { createHttpListener } from '../http-adapter';
import { RequestHandler } from '../request-handler';

describe('createHttpListener', () => {
  let handler: jest.Mocked<Pick<RequestHandler, 'handle'>>;
  let res: { writeHead: jest.Mock; end: jest.Mock };
  let listener: ReturnType<typeof createHttpListener>;

  const request = (chunks: string[], extra: Partial<IncomingMessage> = {}): IncomingMessage =>
    Object.assign(Readable.from(chunks.map((c) => Buffer.from(c))), {
      method: 'POST',
      url: '/sightings?window=5',
      headers: { 'x-api-key': 'k' },
      ...extra,
    }) as unknown as IncomingMessage;

  const run = (req: IncomingMessage) => listener(req, res as unknown as ServerResponse);

  beforeEach(() => {
    handler = { handle: jest.fn().mockResolvedValue({ status: 202, body: { ok: true } }) };
    res = { writeHead: jest.fn(), end: jest.fn() };
    listener = createHttpListener(handler as never);
  });

  it('parses JSON body, query and forwards the response', async () => {
    await run(request(['{"a":', '1}']));

    const forwarded = handler.handle.mock.calls[0][0];
    expect(forwarded).toMatchObject({ method: 'POST', path: '/sightings', body: { a: 1 }, headers: { 'x-api-key': 'k' } });
    expect(forwarded.query.get('window')).toBe('5');
    expect(res.writeHead).toHaveBeenCalledWith(202, { 'Content-Type': 'application/json' });
    expect(res.end).toHaveBeenCalledWith('{"ok":true}');
  });

  it('handles empty bodies and missing method/url', async () => {
    await run(request([], { method: undefined, url: undefined }));
    expect(handler.handle).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET', path: '/', body: undefined }));
  });

  it('answers 400 for invalid JSON', async () => {
    await run(request(['{bad']));
    expect(res.writeHead).toHaveBeenCalledWith(400, expect.anything());
    expect(handler.handle).not.toHaveBeenCalled();
  });

  it('answers 413 for oversized bodies', async () => {
    await run(request(['x'.repeat(70 * 1024)]));
    expect(res.writeHead).toHaveBeenCalledWith(413, expect.anything());
  });
});
