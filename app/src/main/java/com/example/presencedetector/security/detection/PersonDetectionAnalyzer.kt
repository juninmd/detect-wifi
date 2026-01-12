package com.example.presencedetector.security.detection

import android.graphics.Bitmap
import android.util.Log
import com.example.presencedetector.security.model.CameraChannel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.objects.DetectedObject

/**
 * Estado da máquina de estados do debounce de detecção.
 */
enum class DetectionState {
    /** Nenhuma pessoa detectada. Timer zerado. */
    IDLE,
    
    /** Pessoa detectada, contando tempo até threshold. */
    DETECTING,
    
    /** Pessoa sumiu temporariamente, dentro do período de graça. */
    GRACE_PERIOD,
    
    /** Alerta foi disparado, em cooldown. */
    COOLDOWN
}

/**
 * Analisador de frames para detecção de pessoas com lógica de debounce.
 * 
 * ════════════════════════════════════════════════════════════════════
 * 🎯 LÓGICA DE DEBOUNCE - COMO FUNCIONA
 * ════════════════════════════════════════════════════════════════════
 * 
 * O objetivo é evitar falsos positivos causados por:
 * - Sombras ou reflexos detectados erroneamente como pessoas
 * - Glitches no stream RTSP que causam frames corrompidos
 * - Pessoas passando rapidamente (não são uma ameaça real)
 * 
 * A máquina de estados funciona assim:
 * 
 * 1. IDLE (Ocioso)
 *    - Aguardando detecção de pessoa
 *    - Ao detectar pessoa → Vai para DETECTING
 * 
 * 2. DETECTING (Detectando)
 *    - Timer começa a contar
 *    - Se pessoa permanece por X segundos → Dispara alerta → COOLDOWN
 *    - Se pessoa some → Vai para GRACE_PERIOD
 * 
 * 3. GRACE_PERIOD (Período de Graça)
 *    - Tolerância para glitches de stream (padrão: 1 segundo)
 *    - Se pessoa volta dentro do período → Volta para DETECTING (timer continua!)
 *    - Se período expira sem pessoa → Volta para IDLE (timer zera)
 * 
 * 4. COOLDOWN (Resfriamento)
 *    - Evita spam de notificações
 *    - Aguarda X segundos antes de permitir novo alerta
 *    - Após cooldown → Volta para IDLE
 * 
 * ════════════════════════════════════════════════════════════════════
 * 
 * @param channel Canal de câmera sendo monitorado
 * @param detectionThresholdMs Tempo em ms que pessoa precisa permanecer (padrão 5000ms)
 * @param gracePeriodMs Tolerância para glitches (padrão 1000ms)
 * @param cooldownMs Intervalo entre alertas (padrão 60000ms)
 * @param onPersonConfirmed Callback quando pessoa é confirmada após threshold, inclui o frame capturado
 */
