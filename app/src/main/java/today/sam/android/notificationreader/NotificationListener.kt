package today.sam.android.notificationreader

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

private val excludedCategories = setOf(
        Notification.CATEGORY_PROGRESS,
        Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_TRANSPORT)
private val excludedPackages = setOf(
        // OSMAnd has it's own TTS, and does funky notifications
        "net.osmand.plus")

private val T = "NotificationListener"

// only allow a message of the same text to be repeated at most every X seconds
private val messageRepeatGap = 120

class NotificationListener : NotificationListenerService() {
    private var mQueue: TTSQueue? = null
    // Don't reannounce messages with the same text in a short time period
    private var mLastTimeSaid = HashMap<String, Long>()
    // Never reannounce a id/message pair
    private var mHaveSaid = HashMap<Pair<Int, String>, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d(T, "onCreate")
        mQueue = TTSQueue(applicationContext)
    }

    override fun onListenerConnected() {
        Log.d(T, "onListenerConnected")
        super.onListenerConnected()
    }

    private fun normalize(s: String): String {
        return s.trim()
    }

    private fun getNotificationReadout(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        val title = normalize(extras.getString("android.title") ?: "")
        val text = normalize((extras.getCharSequence("android.text") ?: "").toString())
        var pkgInfo: ApplicationInfo? = null
        try {
            pkgInfo = applicationContext.packageManager.getApplicationInfo(sbn.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(T, "Got bad package name ${sbn.packageName}")
            return null
        }
        val name = normalize(applicationContext.packageManager.getApplicationLabel(pkgInfo!!).toString())
        val message = if (name != title) "$name: $title: $text" else "$name: $text"

        if (excludedPackages.contains(sbn.packageName) || excludedCategories.contains(sbn.notification.category)) {
            Log.d(T, "Excluded ${sbn.packageName} ${sbn.notification.category}: $message")
            return null
        }
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.d(T, "Disregarding the summary: $message")
            return null
        }

        return message
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val message = getNotificationReadout(sbn)
        if (message == null) {
            Log.d(T, "Got null message for $sbn")
            return
        }

        val age = Math.abs(System.currentTimeMillis() - sbn.postTime) / 1000
        if (age > 10) {
            Log.d(T, "Disregarding ancient notification: $message")
            return
        }

        val lastTime = mLastTimeSaid[message]
        if (mHaveSaid.contains(Pair(sbn.id, message))) {
            Log.d(T, "Already announced ${sbn.id}, $message")
            return
        }
        val now = System.currentTimeMillis()
        if (lastTime != null) {
            val diffSecs = Math.abs(now - lastTime) / 1000
            if (diffSecs < messageRepeatGap) {
                mLastTimeSaid[message] = now
                Log.d(T, "Messages too close together ($diffSecs): $message")
                return
            }
        }

        mLastTimeSaid[message] = now
        mHaveSaid[Pair(sbn.id, message)] = now
        Log.d(T, "${sbn.packageName}-${sbn.id}-${sbn.tag}, ${sbn.notification.category}, ${sbn.notification.flags}: $message")
        mQueue!!.enqueue(message)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)

        val message = getNotificationReadout(sbn)
        message ?: return

        /* this stops the issue where notifications are removed then replaced with an identical version */
        mLastTimeSaid[message] = System.currentTimeMillis()
    }
}

