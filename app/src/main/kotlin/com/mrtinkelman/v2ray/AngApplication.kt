package com.mrtinkelman.v2ray

import android.content.Context
import android.content.SharedPreferences
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import androidx.work.Configuration
 import com.mrtinkelman.v2ray.util.ContextWrapper
import com.mrtinkelman.v2ray.util.LanguageHelper
import com.mrtinkelman.v2ray.util.LanguagePrefs
import com.tencent.mmkv.MMKV
import java.util.Locale

class AngApplication : MultiDexApplication(), Configuration.Provider {
    companion object {
        lateinit var application: AngApplication
        const val PREF_LAST_VERSION = "pref_last_version"
      }

    var firstRun = false
        private set

    override fun onCreate() {
        super.onCreate()

//        LeakCanary.install(this)
         val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        firstRun = defaultSharedPreferences.getInt(PREF_LAST_VERSION, 0) != BuildConfig.VERSION_CODE
        if (firstRun)
            defaultSharedPreferences.edit().putInt(PREF_LAST_VERSION, BuildConfig.VERSION_CODE).apply()

        //Logger.init().logLevel(if (BuildConfig.DEBUG) LogLevel.FULL else LogLevel.NONE)
        MMKV.initialize(this)



    }


    override fun attachBaseContext(newBase: Context) {
        val context = ContextWrapper.wrap(newBase)
        application = this
        super.attachBaseContext(context)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setDefaultProcessName("${BuildConfig.APPLICATION_ID}:bg")
            .build()
    }
}
