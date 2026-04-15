package com.ufrn.androidhub.modules.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.ufrn.androidhub.R
import kotlin.math.*

class CalculatorActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvDisplay: TextView

    private var expression: String = ""

    private var isDegrees: Boolean = true

    private var justEvaluated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calculator)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.calculator)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)

        tvExpression = findViewById(R.id.txtExpression)
        tvDisplay = findViewById(R.id.txtResultado)

        val digits = listOf(
            "0" to R.id.btn0, "1" to R.id.btn1, "2" to R.id.btn2,
            "3" to R.id.btn3, "4" to R.id.btn4, "5" to R.id.btn5,
            "6" to R.id.btn6, "7" to R.id.btn7, "8" to R.id.btn8,
            "9" to R.id.btn9, "." to R.id.btnPonto
        )
        digits.forEach { (d, id) ->
            findViewById<Button>(id).setOnClickListener { appendToken(d) }
        }

        mapOf(
            "+" to R.id.btnSomar,
            "−" to R.id.btnSubtrair,
            "×" to R.id.btnMultiplicar,
            "÷" to R.id.btnDividir,
            "^" to R.id.btnPow,
            "%" to R.id.btnPercent
        ).forEach { (op, id) ->
            findViewById<Button>(id).setOnClickListener { appendToken(op) }
        }

        findViewById<Button>(R.id.btnParOpen).setOnClickListener { appendToken("(") }
        findViewById<Button>(R.id.btnParClose).setOnClickListener { appendToken(")") }

        findViewById<Button>(R.id.btnPi).setOnClickListener { appendToken("π") }
        findViewById<Button>(R.id.btnE).setOnClickListener { appendToken("e") }

        mapOf(
            "sin(" to R.id.btnSin,
            "cos(" to R.id.btnCos,
            "tan(" to R.id.btnTan,
            "asin(" to R.id.btnAsin,
            "acos(" to R.id.btnAcos,
            "atan(" to R.id.btnAtan,
            "log(" to R.id.btnLog,
            "ln(" to R.id.btnLn,
            "sqrt(" to R.id.btnSqrt,
            "abs(" to R.id.btnAbs
        ).forEach { (fn, id) ->
            findViewById<Button>(id).setOnClickListener { appendToken(fn) }
        }

        findViewById<Button>(R.id.btnIgual).setOnClickListener { onEquals() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearAll() }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { backspace() }
        findViewById<Button>(R.id.btnSign).setOnClickListener { toggleSign() }

        val btnDegRad = findViewById<Button>(R.id.btnDegRad)
        btnDegRad.text = "DEG"
        btnDegRad.setOnClickListener {
            isDegrees = !isDegrees
            btnDegRad.text = if (isDegrees) "DEG" else "RAD"
        }

        updateDisplay()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }


    private fun appendToken(token: String) {
        if (justEvaluated) {
            if (token.first().isDigit() || token == ".") {
                expression = ""
            }
            justEvaluated = false
        }

        if (token == ".") {
            val lastNumber = expression.takeLastWhile { it.isDigit() || it == '.' }
            if (lastNumber.contains('.')) return
        }

        expression += token
        updateDisplay()
    }

    private fun onEquals() {
        if (expression.isBlank()) return
        try {
            val result = evaluate(expression)
            tvExpression.text = expression
            expression = formatResult(result)
            tvDisplay.text = expression
            justEvaluated = true
        } catch (e: Exception) {
            tvDisplay.text = getString(R.string.calc_error)
            expression = ""
        }
    }

    /**
     * Avalia a expressão matemática usando um parser recursivo descendente.
     * Suporta: +, −, ×, ÷, ^, %, (), sin, cos, tan, asin, acos, atan,
     *          log (base 10), ln, sqrt, abs, π, e.
     */
    private fun evaluate(expr: String): Double {
        val tokens = tokenize(expr)
        val parser = Parser(tokens, isDegrees)
        val result = parser.parseExpression()
        if (parser.pos != tokens.size) throw IllegalArgumentException("Expressão inválida")
        return result
    }

    private fun tokenize(input: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        val s = input.trim()
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    result.add(s.substring(start, i))
                }
                c.isLetter() || c == 'π' -> {
                    val start = i
                    while (i < s.length && (s[i].isLetter() || s[i] == 'π')) i++
                    result.add(s.substring(start, i))
                }
                c == '−' -> { result.add("-"); i++ }
                c == '×' -> { result.add("*"); i++ }
                c == '÷' -> { result.add("/"); i++ }
                else -> { result.add(c.toString()); i++ }
            }
        }
        return result
    }

    private inner class Parser(private val tokens: List<String>, private val deg: Boolean) {
        var pos = 0

        private fun peek() = tokens.getOrNull(pos)
        private fun consume() = tokens[pos++]

        fun parseExpression(): Double = parseAddSub()

        private fun parseAddSub(): Double {
            var left = parseMulDiv()
            while (peek() == "+" || peek() == "-") {
                val op = consume()
                val right = parseMulDiv()
                left = if (op == "+") left + right else left - right
            }
            return left
        }

        private fun parseMulDiv(): Double {
            var left = parsePow()
            while (peek() == "*" || peek() == "/") {
                val op = consume()
                val right = parsePow()
                if (op == "/" && right == 0.0) {
                    Toast.makeText(this@CalculatorActivity,
                        getString(R.string.calc_div_zero), Toast.LENGTH_SHORT).show()
                    throw ArithmeticException("Divisão por zero")
                }
                left = if (op == "*") left * right else left / right
            }
            return left
        }

        private fun parsePow(): Double {
            val base = parseUnary()
            return if (peek() == "^") {
                consume()
                base.pow(parsePow())
            } else base
        }

        private fun parseUnary(): Double {
            if (peek() == "-") {
                consume()
                return -parsePostfix()
            }
            if (peek() == "+") { consume() }
            return parsePostfix()
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            if (peek() == "%") {
                consume()
                value /= 100.0
            }
            return value
        }

        private fun parsePrimary(): Double {
            val t = peek() ?: throw IllegalArgumentException("Fim inesperado")

            t.toDoubleOrNull()?.let { consume(); return it }

            if (t == "π" || t == "pi") { consume(); return Math.PI }
            if (t == "e")              { consume(); return Math.E  }

            if (t == "(") {
                consume()
                val v = parseExpression()
                if (peek() == ")") consume()
                return v
            }

            val fnName = t.lowercase()
            val fns = setOf("sin","cos","tan","asin","acos","atan","log","ln","sqrt","abs")
            if (fnName in fns) {
                consume()
                if (peek() == "(") consume()
                val arg = parseExpression()
                if (peek() == ")") consume()
                return applyFunction(fnName, arg)
            }

            throw IllegalArgumentException("Token desconhecido: $t")
        }

        private fun applyFunction(name: String, arg: Double): Double {
            val radArg = if (deg && name in listOf("sin","cos","tan")) Math.toRadians(arg) else arg
            return when (name) {
                "sin"  -> sin(radArg)
                "cos"  -> cos(radArg)
                "tan"  -> tan(radArg)
                "asin" -> { val r = asin(arg); if (deg) Math.toDegrees(r) else r }
                "acos" -> { val r = acos(arg); if (deg) Math.toDegrees(r) else r }
                "atan" -> { val r = atan(arg); if (deg) Math.toDegrees(r) else r }
                "log"  -> log10(arg)
                "ln"   -> ln(arg)
                "sqrt" -> sqrt(arg)
                "abs"  -> abs(arg)
                else   -> throw IllegalArgumentException("Função desconhecida: $name")
            }
        }
    }


    private fun formatResult(v: Double): String {
        if (v.isNaN()) return getString(R.string.calc_error)
        if (v.isInfinite()) return if (v > 0) "∞" else "-∞"
        val s = "%.10g".format(v)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }

    private fun clearAll() {
        expression = ""
        justEvaluated = false
        tvExpression.text = ""
        updateDisplay()
    }

    private fun backspace() {
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            updateDisplay()
        }
    }

    private fun toggleSign() {
        if (expression.isEmpty()) return
        expression = if (expression.startsWith("-")) {
            expression.substring(1)
        } else {
            "-$expression"
        }
        updateDisplay()
    }

    private fun updateDisplay() {
        tvDisplay.text = expression.ifEmpty { "0" }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("expression", expression)
        outState.putBoolean("isDegrees", isDegrees)
        outState.putBoolean("justEvaluated", justEvaluated)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        expression     = savedInstanceState.getString("expression", "")
        isDegrees      = savedInstanceState.getBoolean("isDegrees", true)
        justEvaluated  = savedInstanceState.getBoolean("justEvaluated", false)
        findViewById<Button>(R.id.btnDegRad).text = if (isDegrees) "DEG" else "RAD"
        updateDisplay()
    }
}