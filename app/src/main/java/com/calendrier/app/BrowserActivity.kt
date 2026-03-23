package com.calculatrice.app  // أو com.calendrier.app

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var tabContainer: LinearLayout
    private lateinit var webContainer: FrameLayout

    data class Tab(val webView: WebView, var title: String = "Nouvel onglet", var url: String = DEFAULT_URL)

    private val tabs = mutableListOf<Tab>()
    private var currentTab = 0
    private var backPressCount = 0
    private var lastBackPressTime = 0L

    companion object {
        const val DEFAULT_URL = "https://www.google.com"
        const val PREF_LAST_URL = "last_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        etUrl = findViewById(R.id.et_url)
        tabContainer = findViewById(R.id.tab_container)
        webContainer = findViewById(R.id.web_container)
        val btnNewTab = findViewById<ImageButton>(R.id.btn_new_tab)

        etUrl.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                navigate(); true
            } else false
        }

        btnNewTab.setOnClickListener { openNewTab(DEFAULT_URL) }

        val lastUrl = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString(PREF_LAST_URL, DEFAULT_URL) ?: DEFAULT_URL
        openNewTab(lastUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val wv = WebView(this)
        wv.settings.apply {
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
            savePassword = true
            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val idx = tabs.indexOfFirst { it.webView == view }
                if (idx < 0) return
                tabs[idx].url = url ?: DEFAULT_URL
                tabs[idx].title = try {
                    java.net.URL(url).host.removePrefix("www.")
                } catch (e: Exception) { "Nouvel onglet" }
                if (idx == currentTab) {
                    etUrl.setText(url)
                    getSharedPreferences("prefs", MODE_PRIVATE)
                        .edit().putString(PREF_LAST_URL, url).apply()
                }
                renderTabBar()
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
        }
        return wv
    }

    private fun openNewTab(url: String) {
        val wv = createWebView()
        webContainer.addView(wv, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        wv.loadUrl(url)
        tabs.add(Tab(wv, "Chargement...", url))
        switchToTab(tabs.size - 1)
    }

    private fun switchToTab(index: Int) {
        currentTab = index
        tabs.forEachIndexed { i, tab ->
            tab.webView.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        etUrl.setText(tabs.getOrNull(index)?.url ?: DEFAULT_URL)
        renderTabBar()
    }

    private fun closeTab(index: Int) {
        if (tabs.size == 1) { hideApp(); return }
        webContainer.removeView(tabs[index].webView)
        tabs.removeAt(index)
        switchToTab(if (index >= tabs.size) tabs.size - 1 else index)
    }

    private fun renderTabBar() {
        tabContainer.removeAllViews()
        tabs.forEachIndexed { i, tab ->
            val isActive = i == currentTab
            val tabView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(16, 0, 8, 0)
                setBackgroundColor(if (isActive) Color.WHITE else Color.TRANSPARENT)
            }
            val lp = LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.35).toInt(),
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(2, 3, 2, 3) }

            val tvTitle = TextView(this).apply {
                text = tab.title
                textSize = 12f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(if (isActive) Color.BLACK else Color.DKGRAY)
                if (isActive) setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnClose = TextView(this).apply {
                text = " ✕"
                textSize = 13f
                setTextColor(Color.GRAY)
                setOnClickListener { closeTab(i) }
            }
            tabView.addView(tvTitle)
            tabView.addView(btnClose)
            tabView.setOnClickListener { switchToTab(i) }
            tabContainer.addView(tabView, lp)
        }
    }

    private fun navigate() {
        var url = etUrl.text.toString().trim()
        if (url.isEmpty()) return
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = if (url.contains(".")) "https://$url"
            else "https://www.google.com/search?q=${url.replace(" ", "+")}"
        }
        tabs.getOrNull(currentTab)?.webView?.loadUrl(url)
    }

    private fun hideApp() {
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit().putBoolean("browser_open", false).apply()
        CookieManager.getInstance().flush()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.firstOrNull()?.setExcludeFromRecents(true)
        }
        moveTaskToBack(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 800) backPressCount++ else backPressCount = 1
        lastBackPressTime = now
        if (backPressCount >= 3) { backPressCount = 0; hideApp(); return }
        val wv = tabs.getOrNull(currentTab)?.webView
        if (wv?.canGoBack() == true) wv.goBack()
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
