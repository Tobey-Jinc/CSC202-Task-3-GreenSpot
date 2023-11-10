package com.tobeygronow.android.greenspot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

/**
 * Starts the app with the List UI
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Display the layout
    }
}