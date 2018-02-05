package today.sam.android.notificationreader

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.support.v7.app.NotificationCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.TextUtils
import android.content.ComponentName



private val T = "MainActivity"

class AppListAdapter(settings: Settings, context: Context) : BaseAdapter() {
    private val mSettings = settings
    private val mPackageManager = context.packageManager
    private val mPackages = mPackageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it.loadLabel(mPackageManager).toString() }))
    private val mLayoutInflator = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int = mPackages.size

    override fun getItem(position: Int): ApplicationInfo = mPackages[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = mPackages[position]

        var view = if (convertView == null)
            mLayoutInflator.inflate(R.layout.app_list_item, parent, false)
            else convertView

        val packageName = view.findViewById<TextView>(R.id.app_list_item_packageName)
        packageName.text = item.packageName

        val label = view.findViewById<TextView>(R.id.app_list_item_label)
        label.text = item.loadLabel(mPackageManager)

        val icon = view.findViewById<ImageView>(R.id.app_list_item_icon)
        icon.setImageDrawable(item.loadIcon(mPackageManager))

        val switch = view.findViewById<Switch>(R.id.app_list_item_switch)
        // set it to null before changing the value so it isn't changed
        switch.setOnCheckedChangeListener { _, _ ->  }
        switch.isChecked = mSettings.isEnabledForPackageName(item.packageName)
        switch.setOnCheckedChangeListener { buttonView, isChecked ->
            mSettings.setEnabledForPackageName(item.packageName, isChecked)
        }

        return view
    }

}

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
