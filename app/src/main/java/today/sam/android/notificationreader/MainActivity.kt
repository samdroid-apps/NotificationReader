package today.sam.android.notificationreader

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.app.NotificationManager
import android.content.Context
import android.support.v7.app.NotificationCompat

class MainActivity : AppCompatActivity() {
    private var mCurrentId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openSettingsButton.setOnClickListener {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        sendNotif.setOnClickListener {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif = NotificationCompat.Builder(this)
                    .setContentTitle("Some message title")
                    .setContentText("Body text")
                    .setSmallIcon(R.drawable.abc_btn_check_material)
                    .build()
            notificationManager.notify(++mCurrentId, notif)
        }
    }
}
