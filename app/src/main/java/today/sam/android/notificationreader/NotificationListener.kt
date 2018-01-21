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

class NotificationListener : NotificationListenerService() {
    var myQ: TTSQueue? = null
    var lastTimeSaid = HashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationListener", "onCreate")
        myQ = TTSQueue(applicationContext)
    }

    override fun onListenerConnected() {
        Log.d("NotificationListener", "onListenerConnected")
        super.onListenerConnected()
    }

    private fun normalize(s: String): String = s

    private fun getNotificationReadout(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        val title = normalize(extras.getString("android.title") ?: "")
        val text = normalize((extras.getCharSequence("android.text") ?: "").toString())
        var pkgInfo: ApplicationInfo? = null
        try {
            pkgInfo = applicationContext.packageManager.getApplicationInfo(sbn.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("NotificationListener", "Got bad package name ${sbn.packageName}")
            return null
        }
        val name = normalize(applicationContext.packageManager.getApplicationLabel(pkgInfo!!).toString())
        val message = if (name != title) "$name: $title: $text" else "$name: $text"

        if (excludedPackages.contains(sbn.packageName) || excludedCategories.contains(sbn.notification.category)) {
            Log.d("NotificationListener", "Excluded ${sbn.packageName} ${sbn.notification.category}: $message")
            return null
        }
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.d("NotificationListener", "Disregarding the summary: $message")
            return null
        }

        return message
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val message = getNotificationReadout(sbn)
        if (message == null) {
            Log.d("NotificationListener", "Got null message for $sbn")
            return
        }

        val age = Math.abs(System.currentTimeMillis() - sbn.postTime) / 1000
        if (age > 10) {
            Log.d("NotificationListener", "Disregarding ancient notification: $message")
            return
        }

        val lastTime = lastTimeSaid[message]
        val now = System.currentTimeMillis()
        if (lastTime != null) {
            val diffSecs = Math.abs(now - lastTime) / 1000
            if (diffSecs < 2) {
                Log.d("NotificationListener", "Messages too close together ($diffSecs): $message")
                return
            }
        }

        lastTimeSaid[message] = now
        Log.d("NotificationListener", "${sbn.packageName}-${sbn.id}-${sbn.tag}, ${sbn.notification.category}, ${sbn.notification.flags}: $message")
        myQ!!.enqueue(message)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)

        val message = getNotificationReadout(sbn)
        message ?: return

        /* this stops the issue where notifications are removed then replaced with an identical version */
        lastTimeSaid[message] = System.currentTimeMillis()
    }
}

