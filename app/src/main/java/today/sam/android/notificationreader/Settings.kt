package today.sam.android.notificationreader

import android.content.Context
import android.util.Log

/**
 * Created by sam on 2/5/18.
 */

private val T = "Settings"
private val PREFS_NAME = "NotificatonReaderPrefsSettings"
private val PREF_EXCLUDED_PACKAGES = "excludedPackages"
private val PREF_INCLUDED_PACKAGES = "includedPackages"

private val defaultExcludedPackages = setOf(
        // OSMAnd has it's own TTS, and does funky notifications
        "net.osmand.plus",
        // changes notification whenever you change the radio
        "com.caf.fmradio")


class Settings(context: Context) {
    private val mSettings = context.getSharedPreferences(PREFS_NAME, 0)

    fun isEnabledForPackageName(packageName: String): Boolean {
        val excluded = mSettings.getStringSet(PREF_EXCLUDED_PACKAGES, HashSet<String>())
        if (excluded.contains(packageName)) {
            return false
        }

        if (defaultExcludedPackages.contains(packageName)) {
            val included = mSettings.getStringSet(PREF_INCLUDED_PACKAGES, HashSet<String>())
            return included.contains(packageName)
        }

        return true
    }

    fun setEnabledForPackageName(packageName: String, enabled: Boolean) {
        // getStringSet returns an immutable copy
        val excluded = HashSet<String>(mSettings.getStringSet(PREF_EXCLUDED_PACKAGES, HashSet<String>()))
        val included = HashSet<String>(mSettings.getStringSet(PREF_INCLUDED_PACKAGES, HashSet<String>()))
        Log.d(T, "setEnabledForPackageName $packageName $enabled")
        if (enabled) {
            excluded.remove(packageName)
            if (defaultExcludedPackages.contains(packageName)) {
                included.add(packageName)
            }
        } else {
            excluded.add(packageName)
            included.remove(packageName)
        }
        mSettings
                .edit()
                .putStringSet(PREF_EXCLUDED_PACKAGES, excluded)
                .putStringSet(PREF_INCLUDED_PACKAGES, included)
                .commit()
        Log.d(T, "Excluded $excluded")
        Log.d(T, "Included $included")
    }
}