package com.ufrn.androidhub.modules.hub

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.ufrn.androidhub.R
import com.ufrn.androidhub.modules.basketball.BasketBallActivity
import com.ufrn.androidhub.modules.calculator.CalculatorActivity

class HubActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hub)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.hub)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnToggle = findViewById<ImageButton>(R.id.btnToggleTheme)
        updateThemeIcon(btnToggle)

        btnToggle.setOnClickListener {
            val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            if (isNight) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        findViewById<MaterialCardView>(R.id.cardCalculator).setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardBasketball).setOnClickListener {
            startActivity(Intent(this, BasketBallActivity::class.java))
        }
    }

    private fun updateThemeIcon(btn: ImageButton) {
        val isNight = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                val nightModeFlags = resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }
        btn.setImageResource(if (isNight) R.drawable.ic_sun else R.drawable.ic_moon)
    }
}