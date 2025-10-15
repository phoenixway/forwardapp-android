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
        private var pendingIsDark: Boolean? = null

        init {
            orientation = VERTICAL
            webView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            addView(webView)

            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true
            webView.setBackgroundColor(0) // TRANSPARENT
            webView.settings.textZoom = (100 * resources.configuration.fontScale).toInt()

            webView.addJavascriptInterface(JSBridge(), "AndroidBridge")
            webView.loadUrl("file:///android_asset/viewer.html")
        }

        fun renderMarkdown(markdown: String, isDark: Boolean) {
            if (isJsReady) {
                val jsTheme = if (isDark) {
                    "document.body.classList.add('dark');"
                } else {
                    "document.body.classList.remove('dark');"
                }
                post { webView.evaluateJavascript(jsTheme, null) }

                val escapedMarkdown = escapeForJS(markdown)
                val jsCode = "window.renderMarkdown($escapedMarkdown);"
                post { webView.evaluateJavascript(jsCode, null) }
            } else {
                pendingMarkdown = markdown
                pendingIsDark = isDark
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
                    pendingMarkdown?.let { markdown ->
                        pendingIsDark?.let { isDark ->
                            renderMarkdown(markdown, isDark)
                        }
                        pendingMarkdown = null
                        pendingIsDark = null
                    }
                }
            }
        }
    }