class PersonDetectionAnalyzer(
    private val channel: CameraChannel,
    private val detectionThresholdMs: Long = 5000L,
    private val gracePeriodMs: Long = 1000L,
    private val cooldownMs: Long = 60000L,
    private val onPersonConfirmed: (CameraChannel, Bitmap?) -> Unit
) {
    companion object {
        private const val TAG = "PersonDetectionAnalyzer"
        
        // Confiança mínima para considerar como pessoa (0.0 a 1.0)
        private const val MIN_CONFIDENCE = 0.5f
    }

    // ML Kit Object Detector configurado para detecção de múltiplos objetos
    private val objectDetector: ObjectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE) // Otimizado para vídeo
            .enableMultipleObjects() // Detectar várias pessoas
            .enableClassification() // Classificar tipo de objeto
            .build()
    )

    // Estado atual da máquina de estados
    private var currentState = DetectionState.IDLE

    // Timestamps para controle de tempo
    private var detectionStartTime: Long = 0L
    private var lastPersonSeenTime: Long = 0L
    private var cooldownStartTime: Long = 0L

    // Flag para evitar processamento duplicado
    private var isProcessing = false

    // Último frame onde pessoa foi detectada (para snapshot na notificação)
    private var lastDetectionBitmap: Bitmap? = null

    /**
     * Analisa um frame de vídeo para detectar pessoas.
     * 
     * Esta função deve ser chamada para cada frame extraído do stream RTSP.
     * Recomenda-se processar 5-10 frames por segundo para bom equilíbrio
     * entre precisão e consumo de CPU.
     * 
     * @param bitmap Frame a ser analisado
     */
    fun analyzeFrame(bitmap: Bitmap) {
        // Evita processamento concorrente (descarta frames se ainda processando)
        // Isso é equivalente a ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        if (isProcessing) {
            Log.v(TAG, "Descartando frame - processamento anterior em andamento")
            return
        }

        isProcessing = true
        val currentTime = System.currentTimeMillis()

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val personDetected = hasPersonDetected(detectedObjects)
                
                // Salva cópia do bitmap quando pessoa é detectada
                // para usar na notificação detalhada
                if (personDetected && currentState == DetectionState.DETECTING) {
                    // Cria cópia para não perder referência quando bitmap original for reciclado
                    lastDetectionBitmap?.recycle()
                    lastDetectionBitmap = bitmap.copy(bitmap.config, false)
                }
                
                updateState(personDetected, currentTime)
                isProcessing = false
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro na detecção: ${e.message}")
                // Em caso de erro, tratamos como se não houvesse detecção
                // mas mantemos grace period para não zerar por erro
                updateState(personDetected = false, currentTime = currentTime, isError = true)
                isProcessing = false
            }
    }

    /**
     * Verifica se algum dos objetos detectados é uma pessoa.
     */
    private fun hasPersonDetected(objects: List<DetectedObject>): Boolean {
        for (obj in objects) {
            for (label in obj.labels) {
                // ML Kit retorna labels como "Person", "Animal", etc.
                // Também verifica índice 0 que geralmente é pessoa
                if (label.text.equals("Person", ignoreCase = true) ||
                    (label.index == 0 && label.confidence >= MIN_CONFIDENCE)) {
                    Log.d(TAG, "Pessoa detectada! Confiança: ${label.confidence}")
                    return true
                }
            }
        }
        return false
    }

    /**
     * Atualiza a máquina de estados baseado na detecção atual.
     * 
     * @param personDetected Se uma pessoa foi detectada neste frame
     * @param currentTime Timestamp atual em milissegundos
     * @param isError Se houve erro no processamento
     */
    private fun updateState(personDetected: Boolean, currentTime: Long, isError: Boolean = false) {
        when (currentState) {
            DetectionState.IDLE -> {
                if (personDetected) {
                    // Pessoa detectada! Inicia contagem
                    currentState = DetectionState.DETECTING
                    detectionStartTime = currentTime
                    lastPersonSeenTime = currentTime
                    Log.i(TAG, "[${channel.name}] Pessoa detectada - iniciando timer")
                }
            }

            DetectionState.DETECTING -> {
                if (personDetected) {
                    lastPersonSeenTime = currentTime
                    
                    // Verifica se atingiu o threshold
                    val elapsedTime = currentTime - detectionStartTime
                    if (elapsedTime >= detectionThresholdMs) {
                        // ALERTA! Pessoa confirmada após threshold
                        Log.w(TAG, "[${channel.name}] 🚨 ALERTA! Pessoa confirmada por ${elapsedTime}ms")
                        
                        // Passa o frame capturado junto com o callback
                        onPersonConfirmed(channel, lastDetectionBitmap)
                        
                        // Limpa bitmap após uso (notificação já copiou se precisar)
                        lastDetectionBitmap = null
                        
                        // Entra em cooldown
                        currentState = DetectionState.COOLDOWN
                        cooldownStartTime = currentTime
                    } else {
                        Log.d(TAG, "[${channel.name}] Detectando... ${elapsedTime}ms / ${detectionThresholdMs}ms")
                    }
                } else {
                    // Pessoa sumiu - entra em período de graça
                    // (exceto se for erro, onde já estamos em grace implícito)
                    if (!isError) {
                        currentState = DetectionState.GRACE_PERIOD
                        Log.d(TAG, "[${channel.name}] Pessoa sumiu - entrando em grace period")
                    }
                }
            }

            DetectionState.GRACE_PERIOD -> {
                if (personDetected) {
                    // Pessoa voltou! Retorna a DETECTING sem resetar timer
                    currentState = DetectionState.DETECTING
                    lastPersonSeenTime = currentTime
                    Log.d(TAG, "[${channel.name}] Pessoa voltou - continuando detecção")
                } else {
                    // Verifica se grace period expirou
                    val timeSinceLastSeen = currentTime - lastPersonSeenTime
                    if (timeSinceLastSeen >= gracePeriodMs) {
                        // Grace period expirou - reseta tudo
                        currentState = DetectionState.IDLE
                        detectionStartTime = 0L
                        Log.d(TAG, "[${channel.name}] Grace period expirou - resetando")
                    }
                }
            }

            DetectionState.COOLDOWN -> {
                // Verifica se cooldown expirou
                val cooldownElapsed = currentTime - cooldownStartTime
                if (cooldownElapsed >= cooldownMs) {
                    currentState = DetectionState.IDLE
                    Log.d(TAG, "[${channel.name}] Cooldown expirou - pronto para nova detecção")
                }
            }
        }
    }

    /**
     * Reseta o analisador para estado inicial.
     */
    fun reset() {
        currentState = DetectionState.IDLE
        detectionStartTime = 0L
        lastPersonSeenTime = 0L
        cooldownStartTime = 0L
        isProcessing = false
        Log.d(TAG, "[${channel.name}] Analisador resetado")
    }

    /**
     * Libera recursos do detector.
     */
    fun close() {
        objectDetector.close()
        lastDetectionBitmap?.recycle()
        lastDetectionBitmap = null
        Log.d(TAG, "[${channel.name}] Detector fechado")
    }

    /**
     * Retorna informações de diagnóstico sobre o estado atual.
     */
    fun getDebugInfo(): String {
        return buildString {
            append("Canal: ${channel.name}\n")
            append("Estado: $currentState\n")
            if (currentState == DetectionState.DETECTING) {
                val elapsed = System.currentTimeMillis() - detectionStartTime
                append("Tempo detectando: ${elapsed}ms / ${detectionThresholdMs}ms\n")
            }
        }
    }
}
