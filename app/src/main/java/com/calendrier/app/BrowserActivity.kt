package com.calendrier.app

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etUrl: EditText

    // Triple back press to hide
    private var backPressCount = 0
    private var lastBackPressTime = 0L

    companion object {
        const val DEFAULT_URL = "https://www.google.com"
        const val PREF_LAST_URL = "last_url"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        webView = findViewById(R.id.web_view)
        etUrl = findViewById(R.id.et_url)
        val btnNewTab = findViewById<ImageButton>(R.id.btn_new_tab)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                etUrl.setText(url)
                getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit().putString(PREF_LAST_URL, url).apply()
            }
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ) = false
        }

        val lastUrl = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString(PREF_LAST_URL, DEFAULT_URL) ?: DEFAULT_URL
        webView.loadUrl(lastUrl)

        // Enter key navigates
        etUrl.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                navigate(); true
            } else false
        }

        // + button opens new page (Google)
        btnNewTab.setOnClickListener {
            etUrl.setText("")
            webView.loadUrl(DEFAULT_URL)
            getSharedPreferences("prefs", MODE_PRIVATE)
                .edit().putString(PREF_LAST_URL, DEFAULT_URL).apply()
        }
    }

    private fun navigate() {
        var url = etUrl.text.toString().trim()
        if (url.isEmpty()) return
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = if (url.contains(".")) "https://$url"
            else "https://www.google.com/search?q=${url.replace(" ", "+")}"
        }
        webView.loadUrl(url)
    }

    fun hideApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.firstOrNull()?.setExcludeFromRecents(true)
        }
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 800) backPressCount++
        else backPressCount = 1
        lastBackPressTime = now

        // Triple back press = hide app
        if (backPressCount >= 3) {
            backPressCount = 0
            hideApp()
            return
        }

        if (webView.canGoBack()) webView.goBack()
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
