package com.mrtinkelman.v2ray.ui
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.viewModels
import androidx.preference.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.multiprocess.RemoteWorkManager
import com.mrtinkelman.v2ray.AppConfig
import com.mrtinkelman.v2ray.util.Utils
import com.mrtinkelman.v2ray.AngApplication
import com.mrtinkelman.v2ray.R
import com.mrtinkelman.v2ray.service.SubscriptionUpdater
import com.mrtinkelman.v2ray.viewmodel.SettingsViewModel
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.mrtinkelman.v2ray.util.LanguagePrefs
class SettingsActivity : BaseActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        title = getString(R.string.title_settings)

        settingsViewModel.startListenPreferenceChange()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val perAppProxy by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_PER_APP_PROXY) }
        private val localDns by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_LOCAL_DNS_ENABLED) }
        private val fakeDns by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_FAKE_DNS_ENABLED) }
        private val localDnsPort by lazy { findPreference<EditTextPreference>(AppConfig.PREF_LOCAL_DNS_PORT) }
        private val vpnDns by lazy { findPreference<EditTextPreference>(AppConfig.PREF_VPN_DNS) }

        private val mux by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_MUX_ENABLED) }
        private val muxConcurrency by lazy { findPreference<EditTextPreference>(AppConfig.PREF_MUX_CONCURRENCY) }
        private val muxXudpConcurrency by lazy { findPreference<EditTextPreference>(AppConfig.PREF_MUX_XUDP_CONCURRENCY) }
        private val muxXudpQuic by lazy { findPreference<ListPreference>(AppConfig.PREF_MUX_XUDP_QUIC) }


        //        val autoRestart by lazy { findPreference(PREF_AUTO_RESTART) as CheckBoxPreference }
        private val remoteDns by lazy { findPreference<EditTextPreference>(AppConfig.PREF_REMOTE_DNS) }
        private val domesticDns by lazy { findPreference<EditTextPreference>(AppConfig.PREF_DOMESTIC_DNS) }
        private val socksPort by lazy { findPreference<EditTextPreference>(AppConfig.PREF_SOCKS_PORT) }
        private val httpPort by lazy { findPreference<EditTextPreference>(AppConfig.PREF_HTTP_PORT) }
        private val routingCustom by lazy { findPreference<Preference>(AppConfig.PREF_ROUTING_CUSTOM) }
        private val autoUpdateCheck by lazy { findPreference<CheckBoxPreference>(AppConfig.SUBSCRIPTION_AUTO_UPDATE) }
        private val autoUpdateInterval by lazy { findPreference<EditTextPreference>(AppConfig.SUBSCRIPTION_AUTO_UPDATE_INTERVAL) }
        //        val licenses: Preference by lazy { findPreference(PREF_LICENSES) }
//        val feedback: Preference by lazy { findPreference(PREF_FEEDBACK) }
//        val tgGroup: Preference by lazy { findPreference(PREF_TG_GROUP) }
        private val language by lazy { findPreference<ListPreference>("pref_key_language") }
        private val mode by lazy { findPreference<ListPreference>(AppConfig.PREF_MODE) }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            addPreferencesFromResource(R.xml.pref_settings)

            routingCustom?.setOnPreferenceClickListener {
                startActivity(Intent(activity, RoutingSettingsActivity::class.java))
                false
            }

            autoUpdateCheck?.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue as Boolean
                autoUpdateCheck?.isChecked = value
                autoUpdateInterval?.isEnabled = value
                autoUpdateInterval?.text?.toLong()?.let {
                    if (newValue) configureUpdateTask(it) else cancelUpdateTask()
                }
                true
            }

            autoUpdateInterval?.setOnPreferenceChangeListener { _, any ->
                var nval = any as String

                autoUpdateInterval?.summary = nval
                // It must be greater than 15 minutes because WorkManager couldn't run tasks under 15 minutes intervals
                nval =
                    if (TextUtils.isEmpty(nval) or (nval.toLong() < 15)) AppConfig.SUBSCRIPTION_DEFAULT_UPDATE_INTERVAL else nval
                configureUpdateTask(nval.toLong())
                true
            }

