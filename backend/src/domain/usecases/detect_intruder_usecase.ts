import { Device } from '../entities/device.entity';
import { IDeviceRepository } from '../repositories/device.repository';
import { INotificationService } from '../repositories/notification.service';

export class DetectIntruderUseCase {
  constructor(
    private readonly deviceRepository: IDeviceRepository,
    private readonly notificationService: INotificationService,
  ) {}

  async execute(activeDevices: Device[]): Promise<void> {
    const knownDevices = await this.deviceRepository.getAllDevices();
    const currentTime = new Date().getTime();

    // Regra: 5 minutos de janela para considerar "queda rápida" (Debounce)
    const DEBOUNCE_WINDOW_MS = 5 * 60 * 1000;

    for (const active of activeDevices) {
      const known = knownDevices.find((d) => d.macAddress === active.macAddress);

      if (!known) {
        // Novo dispositivo não listado = INTRUSO (Notifica e salva como não aprovado)
        await this.deviceRepository.saveDevice(
          new Device(
            active.id, active.macAddress, active.ipAddress, active.vendor,
            active.isKnown, false, active.category, new Date()
          )
        );

        await this.notificationService.sendAlert(
          'ALERTA: Novo Dispositivo Desconhecido',
          `Um dispositivo não reconhecido (${active.vendor || 'Desconhecido'}) conectou na sua rede.`,
          { macAddress: active.macAddress }
        );
      } else if (!known.isApproved) {
         // Dispositivo já visto, mas ainda não aprovado: atualiza timestamp e avisa novamente se passou janela
         const timeSinceLastSeen = currentTime - new Date(known.lastSeen).getTime();

         if (timeSinceLastSeen > DEBOUNCE_WINDOW_MS) {
            await this.notificationService.sendAlert(
              'Lembrete: Dispositivo Suspeito na Rede',
              `O dispositivo ${known.vendor || known.macAddress} voltou a se conectar.`,
              { macAddress: known.macAddress }
            );
         }

         await this.deviceRepository.updateLastSeen(known.macAddress, new Date());
      } else {
        // Dispositivo conhecido e aprovado: só atualiza a hora da última vez visto
        await this.deviceRepository.updateLastSeen(known.macAddress, new Date());
      }
    }
  }
}
