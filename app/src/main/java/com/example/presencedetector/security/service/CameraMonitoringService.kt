package com.example.presencedetector.security.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.presencedetector.MainActivity
import com.example.presencedetector.R
import com.example.presencedetector.security.detection.PersonDetectionAnalyzer
import com.example.presencedetector.security.model.CameraChannel
import com.example.presencedetector.security.model.DetectionSettings
import com.example.presencedetector.security.notification.SecurityNotificationManager
import kotlinx.coroutines.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

/**
 * Serviço em foreground para monitoramento contínuo de câmeras.
 *
 * Este serviço:
 * 1. Conecta-se aos streams RTSP das câmeras configuradas
 * 2. Extrai frames usando LibVLC e ImageReader
 * 3. Processa frames com ML Kit para detectar pessoas
 * 4. Dispara notificações quando detecção é confirmada
 *
 * O serviço roda em foreground para evitar ser morto pelo sistema.
 */
class CameraMonitoringService : Service() {

  companion object {
    private const val TAG = "CameraMonitoringService"
    private const val FOREGROUND_CHANNEL_ID = "camera_monitoring"
    private const val FOREGROUND_NOTIFICATION_ID = 1

    // Actions para controle do serviço
    const val ACTION_START = "com.example.presencedetector.security.START_MONITORING"
    const val ACTION_STOP = "com.example.presencedetector.security.STOP_MONITORING"
    const val ACTION_CAMERA_PRESENCE = "com.example.presencedetector.security.CAMERA_PRESENCE"
    const val EXTRA_CAMERA_NAME = "camera_name"

    /** Inicia o serviço de monitoramento. */
    fun start(context: Context) {
      val intent =
        Intent(context, CameraMonitoringService::class.java).apply { action = ACTION_START }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    /** Para o serviço de monitoramento. */
    fun stop(context: Context) {
      val intent =
        Intent(context, CameraMonitoringService::class.java).apply { action = ACTION_STOP }
      context.startService(intent)
    }
  }

  private var libVLC: LibVLC? = null
  private val mediaPlayers = mutableMapOf<Int, MediaPlayer>()
  private val analyzers = mutableMapOf<Int, PersonDetectionAnalyzer>()
  private val imageReaders = mutableMapOf<Int, ImageReader>()

  private lateinit var notificationManager: SecurityNotificationManager
  private lateinit var settings: DetectionSettings

  private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private var backgroundHandlerThread: HandlerThread? = null
  private var backgroundHandler: Handler? = null

  // Resolução fixa para análise (balanço entre qualidade e performance)
  private val CAPTURE_WIDTH = 640
  private val CAPTURE_HEIGHT = 480

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "Serviço criado")

    createForegroundNotificationChannel()
    notificationManager = SecurityNotificationManager(this)

    // Inicializa thread para processamento de imagens
    backgroundHandlerThread = HandlerThread("ImageProcessingThread").apply { start() }
    backgroundHandler = Handler(backgroundHandlerThread!!.looper)