//            licenses.onClick {
//                val fragment = LicensesDialogFragment.Builder(act)
//                        .setNotices(R.raw.licenses)
//                        .setIncludeOwnLicense(false)
//                        .build()
//                fragment.show((act as AppCompatActivity).supportFragmentManager, null)
//            }
//
//            feedback.onClick {
//                Utils.openUri(activity, "https://github.com/2dust/V2rayAGN/issues")
//            }
//            tgGroup.onClick {
//                //                Utils.openUri(activity, "https://t.me/v2rayN")
//                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg:resolve?domain=v2rayN"))
//                try {
//                    startActivity(intent)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    toast(R.string.toast_tg_app_not_found)
//                }
//            }

            perAppProxy?.setOnPreferenceClickListener {
                startActivity(Intent(activity, PerAppProxyActivity::class.java))
                perAppProxy?.isChecked = true
                false
            }

            remoteDns?.setOnPreferenceChangeListener { _, any ->
                // remoteDns.summary = any as String
                val nval = any as String
                remoteDns?.summary = if (nval == "") AppConfig.DNS_AGENT else nval
                true
            }
            domesticDns?.setOnPreferenceChangeListener { _, any ->
                // domesticDns.summary = any as String
                val nval = any as String
                domesticDns?.summary = if (nval == "") AppConfig.DNS_DIRECT else nval
                true
            }

            localDns?.setOnPreferenceChangeListener { _, any ->
                updateLocalDns(any as Boolean)
                true
            }
            localDnsPort?.setOnPreferenceChangeListener { _, any ->
                val nval = any as String
                localDnsPort?.summary =
                    if (TextUtils.isEmpty(nval)) AppConfig.PORT_LOCAL_DNS else nval
                true
            }
            vpnDns?.setOnPreferenceChangeListener { _, any ->
                vpnDns?.summary = any as String
                true
            }
            socksPort?.setOnPreferenceChangeListener { _, any ->
                val nval = any as String
                socksPort?.summary = if (TextUtils.isEmpty(nval)) AppConfig.PORT_SOCKS else nval
                true
            }
            httpPort?.setOnPreferenceChangeListener { _, any ->
                val nval = any as String
                httpPort?.summary = if (TextUtils.isEmpty(nval)) AppConfig.PORT_HTTP else nval
                true
            }
            mode?.setOnPreferenceChangeListener { _, newValue ->
                updateMode(newValue.toString())
                true
            }
            mode?.dialogLayoutResource = R.layout.preference_with_help_link
            //loglevel.summary = "LogLevel"
            mux?.setOnPreferenceChangeListener { _, newValue ->
                updateMux(newValue as Boolean)
                true
            }
            muxConcurrency?.setOnPreferenceChangeListener { _, newValue ->
                updateMuxConcurrency(newValue as String)
                true
            }
            muxXudpConcurrency?.setOnPreferenceChangeListener { _, newValue ->
                updateMuxXudpConcurrency(newValue as String)
                true
            }
            language?.setOnPreferenceChangeListener { _, newValue ->
                val selectedLanguage = newValue as String
                val locale = when (selectedLanguage) {
                    "en" -> Locale.ENGLISH
                    "zh-rCN" -> Locale.SIMPLIFIED_CHINESE
                    "zh-rTW" -> Locale.TRADITIONAL_CHINESE
                    // Add more cases for other languages if needed
                    else -> Locale.getDefault()
                }
                val configuration = Configuration(resources.configuration)
                configuration.setLocale(locale)
                resources.updateConfiguration(configuration, resources.displayMetrics)
                context?.let { LanguagePrefs(it) }?.setSelectedLanguage(selectedLanguage)

                recreateApp()
                true
            }
        }

        private fun recreateApp() {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            if (context is Activity) {
                (context as Activity).finish()
            }
        }


        override fun onStart() {
            super.onStart()
            val defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(requireActivity())
            updateMode(defaultSharedPreferences.getString(AppConfig.PREF_MODE, "VPN"))
            var remoteDnsString = defaultSharedPreferences.getString(AppConfig.PREF_REMOTE_DNS, "")

            domesticDns?.summary = defaultSharedPreferences.getString(AppConfig.PREF_DOMESTIC_DNS, "")
            localDnsPort?.summary = defaultSharedPreferences.getString(AppConfig.PREF_LOCAL_DNS_PORT, AppConfig.PORT_LOCAL_DNS)
            socksPort?.summary = defaultSharedPreferences.getString(AppConfig.PREF_SOCKS_PORT, AppConfig.PORT_SOCKS)
            httpPort?.summary = defaultSharedPreferences.getString(AppConfig.PREF_HTTP_PORT, AppConfig.PORT_HTTP)
            updateMux(defaultSharedPreferences.getBoolean(AppConfig.PREF_MUX_ENABLED, false))
            muxConcurrency?.summary = defaultSharedPreferences.getString(AppConfig.PREF_MUX_CONCURRENCY, "8")
            muxXudpConcurrency?.summary = defaultSharedPreferences.getString(AppConfig.PREF_MUX_XUDP_CONCURRENCY, "8")
            autoUpdateInterval?.summary = defaultSharedPreferences.getString(AppConfig.SUBSCRIPTION_AUTO_UPDATE_INTERVAL,AppConfig.SUBSCRIPTION_DEFAULT_UPDATE_INTERVAL)
            autoUpdateInterval?.isEnabled = defaultSharedPreferences.getBoolean(AppConfig.SUBSCRIPTION_AUTO_UPDATE, false)

            if (TextUtils.isEmpty(remoteDnsString)) {
                remoteDnsString = AppConfig.DNS_AGENT
            }
            if (TextUtils.isEmpty(domesticDns?.summary)) {
                domesticDns?.summary = AppConfig.DNS_DIRECT
            }
            remoteDns?.summary = remoteDnsString
            vpnDns?.summary =
                defaultSharedPreferences.getString(AppConfig.PREF_VPN_DNS, remoteDnsString)

            if (TextUtils.isEmpty(localDnsPort?.summary)) {
                localDnsPort?.summary = AppConfig.PORT_LOCAL_DNS
            }
            if (TextUtils.isEmpty(socksPort?.summary)) {
                socksPort?.summary = AppConfig.PORT_SOCKS
            }
            if (TextUtils.isEmpty(httpPort?.summary)) {
                httpPort?.summary = AppConfig.PORT_HTTP
            }
            val selectedLanguage = defaultSharedPreferences.getString("pref_key_language", "")
            val languageEntries = resources.getStringArray(R.array.language_select)
            val languageValues = resources.getStringArray(R.array.language_select_value)

            val languageIndex = languageValues.indexOf(selectedLanguage)
            if (languageIndex != -1) {
                language?.summary = languageEntries[languageIndex]
            }
        }

        private fun updateMode(mode: String?) {
            val defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(requireActivity())
            val vpn = mode == "VPN"
            perAppProxy?.isEnabled = vpn
            perAppProxy?.isChecked =
                PreferenceManager.getDefaultSharedPreferences(requireActivity())
                    .getBoolean(AppConfig.PREF_PER_APP_PROXY, false)
            localDns?.isEnabled = vpn
            fakeDns?.isEnabled = vpn
            localDnsPort?.isEnabled = vpn
            vpnDns?.isEnabled = vpn
            if (vpn) {
                updateLocalDns(
                    defaultSharedPreferences.getBoolean(
                        AppConfig.PREF_LOCAL_DNS_ENABLED,
                        false
                    )
                )
            }
        }

        private fun updateLocalDns(enabled: Boolean) {
            fakeDns?.isEnabled = enabled
            localDnsPort?.isEnabled = enabled
            vpnDns?.isEnabled = !enabled
        }

        private fun configureUpdateTask(interval: Long) {
            val rw = RemoteWorkManager.getInstance(AngApplication.application)
            rw.cancelUniqueWork(AppConfig.SUBSCRIPTION_UPDATE_TASK_NAME)
            rw.enqueueUniquePeriodicWork(
                AppConfig.SUBSCRIPTION_UPDATE_TASK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequest.Builder(
                    SubscriptionUpdater.UpdateTask::class.java,
                    interval,
                    TimeUnit.MINUTES
                )
                    .apply {
                        setInitialDelay(interval, TimeUnit.MINUTES)
                    }
                    .build()
            )
        }

        private fun cancelUpdateTask() {
            val rw = RemoteWorkManager.getInstance(AngApplication.application)
            rw.cancelUniqueWork(AppConfig.SUBSCRIPTION_UPDATE_TASK_NAME)
        }

        private fun updateMux(enabled: Boolean) {
            val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
            muxConcurrency?.isEnabled = enabled
            muxXudpConcurrency?.isEnabled = enabled
            muxXudpQuic?.isEnabled = enabled
            if (enabled) {
                updateMuxConcurrency(defaultSharedPreferences.getString(AppConfig.PREF_MUX_CONCURRENCY, "8"))
                updateMuxXudpConcurrency(defaultSharedPreferences.getString(AppConfig.PREF_MUX_XUDP_CONCURRENCY, "8"))
            }
        }

        private fun updateMuxConcurrency(value: String?) {
            if (value == null) {
            } else {
                val concurrency = value.toIntOrNull() ?: 8
                muxConcurrency?.summary = concurrency.toString()
            }
        }

        private fun updateMuxXudpConcurrency(value: String?) {
            if (value == null) {
                muxXudpQuic?.isEnabled = true
            } else {
                val concurrency = value.toIntOrNull() ?: 8
                muxXudpConcurrency?.summary = concurrency.toString()
                muxXudpQuic?.isEnabled = concurrency >= 0
            }
        }
    }

    fun onModeHelpClicked(view: View) {
        Utils.openUri(this, AppConfig.v2rayNGWikiMode)
    }
}
