import { IncomingMessage, ServerResponse } from 'http';
import { RequestHandler } from './request-handler';

const MAX_BODY_BYTES = 64 * 1024;

function send(res: ServerResponse, status: number, body: unknown): void {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
}

async function readBody(req: IncomingMessage): Promise<Buffer> {
  const chunks: Buffer[] = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > MAX_BODY_BYTES) throw new Error('Payload too large');
    chunks.push(Buffer.from(chunk));
  }
  return Buffer.concat(chunks);
}

/** Bridges Node's http server to the framework-agnostic RequestHandler. */
export function createHttpListener(handler: RequestHandler) {
  return async (req: IncomingMessage, res: ServerResponse): Promise<void> => {
    let body: unknown;
    try {
      const raw = await readBody(req);
      body = raw.length > 0 ? JSON.parse(raw.toString('utf8')) : undefined;
    } catch (error) {
      const tooLarge = (error as Error).message === 'Payload too large';
      return send(res, tooLarge ? 413 : 400, { error: tooLarge ? 'Payload too large' : 'Invalid JSON' });
    }

    const url = new URL(req.url ?? '/', 'http://localhost');
    try {
      const result = await handler.handle({
        method: req.method ?? 'GET',
        path: url.pathname,
        query: url.searchParams,
        headers: req.headers,
        body,
      });
      send(res, result.status, result.body);
    } catch (error) {
      console.error('Unhandled request error', error);
      send(res, 500, { error: 'Internal error' });
    }
  };
}
