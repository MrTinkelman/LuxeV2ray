package com.mrtinkelman.v2ray.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mrtinkelman.v2ray.R


class AboutActivity : AppCompatActivity() {


    // This is the loading time of the splash screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        title = "About"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


}