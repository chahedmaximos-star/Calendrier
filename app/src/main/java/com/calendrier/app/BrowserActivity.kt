package com.calendrier.app

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var webContainer: LinearLayout

    private val webViews = mutableListOf<WebView>()
    private var activeWebView: WebView? = null

    private var backPressCount = 0
    private var lastBackPressTime = 0L

    companion object {
        const val DEFAULT_URL = "https://www.google.com"
        const val PREF_LAST_URL = "last_url"
        const val MAX_PANELS = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        etUrl = findViewById(R.id.et_url)
        webContainer = findViewById(R.id.web_container)
        val btnNewTab = findViewById<ImageButton>(R.id.btn_new_tab)

        etUrl.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                navigate(); true
            } else false
        }

        btnNewTab.setOnClickListener {
            if (webViews.size < MAX_PANELS) {
                addPanel(DEFAULT_URL)
            } else {
                Toast.makeText(this, "Maximum $MAX_PANELS fenêtres", Toast.LENGTH_SHORT).show()
            }
        }

        val lastUrl = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString(PREF_LAST_URL, DEFAULT_URL) ?: DEFAULT_URL
        addPanel(lastUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun addPanel(url: String) {
        // Wrapper for each panel (WebView + close button)
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val wv = createWebView(wrapper)
        wv.loadUrl(url)

        // Close button for this panel
        val btnClose = TextView(this).apply {
            text = "✕ Fermer"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#B71C1C"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 6, 0, 6)
            visibility = if (webViews.size >= 1) View.VISIBLE else View.GONE
            setOnClickListener {
                removePanel(wrapper, wv)
            }
        }

        wrapper.addView(btnClose, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        wrapper.addView(wv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // Divider between panels
        if (webViews.isNotEmpty()) {
            val divider = View(this).apply {
                setBackgroundColor(Color.parseColor("#DADCE0"))
            }
            val divLp = LinearLayout.LayoutParams(2, LinearLayout.LayoutParams.MATCH_PARENT)
            webContainer.addView(divider, divLp)
        }

        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        webContainer.addView(wrapper, lp)

        webViews.add(wv)
        activeWebView = wv

        // Refresh close buttons visibility
        updateCloseButtons()
    }

    private fun removePanel(wrapper: LinearLayout, wv: WebView) {
        val idx = webViews.indexOf(wv)
        if (idx < 0) return

        webViews.removeAt(idx)

        // Remove divider before this panel if exists
        val wrapperIdx = webContainer.indexOfChild(wrapper)
        if (wrapperIdx > 0) {
            webContainer.removeViewAt(wrapperIdx - 1) // divider
        }
        webContainer.removeView(wrapper)

        if (webViews.isEmpty()) {
            hideApp()
        } else {
            activeWebView = webViews.last()
            updateCloseButtons()
        }
    }

    private fun updateCloseButtons() {
        // Show close button only when more than 1 panel
        for (i in 0 until webContainer.childCount) {
            val child = webContainer.getChildAt(i)
            if (child is LinearLayout) {
                val closeBtn = child.getChildAt(0)
                if (closeBtn is TextView) {
                    closeBtn.visibility = if (webViews.size > 1) View.VISIBLE else View.GONE
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(wrapper: LinearLayout): WebView {
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
                if (view == activeWebView) {
                    etUrl.setText(url)
                    getSharedPreferences("prefs", MODE_PRIVATE)
                        .edit().putString(PREF_LAST_URL, url).apply()
                }
            }
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ) = false
        }
        wv.setOnTouchListener { v, _ ->
            activeWebView = v as WebView
            etUrl.setText(wv.url)
            false
        }
        return wv
    }

    private fun navigate() {
        var url = etUrl.text.toString().trim()
        if (url.isEmpty()) return
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = if (url.contains(".")) "https://$url"
            else "https://www.google.com/search?q=${url.replace(" ", "+")}"
        }
        activeWebView?.loadUrl(url)
    }

    fun hideApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.firstOrNull()?.setExcludeFromRecents(true)
        }
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
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

        if (activeWebView?.canGoBack() == true) activeWebView?.goBack()
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
