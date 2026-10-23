import { timingSafeEqual } from 'crypto';
import { GetPresenceUseCase } from '../../domain/usecases/get-presence.usecase';
import { RecordSightingsUseCase } from '../../domain/usecases/record-sightings.usecase';

export interface HttpRequest {
  method: string;
  path: string;
  query: URLSearchParams;
  headers: Record<string, string | string[] | undefined>;
  body: unknown;
}

export interface HttpResponse {
  status: number;
  body: unknown;
}

const DEFAULT_WINDOW_SECONDS = 300;
const MAX_WINDOW_SECONDS = 3600;

/** Framework-agnostic routing: sensors POST sightings, clients GET presence. */
export class RequestHandler {
  constructor(
    private readonly apiKey: string,
    private readonly recordSightings: RecordSightingsUseCase,
    private readonly getPresence: GetPresenceUseCase,
  ) {}

  async handle(req: HttpRequest): Promise<HttpResponse> {
    if (req.method === 'GET' && req.path === '/health') return { status: 200, body: { status: 'ok' } };
    if (!this.isAuthorized(req.headers['x-api-key'])) return { status: 401, body: { error: 'Unauthorized' } };

    if (req.method === 'POST' && req.path === '/sightings') return this.postSightings(req.body);
    if (req.method === 'GET' && req.path === '/presence') return this.getPresenceSnapshot(req.query);
    return { status: 404, body: { error: 'Not found' } };
  }

  private async postSightings(body: unknown): Promise<HttpResponse> {
    const { sensorId, sightings } = (body ?? {}) as { sensorId?: string; sightings?: unknown };
    if (!Array.isArray(sightings)) return { status: 400, body: { error: 'sightings must be an array' } };
    try {
      const accepted = await this.recordSightings.execute(sensorId, sightings);
      return { status: 202, body: { accepted } };
    } catch (error) {
      return { status: 400, body: { error: (error as Error).message } };
    }
  }

  private async getPresenceSnapshot(query: URLSearchParams): Promise<HttpResponse> {
    const raw = query.get('window');
    const windowSeconds = raw === null ? DEFAULT_WINDOW_SECONDS : Number(raw);
    if (!Number.isInteger(windowSeconds) || windowSeconds < 1 || windowSeconds > MAX_WINDOW_SECONDS) {
      return { status: 400, body: { error: `window must be an integer between 1 and ${MAX_WINDOW_SECONDS}` } };
    }
    return { status: 200, body: await this.getPresence.execute(windowSeconds) };
  }

  private isAuthorized(provided: string | string[] | undefined): boolean {
    if (typeof provided !== 'string') return false;
    const a = Buffer.from(provided);
    const b = Buffer.from(this.apiKey);
    return a.length === b.length && timingSafeEqual(a, b);
  }
}
