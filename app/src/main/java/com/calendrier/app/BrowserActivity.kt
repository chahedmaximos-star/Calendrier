package com.calendrier.app

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

    private val webViews = mutableListOf<WebView>()
    private val tabUrls = mutableListOf<String>()
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

        btnNewTab.setOnClickListener {
            addNewTab(DEFAULT_URL)
        }

        // Open first tab
        val lastUrl = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString(PREF_LAST_URL, DEFAULT_URL) ?: DEFAULT_URL
        addNewTab(lastUrl)
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
            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val idx = webViews.indexOf(view)
                if (idx == currentTab) {
                    etUrl.setText(url)
                    getSharedPreferences("prefs", MODE_PRIVATE)
                        .edit().putString(PREF_LAST_URL, url).apply()
                }
                if (idx >= 0) {
                    tabUrls[idx] = url ?: DEFAULT_URL
                    updateTabBar()
                }
            }
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ) = false
        }
        return wv
    }

    private fun addNewTab(url: String) {
        val wv = createWebView()
        wv.loadUrl(url)

        webViews.add(wv)
        tabUrls.add(url)

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        webContainer.addView(wv, lp)

        switchToTab(webViews.size - 1)
        updateTabBar()
    }

    private fun switchToTab(index: Int) {
        currentTab = index
        for (i in webViews.indices) {
            webViews[i].visibility = if (i == index) View.VISIBLE else View.GONE
        }
        etUrl.setText(tabUrls.getOrElse(index) { DEFAULT_URL })
        updateTabBar()
    }

    private fun closeTab(index: Int) {
        if (webViews.size == 1) {
            // Last tab: go home
            hideApp()
            return
        }
        webContainer.removeView(webViews[index])
        webViews.removeAt(index)
        tabUrls.removeAt(index)

        val newIndex = if (index >= webViews.size) webViews.size - 1 else index
        switchToTab(newIndex)
        updateTabBar()
    }

    private fun updateTabBar() {
        tabContainer.removeAllViews()
        for (i in webViews.indices) {
            val tabView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(12, 0, 8, 0)
                background = if (i == currentTab) {
                    val d = android.graphics.drawable.GradientDrawable()
                    d.setColor(Color.WHITE)
                    d.cornerRadius = 8f
                    d
                } else null
            }

            val tabLp = LinearLayout.LayoutParams(
                resources.displayMetrics.widthPixels / 3,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(2, 4, 2, 4) }

            // Tab title
            val tvTitle = TextView(this).apply {
                text = getDomain(tabUrls.getOrElse(i) { "Nouvel onglet" })
                textSize = 12f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(if (i == currentTab) Color.BLACK else Color.DKGRAY)
                if (i == currentTab) setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            // Close button
            val btnClose = TextView(this).apply {
                text = "✕"
                textSize = 12f
                setTextColor(Color.GRAY)
                setPadding(8, 0, 0, 0)
                setOnClickListener { closeTab(i) }
            }

            tabView.addView(tvTitle)
            tabView.addView(btnClose)
            tabView.setOnClickListener { switchToTab(i) }

            tabContainer.addView(tabView, tabLp)
        }
    }

    private fun getDomain(url: String): String {
        return try {
            val host = java.net.URL(url).host
            host.removePrefix("www.")
        } catch (e: Exception) {
            "Nouvel onglet"
        }
    }

    private fun navigate() {
        var url = etUrl.text.toString().trim()
        if (url.isEmpty()) return
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = if (url.contains(".")) "https://$url"
            else "https://www.google.com/search?q=${url.replace(" ", "+")}"
        }
        webViews.getOrNull(currentTab)?.loadUrl(url)
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

        if (backPressCount >= 3) {
            backPressCount = 0
            hideApp()
            return
        }

        val wv = webViews.getOrNull(currentTab)
        if (wv?.canGoBack() == true) wv.goBack()
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
