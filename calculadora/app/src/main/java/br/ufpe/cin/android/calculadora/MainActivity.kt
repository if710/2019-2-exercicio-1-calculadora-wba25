package br.ufpe.cin.android.calculadora

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.R.attr.orientation
import android.content.res.Configuration


class MainActivity : AppCompatActivity() {

    var expr = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateExpr("0.0")
        addTextInfo("")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        toastSomething("Configuration was changed")
        val sharedPref = getPreferences(MODE_PRIVATE) ?: return
        val infoTxt = sharedPref.getString("info", "")
        if(infoTxt!= null) addTextInfo(infoTxt)
    }

    fun onClickNumber(view: View) {
        if (view is Button) {
            addElementToExpr(view.text.toString())
        }
    }

    fun addElementToExpr(elem: String) {
        when (elem) {
            "=" -> {
                val res = eval(expr).toString()
                updateExpr(res)
                addTextInfo(res)
            }
            "C" -> {
                updateExpr("0.0")
            }
            else -> {
                if(expr == "0.0") expr = ""
                updateExpr(expr + elem)
            }
        }
    }

    fun updateExpr(newExpr: String) {
        expr = newExpr
        addTextResult(expr)
    }

    fun toastSomething(txt: String) {
        Toast.makeText(this, txt, Toast.LENGTH_LONG).show()
    }

    fun addTextInfo(txt: String) {
        val infoText = findViewById<TextView>(R.id.text_info)
        infoText.text = txt
        val sharedPref = getPreferences(MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString("info", txt)
            commit()
        }
    }

    fun addTextResult(txt: String) {
        val resultText = findViewById<EditText>(R.id.text_calc)
        resultText.setText(txt)
    }

    //Como usar a função:
    // eval("2+2") == 4.0
    // eval("2+3*4") = 14.0
    // eval("(2+3)*4") = 20.0
    //Fonte: https://stackoverflow.com/a/26227947
    fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch: Char = ' '
            fun nextChar() {
                val size = str.length
                ch = if ((++pos < size)) str.get(pos) else (-1).toChar()
            }

            fun eat(charToEat: Char): Boolean {
                while (ch == ' ') nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) {
                    toastSomething("Caractere inesperado: " + ch)
                    return 0.0
                }
                return x
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            // | number | functionName factor | factor `^` factor
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'))
                        x += parseTerm() // adição
                    else if (eat('-'))
                        x -= parseTerm() // subtração
                    else
                        return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'))
                        x *= parseFactor() // multiplicação
                    else if (eat('/'))
                        x /= parseFactor() // divisão
                    else
                        return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+')) return parseFactor() // + unário
                if (eat('-')) return -parseFactor() // - unário
                var x: Double
                val startPos = this.pos
                if (eat('(')) { // parênteses
                    x = parseExpression()
                    eat(')')
                } else if ((ch in '0'..'9') || ch == '.') { // números
                    while ((ch in '0'..'9') || ch == '.') nextChar()
                    try {
                        x = java.lang.Double.parseDouble(str.substring(startPos, this.pos))
                    }
                    catch (e: NumberFormatException) {
                        toastSomething("Erro de conversão")
                        return 0.0
                    }
                } else if (ch in 'a'..'z') { // funções
                    while (ch in 'a'..'z') nextChar()
                    val func = str.substring(startPos, this.pos)
                    x = parseFactor()
                    if (func == "sqrt")
                        x = Math.sqrt(x)
                    else if (func == "sin")
                        x = Math.sin(Math.toRadians(x))
                    else if (func == "cos")
                        x = Math.cos(Math.toRadians(x))
                    else if (func == "tan")
                        x = Math.tan(Math.toRadians(x))
                    else{
                        toastSomething("Função desconhecida: " + func)
                        return 0.0
                    }
                } else {
                    toastSomething("Caractere inesperado: " + ch.toChar())
                    return 0.0
                }
                if (eat('^')) x = Math.pow(x, parseFactor()) // potência
                return x
            }
        }.parse()
    }
}
