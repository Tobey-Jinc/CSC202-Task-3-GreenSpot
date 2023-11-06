package com.tobeygronow.android.greenspot

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val webView: WebView = findViewById(R.id.help_web_view)
        webView.loadUrl("https://identify.plantnet.org")
    }
}