    // Inicializa LibVLC com opções otimizadas para extração de frames
    val vlcOptions =
      arrayListOf(
        "--no-audio", // Sem áudio (economia de recursos)
        "--rtsp-tcp", // Força TCP para RTSP (mais estável)
        "--network-caching=1000", // Cache de rede em ms
        "--no-video-title-show", // Sem título de vídeo
        "--no-stats", // Sem estatísticas
        "--no-sub-autodetect-file", // Sem legendas
        "--no-spu", // Sem subtítulos
        "-vvv" // Log verbose para debug
      )
    libVLC = LibVLC(this, vlcOptions)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> startMonitoring()
      ACTION_STOP -> stopMonitoring()
    }
    return START_STICKY
  }

  /** Inicia monitoramento de todas as câmeras habilitadas. */
  private fun startMonitoring() {
    Log.i(TAG, "Iniciando monitoramento...")

    // Exibe notificação de foreground
    val notification = createForegroundNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        FOREGROUND_NOTIFICATION_ID,
        notification,
        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
      )
    } else {
      startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    // Carrega configurações
    settings = DetectionSettings.load(this)

    if (settings.channels.isEmpty()) {
      Log.w(TAG, "Nenhuma câmera configurada. Parando serviço.")
      stopSelf()
      return
    }

    // Inicia monitoramento para cada câmera habilitada
    settings.channels
      .filter { it.id in settings.enabledChannelIds }
      .forEach { channel -> startChannelMonitoring(channel) }
  }

  /** Inicia monitoramento de um canal específico. */
  private fun startChannelMonitoring(channel: CameraChannel) {
    Log.d(TAG, "Iniciando monitoramento do canal: ${channel.name}")

    val vlc = libVLC ?: return

    // Cria analisador de detecção para este canal
    val analyzer =
      PersonDetectionAnalyzer(
        channel = channel,
        detectionThresholdMs = settings.detectionThresholdMs,
        gracePeriodMs = settings.gracePeriodMs,
        cooldownMs = settings.notificationCooldownMs,
        onPersonConfirmed = { confirmedChannel, snapshot ->
          // Callback quando pessoa é confirmada (com frame capturado)
          onPersonConfirmed(confirmedChannel, snapshot)
        }
      )
    analyzers[channel.id] = analyzer

    // Configura ImageReader para captura de frames
    val imageReader =
      ImageReader.newInstance(CAPTURE_WIDTH, CAPTURE_HEIGHT, PixelFormat.RGBA_8888, 2)
    imageReader.setOnImageAvailableListener(
      { reader ->
        val image = reader.acquireLatestImage()
        if (image != null) {
          try {
            val bitmap = imageToBitmap(image)
            if (bitmap != null) {
              analyzer.analyzeFrame(bitmap)
              bitmap.recycle() // Recycle after analysis (analyzer makes a copy if needed)
            }
          } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ${e.message}")
          } finally {
            image.close()
          }
        }
      },
      backgroundHandler
    )
    imageReaders[channel.id] = imageReader

    // Cria media player para o stream RTSP
    val media =
      Media(vlc, channel.rtspUrlSubstream).apply {
        setHWDecoderEnabled(true, false)
        addOption(":network-caching=1000")
        addOption(":rtsp-tcp")
      }

    val mediaPlayer =
      MediaPlayer(vlc).apply {
        setMedia(media)
        // Configura Surface do ImageReader como saída de vídeo
        getVLCVout().setVideoSurface(imageReader.surface, null)
        getVLCVout().setWindowSize(CAPTURE_WIDTH, CAPTURE_HEIGHT)
        getVLCVout().attachViews()
      }

    mediaPlayers[channel.id] = mediaPlayer

    // Inicia playback
    mediaPlayer.play()
  }

  /** Converte Image (RGBA_8888) para Bitmap. */
  private fun imageToBitmap(image: Image): Bitmap? {
    val planes = image.planes
    val buffer = planes[0].buffer
    val pixelStride = planes[0].pixelStride
    val rowStride = planes[0].rowStride
    val rowPadding = rowStride - pixelStride * image.width

    // Create bitmap
    val bitmap =
      Bitmap.createBitmap(
        image.width + rowPadding / pixelStride,
        image.height,
        Bitmap.Config.ARGB_8888
      )
    bitmap.copyPixelsFromBuffer(buffer)

    // Se houver padding, recorta para o tamanho real
    if (rowPadding > 0) {
      return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    return bitmap
  }

  /**
   * Callback quando uma pessoa é confirmada após o threshold.
   *
   * @param channel Canal da câmera que detectou
   * @param snapshot Frame capturado no momento da detecção (pode ser null)
   */
  private fun onPersonConfirmed(channel: CameraChannel, snapshot: Bitmap?) {
    Log.w(TAG, "🚨 ALERTA: Pessoa confirmada em ${channel.name}!")

    // Dispara notificação detalhada com imagem do snapshot
    notificationManager.showDetectionNotification(channel, snapshot)

    // Envia broadcast para DetectionBackgroundService integrar com PresenceDetectionManager
    val intent = Intent(ACTION_CAMERA_PRESENCE).apply { putExtra(EXTRA_CAMERA_NAME, channel.name) }
    sendBroadcast(intent)

    // Recicla bitmap após uso na notificação
    snapshot?.recycle()
  }

  /** Para o monitoramento de todas as câmeras. */
  private fun stopMonitoring() {
    Log.i(TAG, "Parando monitoramento...")

    // Cancela todos os jobs
    serviceScope.cancel()

    // Para todos os media players
    mediaPlayers.values.forEach { player ->
      player.stop()
      player.getVLCVout().detachViews()
      player.release()
    }
    mediaPlayers.clear()

    // Fecha ImageReaders
    imageReaders.values.forEach { it.close() }
    imageReaders.clear()

    // Fecha todos os analyzers
    analyzers.values.forEach { it.close() }
    analyzers.clear()

    // Para o serviço
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d(TAG, "Serviço destruído")

    serviceScope.cancel()
    backgroundHandlerThread?.quitSafely()
    libVLC?.release()
    libVLC = null
  }

  /** Cria canal de notificação do foreground service. */
  private fun createForegroundNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            "Monitoramento de Câmeras",
            NotificationManager.IMPORTANCE_LOW
          )
          .apply {
            description = "Notificação do serviço de monitoramento em segundo plano"
            setShowBadge(false)
          }

      val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  /** Cria notificação do foreground service. */
  private fun createForegroundNotification(): Notification {
    val intent = Intent(this, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
      .setContentTitle("Monitoramento ativo")
      .setContentText("Monitorando câmeras de segurança")
      .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Criar ícone próprio
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .build()
  }
}
