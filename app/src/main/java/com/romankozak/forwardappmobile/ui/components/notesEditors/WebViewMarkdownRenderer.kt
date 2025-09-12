package com.romankozak.forwardappmobile.ui.components.notesEditors

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.LinearLayout

@SuppressLint("SetJavaScriptEnabled")
class WebViewMarkdownViewer
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : LinearLayout(context, attrs) {
        private val webView: WebView = WebView(context)
        private var isJsReady = false
        private var pendingMarkdown: String? = null

        init {
            orientation = VERTICAL
            webView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            addView(webView)

            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true

            webView.addJavascriptInterface(JSBridge(), "AndroidBridge")
            webView.loadUrl("file:///android_asset/viewer.html")
        }

        fun renderMarkdown(markdown: String) {
            if (isJsReady) {
                val escapedMarkdown = escapeForJS(markdown)
                val jsCode = "window.renderMarkdown($escapedMarkdown);"
                post { webView.evaluateJavascript(jsCode, null) }
            } else {
                pendingMarkdown = markdown
            }
        }

        private fun escapeForJS(text: String): String =
            "'" +
                text
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "") + "'"

        inner class JSBridge {
            @JavascriptInterface
            fun onJsReady() {
                post {
                    isJsReady = true
                    pendingMarkdown?.let {
                        renderMarkdown(it)
                        pendingMarkdown = null
                    }
                }
            }
        }
    }
