package ci.sicadevd.mobile

import android.app.Application
import android.content.Context
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
class Application: Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        CustomActivityOnCrash.install(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()

        CaocConfig.Builder.create().apply()
    }


}