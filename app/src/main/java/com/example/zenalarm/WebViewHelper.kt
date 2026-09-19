package com.example.zenalarm

import android.webkit.WebView

object WebViewHelper {
    fun configure(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            defaultTextEncodingName = "UTF-8"
        }
    }

    fun loadAssetHtml(webView: WebView, assetPath: String) {
        val html = webView.context.assets
            .open(assetPath)
            .bufferedReader(Charsets.UTF_8)
            .readText()
        webView.loadDataWithBaseURL(
            "file:///android_asset/",
            html,
            "text/html",
            "UTF-8",
            null,
        )
    }
}
