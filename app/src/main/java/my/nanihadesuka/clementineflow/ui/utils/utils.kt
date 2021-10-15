package my.nanihadesuka.clementineflow.ui.utils

import android.util.Log

object utils
{
    const val showLogs = true
    fun log(head: String, body: Any)
    {
        if (showLogs) Log.e(head, body.toString())
    }
}