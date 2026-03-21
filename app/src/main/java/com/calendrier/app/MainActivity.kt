package com.calendrier.app

import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var firstTap = 0
    private var firstTapTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("activated", false)) {
            startActivity(Intent(this, ActivationActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val calendar = findViewById<CalendarView>(R.id.calendar_view)

        calendar.setOnDateChangeListener { _, _, _, dayOfMonth ->
            val now = System.currentTimeMillis()

            if (dayOfMonth == 5) {
                // First tap: day 5
                firstTap = 5
                firstTapTime = now
            } else if (dayOfMonth == 19 && firstTap == 5 && (now - firstTapTime) < 5000) {
                // Second tap: day 19 within 5 seconds after day 5
                firstTap = 0
                startActivity(Intent(this, BrowserActivity::class.java))
            } else {
                firstTap = 0
            }
        }
    }
}
