import { createServer } from 'http';
import { GetPresenceUseCase } from './domain/usecases/get-presence.usecase';
import { RecordSightingsUseCase } from './domain/usecases/record-sightings.usecase';
import { InMemorySightingRepository } from './infrastructure/database/in-memory-sighting.repository';
import { InMemoryDeviceRepository } from './infrastructure/database/in-memory-device.repository';
import { createHttpListener } from './presentation/http/http-adapter';
import { RequestHandler } from './presentation/http/request-handler';

const apiKey = process.env.API_KEY;
if (!apiKey || apiKey.length < 16) {
  console.error('API_KEY env var is required (min 16 chars).');
  process.exit(1);
}

const sightings = new InMemorySightingRepository();
const devices = new InMemoryDeviceRepository(process.env.KNOWN_MACS);
const handler = new RequestHandler(apiKey, new RecordSightingsUseCase(sightings), new GetPresenceUseCase(sightings, devices));

const port = Number(process.env.PORT ?? 3000);
createServer(createHttpListener(handler)).listen(port, () => console.log(`presence backend listening on :${port}`));
