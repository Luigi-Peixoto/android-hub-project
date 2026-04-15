package com.ufrn.androidhub.modules.pomodoro

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.ufrn.androidhub.R
import java.util.Locale

class PomodoroActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var btnStart: MaterialButton
    private lateinit var btnReset: ImageButton
    private lateinit var btnMusic: ImageButton
    
    private lateinit var btnPomodoroMode: MaterialButton
    private lateinit var btnShortBreakMode: MaterialButton
    private lateinit var btnLongBreakMode: MaterialButton
    private lateinit var llTracker: LinearLayout

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var timeRemainingMillis: Long = 25 * 60 * 1000L // 25 min default

    private val configPomodoro = 25 * 60 * 1000L
    private val configShortBreak = 5 * 60 * 1000L
    private val configLongBreak = 15 * 60 * 1000L

    private var currentMode = "pomodoro"
    private var currentMusicRawId: Int? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pomodoro)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pomodoro)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvTimer = findViewById(R.id.tvTimer)
        btnStart = findViewById(R.id.btnStart)
        btnReset = findViewById(R.id.btnReset)
        btnMusic = findViewById(R.id.btnMusic)
        btnPomodoroMode = findViewById(R.id.btnPomodoroMode)
        btnShortBreakMode = findViewById(R.id.btnShortBreakMode)
        btnLongBreakMode = findViewById(R.id.btnLongBreakMode)
        llTracker = findViewById(R.id.llTracker)

        updateTimerText()

        btnStart.setOnClickListener {
            if (isTimerRunning) pauseTimer() else startTimer()
        }

        btnReset.setOnClickListener {
            resetTimer()
        }

        btnMusic.setOnClickListener {
            showMusicDialog()
        }

        btnPomodoroMode.setOnClickListener { setMode("pomodoro", configPomodoro) }
        btnShortBreakMode.setOnClickListener { setMode("short", configShortBreak) }
        btnLongBreakMode.setOnClickListener { setMode("long", configLongBreak) }
    }

    private fun setMode(mode: String, timeInMillis: Long) {
        currentMode = mode
        resetTimerTo(timeInMillis)
        
        // Reset styles
        btnPomodoroMode.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        btnPomodoroMode.setTextColor(Color.WHITE)
        btnShortBreakMode.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        btnShortBreakMode.setTextColor(Color.WHITE)
        btnLongBreakMode.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        btnLongBreakMode.setTextColor(Color.WHITE)

        // Set active style
        when (mode) {
            "pomodoro" -> {
                btnPomodoroMode.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                btnPomodoroMode.setTextColor(Color.BLACK)
            }
            "short" -> {
                btnShortBreakMode.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                btnShortBreakMode.setTextColor(Color.BLACK)
            }
            "long" -> {
                btnLongBreakMode.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                btnLongBreakMode.setTextColor(Color.BLACK)
            }
        }
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeRemainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                isTimerRunning = false
                btnStart.text = "start"
                timeRemainingMillis = 0
                updateTimerText()
                handleTimerFinished()
            }
        }.start()

        isTimerRunning = true
        btnStart.text = "pause"
        playBackgroundMusic()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        btnStart.text = "start"
        pauseBackgroundMusic()
    }

    private fun resetTimer() {
        pauseTimer()
        val defaultTime = when (currentMode) {
            "short" -> configShortBreak
            "long" -> configLongBreak
            else -> configPomodoro
        }
        timeRemainingMillis = defaultTime
        updateTimerText()
    }

    private fun resetTimerTo(timeInMillis: Long) {
        pauseTimer()
        timeRemainingMillis = timeInMillis
        updateTimerText()
    }

    private fun updateTimerText() {
        val minutes = (timeRemainingMillis / 1000) / 60
        val seconds = (timeRemainingMillis / 1000) % 60
        tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun handleTimerFinished() {
        pauseBackgroundMusic()
        playAlarm()
        addTrackerIcon()
    }

    private fun addTrackerIcon() {
        val imageView = ImageView(this)
        val iconRes = when (currentMode) {
            "short" -> R.drawable.ic_lightning
            "long" -> R.drawable.ic_clock
            else -> R.drawable.ic_circle_outline
        }
        imageView.setImageResource(iconRes)
        imageView.setColorFilter(Color.WHITE)
        val params = LinearLayout.LayoutParams(64, 64)
        params.marginStart = 8
        params.marginEnd = 8
        imageView.layoutParams = params
        llTracker.addView(imageView)
    }

    private fun playBackgroundMusic() {
        if (currentMusicRawId == null) return
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, currentMusicRawId!!)
            mediaPlayer?.setVolume(1.0f, 1.0f)
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.start()
    }

    private fun pauseBackgroundMusic() {
        mediaPlayer?.pause()
    }

    private fun stopBackgroundMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun playAlarm() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showMusicDialog() {
        val options = arrayOf("Floresta", "Chuva", "Mar", "Lareira", "Nenhuma")
        AlertDialog.Builder(this)
            .setTitle("Escolher som de fundo")
            .setItems(options) { _, which ->
                stopBackgroundMusic()
                currentMusicRawId = when (which) {
                    0 -> R.raw.forest
                    1 -> R.raw.rain
                    2 -> R.raw.ocean
                    3 -> R.raw.firepit
                    else -> null
                }
                if (isTimerRunning) {
                    playBackgroundMusic()
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBackgroundMusic()
    }
}
