package com.ufrn.androidhub.modules.hub

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.ufrn.androidhub.R
import com.ufrn.androidhub.modules.basketball.BasketBallActivity
import com.ufrn.androidhub.modules.calculator.CalculatorActivity
import java.util.Calendar

class HubActivity : AppCompatActivity() {

    private data class Module(
        val prefKey: String,
        val nameRes: Int,
        val descRes: Int,
        val intentClass: Class<*>
    )

    private lateinit var prefs: SharedPreferences

    // Ordem aqui define o "padrão" no primeiro uso (todos com 0 acessos)
    private val modules by lazy {
        listOf(
            Module("access_basketball", R.string.basket_module_name, R.string.basket_desc, BasketBallActivity::class.java),
            Module("access_calculator", R.string.calculator_name, R.string.calculator_desc, CalculatorActivity::class.java),
            Module("access_todo", R.string.third_app_name, R.string.third_app_desc, CalculatorActivity::class.java) // trocar quando criar
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hub)

        prefs = getSharedPreferences("hub_stats", MODE_PRIVATE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.hub)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnToggle = findViewById<ImageButton>(R.id.btnToggleTheme)
        updateThemeIcon(btnToggle)
        btnToggle.setOnClickListener {
            val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            AppCompatDelegate.setDefaultNightMode(
                if (isNight) AppCompatDelegate.MODE_NIGHT_NO
                else AppCompatDelegate.MODE_NIGHT_YES
            )
        }
    }

    override fun onResume() {
        super.onResume()
        renderModules()
    }

    private fun renderModules() {
        val sorted = modules.sortedByDescending { prefs.getInt(it.prefKey, 0) }

        bindHeroCard(sorted[0])
        bindMiniCard(
            card     = findViewById(R.id.cardMini1),
            titleId  = R.id.tvMini1Title,
            descId   = R.id.tvMini1Desc,
            module   = sorted[1]
        )
        bindMiniCard(
            card     = findViewById(R.id.cardMini2),
            titleId  = R.id.tvMini2Title,
            descId   = R.id.tvMini2Desc,
            module   = sorted[2]
        )
    }

    private fun bindHeroCard(module: Module) {
        val card = findViewById<MaterialCardView>(R.id.cardHero)
        card.findViewById<TextView>(R.id.tvHeroTitle).text = getString(module.nameRes)
        card.findViewById<TextView>(R.id.tvHeroDesc).text  = getString(module.descRes)
        card.setOnClickListener { openModule(module) }
    }

    private fun bindMiniCard(
        card: MaterialCardView,
        titleId: Int,
        descId: Int,
        module: Module
    ) {
        card.findViewById<TextView>(titleId).text = getString(module.nameRes)
        card.findViewById<TextView>(descId).text  = getString(module.descRes)
        card.setOnClickListener { openModule(module) }
    }

    private fun openModule(module: Module) {
        prefs.edit().putInt(module.prefKey, prefs.getInt(module.prefKey, 0) + 1).apply()
        startActivity(Intent(this, module.intentClass))
    }

    private fun updateThemeIcon(btn: ImageButton) {
        val isNight = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO  -> false
            else -> resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        btn.setImageResource(if (isNight) R.drawable.ic_sun else R.drawable.ic_moon)
    }
}