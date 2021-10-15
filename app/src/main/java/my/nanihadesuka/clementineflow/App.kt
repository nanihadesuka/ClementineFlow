package my.nanihadesuka.clementineflow

import android.app.Application
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

class App : Application()
{
    override fun onCreate()
    {
        _instance = this
        super.onCreate()
    }

    val preferencesChangeListeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("App"))

    companion object
    {
        private lateinit var _instance: App
        val instance get() = _instance
        val cacheDir: File get() = _instance.cacheDir
        val scope get() = instance.scope
        val preferences get() = instance.appSharedPreferences()
//        fun getDatabasePath(databaseName: String): File = instance.applicationContext.getDatabasePath(databaseName)
    }
}