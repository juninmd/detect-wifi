package com.example.presencedetector.security.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.presencedetector.databinding.ActivitySecuritySettingsBinding
import com.example.presencedetector.security.model.CameraChannel
import com.example.presencedetector.security.model.DetectionSettings
import com.example.presencedetector.security.service.CameraMonitoringService
import kotlinx.coroutines.*

/**
 * Activity para configuração do monitoramento de segurança.
 * 
 * Permite configurar:
 * - Conexão com DVR (IP, porta, credenciais)
 * - Parâmetros de detecção (tempo, grace period)
 * - Gerenciamento de canais de câmera
 */
class SecuritySettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecuritySettingsBinding
    private lateinit var settings: DetectionSettings

    // Launcher para permissão de notificação (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateMonitoringState(true)
        } else {
            Toast.makeText(this, 
                "Permissão de notificação necessária para alertas", 
                Toast.LENGTH_LONG).show()
            binding.switchMonitoring.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySecuritySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.apply {
            title = "Configurações de Segurança"
            setDisplayHomeAsUpEnabled(true)
        }
        
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        settings = DetectionSettings.load(this)
        
        // Preenche campos de conexão
        binding.etDvrHost.setText(settings.dvrHost)
        binding.etDvrPort.setText(settings.dvrPort.toString())
        binding.etUsername.setText(settings.username)
        binding.etPassword.setText(settings.password)
        
        // Preenche parâmetros de detecção
        binding.sliderDetectionTime.value = settings.detectionThresholdSeconds.toFloat()
        binding.tvDetectionTimeValue.text = "${settings.detectionThresholdSeconds}s"
        
        binding.sliderGracePeriod.value = settings.gracePeriodSeconds.toFloat()
        binding.tvGracePeriodValue.text = "${settings.gracePeriodSeconds}s"
        
        binding.sliderCooldown.value = settings.notificationCooldownSeconds.toFloat()
        binding.tvCooldownValue.text = "${settings.notificationCooldownSeconds}s"
        
        // Estado do monitoramento
        binding.switchMonitoring.isChecked = settings.monitoringEnabled
        
        // Atualiza lista de canais
        updateChannelsList()
    }

    private fun setupListeners() {
        // Slider de tempo de detecção
        binding.sliderDetectionTime.addOnChangeListener { _, value, _ ->
            binding.tvDetectionTimeValue.text = "${value.toInt()}s"
        }
        
        // Slider de grace period
        binding.sliderGracePeriod.addOnChangeListener { _, value, _ ->
            binding.tvGracePeriodValue.text = "${value.toInt()}s"
        }
        
        // Slider de cooldown
        binding.sliderCooldown.addOnChangeListener { _, value, _ ->
            binding.tvCooldownValue.text = "${value.toInt()}s"
        }
        
        // Switch de monitoramento
        binding.switchMonitoring.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkNotificationPermission()
            } else {
                updateMonitoringState(false)
            }
        }
        
        // Botão testar conexão
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }
        
        // Botão adicionar canal
        binding.btnAddChannel.setOnClickListener {
            showAddChannelDialog()
        }
        
        // Botão auto descobrir
        binding.btnAutoDiscover.setOnClickListener {
            showAutoDiscoverDialog()
        }
        
        // Botão salvar
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED -> {
                    updateMonitoringState(true)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    AlertDialog.Builder(this)
                        .setTitle("Permissão Necessária")
                        .setMessage("O app precisa de permissão para enviar notificações de alerta quando uma pessoa for detectada.")
                        .setPositiveButton("Permitir") { _, _ ->
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancelar") { _, _ ->
                            binding.switchMonitoring.isChecked = false
                        }
                        .show()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            updateMonitoringState(true)
        }
    }

    private fun updateMonitoringState(enabled: Boolean) {
        settings = settings.copy(monitoringEnabled = enabled)
        
        if (enabled) {
            if (settings.channels.isEmpty()) {
                Toast.makeText(this, "Adicione pelo menos um canal antes de ativar", Toast.LENGTH_LONG).show()
                binding.switchMonitoring.isChecked = false
                return
            }
            
            saveSettings()
            CameraMonitoringService.start(this)
            Toast.makeText(this, "Monitoramento iniciado", Toast.LENGTH_SHORT).show()
        } else {
            CameraMonitoringService.stop(this)
            Toast.makeText(this, "Monitoramento parado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testConnection() {
        val host = binding.etDvrHost.text.toString().trim()
        val port = binding.etDvrPort.text.toString().toIntOrNull() ?: 554
        
        if (host.isEmpty()) {
            binding.tilDvrHost.error = "Informe o IP do DVR"
            return
        }
        
        binding.tilDvrHost.error = null
        binding.btnTestConnection.isEnabled = false
        binding.btnTestConnection.text = "Testando..."
        
        // Cria canal temporário para teste
        val testChannel = CameraChannel(
            id = -1,
            name = "Teste",
            host = host,
            port = port,
            username = binding.etUsername.text.toString(),
            password = binding.etPassword.text.toString()
        )
        
        // Teste real de conexão e autenticação via LibVLC
        binding.tilDvrHost.error = null
        binding.btnTestConnection.isEnabled = false
        binding.btnTestConnection.text = "Autenticando..."
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            var libVLC: org.videolan.libvlc.LibVLC? = null
            var media: org.videolan.libvlc.Media? = null
            
            try {
                // Configurações otimizadas para teste rápido
                val options = ArrayList<String>()
                options.add("--rtsp-tcp")
                options.add("--no-audio")
                options.add("--no-video") // Não precisamos decodificar vídeo, apenas conectar
                
                libVLC = org.videolan.libvlc.LibVLC(this@SecuritySettingsActivity, options)
                media = org.videolan.libvlc.Media(libVLC, testChannel.rtspUrl) // Usa URL com senha
                
                // Tenta fazer o parse (conectar e ler headers) com timeout de 5s
                // Flag 4 = FetchNetwork (Force network connection)
                val success = media?.parse(4) ?: false
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (success) {
                        binding.btnTestConnection.text = "Testar Conexão"
                        Toast.makeText(this@SecuritySettingsActivity, "Sucesso! DVR conectado e autenticado.", Toast.LENGTH_LONG).show()
                    } else {
                        binding.btnTestConnection.text = "Testar Conexão"
                        Toast.makeText(this@SecuritySettingsActivity, "Falha na autenticação ou conexão.", Toast.LENGTH_LONG).show()
                    }
                    binding.btnTestConnection.isEnabled = true
                }
                
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    binding.btnTestConnection.isEnabled = true
                    binding.btnTestConnection.text = "Testar Conexão"
                    Toast.makeText(this@SecuritySettingsActivity, "Erro: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                media?.release()
                libVLC?.release()
            }
        }
    }

    private fun showAddChannelDialog() {
        val dialogBinding = android.widget.EditText(this).apply {
            hint = "Nome da câmera (ex: Garagem)"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Adicionar Câmera")
            .setView(dialogBinding)
            .setPositiveButton("Adicionar") { _, _ ->
                val name = dialogBinding.text.toString().trim()
                if (name.isNotEmpty()) {
                    val channelNumber = settings.channels.size + 1
                    settings = settings.addChannel(name, channelNumber)
                    updateChannelsList()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAutoDiscoverDialog() {
        val options = arrayOf("4 Canais", "8 Canais", "16 Canais")
        AlertDialog.Builder(this)
            .setTitle("Auto-Configurar Canais")
            .setItems(options) { _, which ->
                val count = when (which) {
                    0 -> 4
                    1 -> 8
                    2 -> 16
                    else -> 4
                }
                autoAddChannels(count)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun autoAddChannels(count: Int) {
        var added = 0
        for (i in 1..count) {
             val exists = settings.channels.any { it.channel == i }
             if (!exists) {
                 settings = settings.addChannel("Camera $i", i)
                 added++
             }
        }
        updateChannelsList()
        Toast.makeText(this, "$added canais adicionados.", Toast.LENGTH_SHORT).show()
    }

    private fun updateChannelsList() {
        val channelNames = settings.channels.joinToString("\n") { channel ->
            "• ${channel.name} (Canal ${channel.channel})"
        }
        
        binding.tvChannelsList.text = if (channelNames.isEmpty()) {
            "Nenhuma câmera configurada"
        } else {
            channelNames
        }
    }

    private fun saveSettings() {
        // Coleta valores dos campos
        val host = binding.etDvrHost.text.toString().trim()
        val port = binding.etDvrPort.text.toString().toIntOrNull() ?: 554
        val username = binding.etUsername.text.toString()
        val password = binding.etPassword.text.toString()
        val detectionTime = binding.sliderDetectionTime.value.toInt()
        val gracePeriod = binding.sliderGracePeriod.value.toInt()
        val cooldown = binding.sliderCooldown.value.toInt()
        
        // Atualiza settings
        settings = settings.copy(
            dvrHost = host,
            dvrPort = port,
            username = username,
            password = password,
            detectionThresholdSeconds = detectionTime,
            gracePeriodSeconds = gracePeriod,
            notificationCooldownSeconds = cooldown
        ).withUpdatedChannelConnections()
        
        // Salva
        DetectionSettings.save(this, settings)
        
        Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show()
        
        // Reinicia serviço se estiver ativo
        if (settings.monitoringEnabled) {
            CameraMonitoringService.stop(this)
            CameraMonitoringService.start(this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
