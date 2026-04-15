package com.ufrn.androidhub.modules.basketball

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.ufrn.androidhub.R
import java.util.Locale

class BasketBallActivity : AppCompatActivity() {
    private var pontuacaoTimeA: Int = 0
    private var pontuacaoTimeB: Int = 0

    private lateinit var pTimeA: TextView
    private lateinit var pTimeB: TextView

    private var ultimaJogada: Pair<Int, String>? = null
    private lateinit var txtHistorico: TextView

    private lateinit var txtCronometro: TextView
    private lateinit var txtPeriodo: TextView
    private lateinit var spinnerQtdPeriodos: Spinner
    private lateinit var spinnerDuracaoPeriodo: Spinner
    private lateinit var btnAplicarConfig: Button
    private lateinit var btnStartPause: Button
    private lateinit var btnResetTempo: Button
    private lateinit var btnProximoPeriodo: Button

    private lateinit var qtdPeriodosAdapter: ArrayAdapter<Int>
    private lateinit var duracaoPeriodoAdapter: ArrayAdapter<Int>

    private var totalPeriodos: Int = 4
    private var duracaoPeriodoMs: Long = 12 * 60_000L
    private var periodoAtual: Int = 1

    private var tempoRestanteMs: Long = duracaoPeriodoMs
    private var cronometroRodando: Boolean = false
    private var countDownTimer: CountDownTimer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_basket_ball)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.basketball)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)

        pTimeA = findViewById(R.id.placarTimeA)
        pTimeB = findViewById(R.id.placarTimeB)

        txtHistorico = findViewById(R.id.txtHistorico)

        txtCronometro = findViewById(R.id.txtCronometro)
        txtPeriodo = findViewById(R.id.txtPeriodo)
        spinnerQtdPeriodos = findViewById(R.id.spinnerQtdPeriodos)
        spinnerDuracaoPeriodo = findViewById(R.id.spinnerDuracaoPeriodo)
        btnAplicarConfig = findViewById(R.id.btnAplicarConfig)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnResetTempo = findViewById(R.id.btnResetTempo)
        btnProximoPeriodo = findViewById(R.id.btnProximoPeriodo)

        val bTresPontosTimeA: Button = findViewById(R.id.tresPontosA)
        val bDoisPontosTimeA: Button = findViewById(R.id.doisPontosA)
        val bTLivreTimeA: Button = findViewById(R.id.tiroLivreA)
        val bTresPontosTimeB: Button = findViewById(R.id.tresPontosB)
        val bDoisPontosTimeB: Button = findViewById(R.id.doisPontosB)
        val bTLivreTimeB: Button = findViewById(R.id.tiroLivreB)
        val bReiniciar: Button = findViewById(R.id.reiniciarPartida)
        val bDesfazer: Button = findViewById(R.id.btnDesfazer)

        bDesfazer.setOnClickListener { desfazer() }

        bTresPontosTimeA.setOnClickListener { adicionarPontos(3, "A") }
        bDoisPontosTimeA.setOnClickListener { adicionarPontos(2, "A") }
        bTLivreTimeA.setOnClickListener { adicionarPontos(1, "A") }
        bTresPontosTimeB.setOnClickListener { adicionarPontos(3, "B") }
        bDoisPontosTimeB.setOnClickListener { adicionarPontos(2, "B") }
        bTLivreTimeB.setOnClickListener { adicionarPontos(1, "B") }
        bReiniciar.setOnClickListener { reiniciarPartida() }

        setupCronometroSpinners()
        restoreCronometroState(savedInstanceState)
        updateCronometroUi()

        btnAplicarConfig.setOnClickListener { aplicarConfiguracaoCronometro() }
        btnStartPause.setOnClickListener { toggleStartPause() }
        btnResetTempo.setOnClickListener { resetTempoDoPeriodo() }
        btnProximoPeriodo.setOnClickListener { proximoPeriodo() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupCronometroSpinners() {
        val opcoesQtd = listOf(2, 4)
        qtdPeriodosAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            opcoesQtd
        )
        spinnerQtdPeriodos.adapter = qtdPeriodosAdapter

        val opcoesDuracaoMin = listOf(5, 8, 10, 12)
        duracaoPeriodoAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            opcoesDuracaoMin
        )
        spinnerDuracaoPeriodo.adapter = duracaoPeriodoAdapter
    }

    private fun aplicarConfiguracaoCronometro() {
        if (cronometroRodando) {
            Toast.makeText(this, getString(R.string.timer_pause_to_change), Toast.LENGTH_SHORT).show()
            return
        }

        val qtd = spinnerQtdPeriodos.selectedItem as? Int
        val duracaoMin = spinnerDuracaoPeriodo.selectedItem as? Int
        if (qtd == null || duracaoMin == null) {
            Toast.makeText(this, getString(R.string.timer_invalid_config), Toast.LENGTH_SHORT).show()
            return
        }

        totalPeriodos = qtd
        duracaoPeriodoMs = duracaoMin * 60_000L
        periodoAtual = 1
        tempoRestanteMs = duracaoPeriodoMs
        txtHistorico.text = getString(R.string.timer_config_applied, totalPeriodos, duracaoMin)
        updateCronometroUi()
    }

    private fun toggleStartPause() {
        if (cronometroRodando) {
            pauseCronometro()
        } else {
            startCronometro()
        }
    }

    private fun startCronometro() {
        if (tempoRestanteMs <= 0L) {
            tempoRestanteMs = duracaoPeriodoMs
        }
        cronometroRodando = true
        btnStartPause.text = getString(R.string.timer_pause)
        iniciarCountDown(tempoRestanteMs)
    }

    private fun pauseCronometro() {
        cronometroRodando = false
        btnStartPause.text = getString(R.string.timer_start)
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun resetTempoDoPeriodo() {
        pauseCronometro()
        tempoRestanteMs = duracaoPeriodoMs
        updateCronometroUi()
    }

    private fun proximoPeriodo() {
        pauseCronometro()
        if (periodoAtual >= totalPeriodos) {
            Toast.makeText(this, getString(R.string.timer_game_end), Toast.LENGTH_SHORT).show()
            tempoRestanteMs = 0L
            updateCronometroUi()
            return
        }
        periodoAtual += 1
        tempoRestanteMs = duracaoPeriodoMs
        updateCronometroUi()
    }

    private fun iniciarCountDown(iniciarEmMs: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(iniciarEmMs, 250L) {
            override fun onTick(millisUntilFinished: Long) {
                tempoRestanteMs = millisUntilFinished
                updateCronometroUi()
            }

            override fun onFinish() {
                tempoRestanteMs = 0L
                cronometroRodando = false
                btnStartPause.text = getString(R.string.timer_start)
                updateCronometroUi()
                Toast.makeText(this@BasketBallActivity, getString(R.string.timer_period_end), Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun updateCronometroUi() {
        txtCronometro.text = formatarMs(tempoRestanteMs)
        txtPeriodo.text = getString(R.string.timer_period_label, periodoAtual, totalPeriodos)
        btnProximoPeriodo.isEnabled = !cronometroRodando
        btnAplicarConfig.isEnabled = !cronometroRodando
    }

    private fun formatarMs(ms: Long): String {
        val totalSeg = (ms.coerceAtLeast(0L) + 999L) / 1000L
        val min = totalSeg / 60
        val seg = totalSeg % 60
        return String.format(Locale.getDefault(), "%02d:%02d", min, seg)
    }

    private fun restoreCronometroState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            tempoRestanteMs = duracaoPeriodoMs
            val idxQtd = qtdPeriodosAdapter.getPosition(totalPeriodos)
            if (idxQtd >= 0) spinnerQtdPeriodos.setSelection(idxQtd)
            val idxDur = duracaoPeriodoAdapter.getPosition((duracaoPeriodoMs / 60_000L).toInt())
            if (idxDur >= 0) spinnerDuracaoPeriodo.setSelection(idxDur)
            btnStartPause.text = getString(R.string.timer_start)
            return
        }

        totalPeriodos = savedInstanceState.getInt(STATE_TOTAL_PERIODOS, totalPeriodos)
        duracaoPeriodoMs = savedInstanceState.getLong(STATE_DURACAO_MS, duracaoPeriodoMs)
        periodoAtual = savedInstanceState.getInt(STATE_PERIODO_ATUAL, periodoAtual)
        tempoRestanteMs = savedInstanceState.getLong(STATE_TEMPO_RESTANTE_MS, duracaoPeriodoMs)
        cronometroRodando = savedInstanceState.getBoolean(STATE_RODANDO, false)

        val idxQtd = qtdPeriodosAdapter.getPosition(totalPeriodos)
        if (idxQtd >= 0) spinnerQtdPeriodos.setSelection(idxQtd)
        val idxDur = duracaoPeriodoAdapter.getPosition((duracaoPeriodoMs / 60_000L).toInt())
        if (idxDur >= 0) spinnerDuracaoPeriodo.setSelection(idxDur)

        btnStartPause.text = if (cronometroRodando) getString(R.string.timer_pause) else getString(R.string.timer_start)
        if (cronometroRodando) {
            iniciarCountDown(tempoRestanteMs)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TOTAL_PERIODOS, totalPeriodos)
        outState.putLong(STATE_DURACAO_MS, duracaoPeriodoMs)
        outState.putInt(STATE_PERIODO_ATUAL, periodoAtual)
        outState.putLong(STATE_TEMPO_RESTANTE_MS, tempoRestanteMs)
        outState.putBoolean(STATE_RODANDO, cronometroRodando)
    }

    override fun onStop() {
        super.onStop()
        if (cronometroRodando) {
            pauseCronometro()
        }
    }

    fun adicionarPontos(pontos: Int, time: String) {
        if (time == "A") {
            pontuacaoTimeA += pontos
        } else {
            pontuacaoTimeB += pontos
        }
        atualizaPlacar(time)
        ultimaJogada = Pair(pontos, time)
        txtHistorico.text = getString(R.string.history_last_play, time, pontos)
    }

    fun atualizaPlacar(time: String) {
        if (time == "A") {
            pTimeA.setText(pontuacaoTimeA.toString())
        } else {
            pTimeB.setText(pontuacaoTimeB.toString())
        }
    }

    fun desfazer() {
        ultimaJogada?.let { (pontos, time) ->
            if (time == "A") pontuacaoTimeA -= pontos else pontuacaoTimeB -= pontos
            atualizaPlacar(time)
            txtHistorico.text = getString(R.string.history_undo)
            ultimaJogada = null
        }
    }

    fun reiniciarPartida() {
        pontuacaoTimeA = 0
        pTimeA.setText(pontuacaoTimeA.toString())
        pontuacaoTimeB = 0
        pTimeB.setText(pontuacaoTimeB.toString())
        periodoAtual = 1
        resetTempoDoPeriodo()
        Toast.makeText(this, "Placar reiniciado", Toast.LENGTH_SHORT).show()
    }
}

private const val STATE_TOTAL_PERIODOS = "state_total_periodos"
private const val STATE_DURACAO_MS = "state_duracao_ms"
private const val STATE_PERIODO_ATUAL = "state_periodo_atual"
private const val STATE_TEMPO_RESTANTE_MS = "state_tempo_restante_ms"
private const val STATE_RODANDO = "state_rodando"