package com.mrtinkelman.v2ray.ui;

import android.annotation.SuppressLint;
import android.app.ActivityManager
import android.app.AlertDialog;
import android.content.Context
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable
import android.text.Html;
import android.text.TextWatcher
import android.view.View;
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat
import com.mrtinkelman.v2ray.R;
import com.mrtinkelman.v2ray.service.HttpProxyService

class VPNHotspot : BaseActivity() {
        private lateinit var etPort: EditText
        private lateinit var ports: TextView
        private lateinit var btnStop: Button
        private lateinit var btnStart: Button
        private lateinit var status: TextView
        private lateinit var cbUseSystemProxy: CheckBox

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hotspot)
        title = "VPNHotspot"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        btnStart = findViewById(R.id.btnStart)
        etPort = findViewById(R.id.etPort)
         cbUseSystemProxy = findViewById<CheckBox>(R.id.cbUseSystemProxy)
        ports = findViewById(R.id.port)
        status = findViewById(R.id.status)
        btnStart.setOnClickListener {
        val port = etPort.text.toString().toIntOrNull() ?: 1081

        startService(HttpProxyService.newStartIntent(this, port))
        etPort.setText(port.toString())

        }

        btnStop = findViewById(R.id.btnStop)
        btnStop.setOnClickListener {
        stopService(HttpProxyService.newStopIntent(this))
        }

        var useSystemProxy = cbUseSystemProxy.isChecked
        cbUseSystemProxy.setOnCheckedChangeListener { _, isChecked ->
        useSystemProxy = isChecked
        }
                Thread {
                        while (true) {
                                try {
                                        Thread.sleep(1000)

                                        updateProxyView()

                                } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                }
                        }
                }.start()
        }
        private fun updateProxyView() {
                runOnUiThread {
                        if (isMyServiceRunning(HttpProxyService::class.java)) {
                                btnStart.visibility = GONE
                                btnStop.visibility = VISIBLE
                                etPort.isEnabled = false
                                cbUseSystemProxy.isEnabled = false
                                status.text = getString(R.string.proxy_is_running)
                                status.setBackgroundColor(ContextCompat.getColor(this, R.color.green_banner_70))

                        } else {
                                btnStart.visibility = VISIBLE
                                btnStop.visibility = GONE
                                etPort.isEnabled = true
                                cbUseSystemProxy.isEnabled = true
                                status.text = getString(R.string.proxy_not_running)
                                status.setBackgroundColor(ContextCompat.getColor(this, R.color.red))

                        }
                        val mport: String = etPort.text.toString()
                        ports.text = "Port: "+ mport
                }
        }

        private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
                val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                        if (serviceClass.name == service.service.className) {
                                return true
                        }
                }
                return false
        }




        override fun onDestroy() {
        super.onDestroy()
         }
        }
