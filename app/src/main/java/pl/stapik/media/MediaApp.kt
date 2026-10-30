package pl.stapik.media

import android.app.Application
import pl.stapik.media.di.AppContainer

class MediaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
