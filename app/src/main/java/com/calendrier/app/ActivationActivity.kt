package com.calendrier.app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ActivationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activation)

        val etCode = findViewById<EditText>(R.id.et_activation_code)
        val btnActivate = findViewById<Button>(R.id.btn_activate)
        val tvError = findViewById<TextView>(R.id.tv_error)

        btnActivate.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code == "Max1987") {
                // Save activation flag
                getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit().putBoolean("activated", true).apply()
                // Go to fake calendar
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                tvError.visibility = android.view.View.VISIBLE
                tvError.text = "Code invalide. Veuillez réessayer."
                etCode.text.clear()
            }
        }
    }
}
