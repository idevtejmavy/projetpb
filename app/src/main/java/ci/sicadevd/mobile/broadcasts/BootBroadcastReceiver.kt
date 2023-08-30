package ci.sicadevd.mobile.broadcasts

import android.content.Context
import android.content.Intent
import androidx.legacy.content.WakefulBroadcastReceiver
import ci.sicadevd.mobile.tools.Commons


class BootBroadcastReceiver : WakefulBroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        Commons.synchronisation("all", context)
    }
}
