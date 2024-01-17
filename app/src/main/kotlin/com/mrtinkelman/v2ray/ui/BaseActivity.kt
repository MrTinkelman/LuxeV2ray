package com.mrtinkelman.v2ray.ui

import android.content.Context
import android.os.Build
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.mrtinkelman.v2ray.util.ContextWrapper
import com.mrtinkelman.v2ray.util.LanguagePrefs
import com.mrtinkelman.v2ray.util.MyContextWrapper
import com.mrtinkelman.v2ray.util.Utils
import java.util.Locale

abstract class BaseActivity : AppCompatActivity() {
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
    override fun attachBaseContext(newBase: Context) {
        val context = ContextWrapper.wrap(newBase)
        super.attachBaseContext(context)
    }

}
