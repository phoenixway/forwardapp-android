package com.romankozak.forwardappmobile.ui.components

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout

class WebViewMarkdownRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val webView: WebView
    var onMarkdownChanged: ((String) -> Unit)? = null

    init {
        orientation = VERTICAL
        webView = WebView(context)
        webView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        addView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        // JS Bridge для передачі даних назад у Kotlin
        webView.addJavascriptInterface(JSBridge(), "AndroidBridge")

        // Завантаження HTML редактора
        webView.loadUrl("file:///android_asset/editor.html")
    }

    /** Оновлення тексту Markdown з Kotlin */
    fun setMarkdown(markdown: String) {
        val js = "editor.setValue(${escapeForJS(markdown)});"
        webView.evaluateJavascript(js, null)
    }

    private fun escapeForJS(text: String): String {
        return "'" + text.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "") + "'"
    }

    inner class JSBridge {
        @android.webkit.JavascriptInterface
        fun onMarkdownChange(text: String) {
            post {
                onMarkdownChanged?.invoke(text)
            }
        }
    }
}
