package com.calendrier.app

import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        // First launch: show activation screen
        if (!prefs.getBoolean("activated", false)) {
            startActivity(Intent(this, ActivationActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Start the background tap detector service
        startService(Intent(this, TapDetectorService::class.java))
    }
}
