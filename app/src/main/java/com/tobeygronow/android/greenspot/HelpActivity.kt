package com.tobeygronow.android.greenspot

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

/**
 * Displays GreenSpots help website of choice
 */
class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help) // Choose layout

        // Get layouts WebView and load the website into that View
        val webView: WebView = findViewById(R.id.help_web_view)
        webView.loadUrl("https://identify.plantnet.org")
    }
}