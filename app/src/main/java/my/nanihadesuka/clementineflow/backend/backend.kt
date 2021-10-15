package my.nanihadesuka.clementineflow.backend

import my.nanihadesuka.clementineflow.*

object backend
{
    val messagesFlow = MessagesFlow()
    var rc =  createDefaultRemoteConnection()
    fun createDefaultRemoteConnection() = RemoteConnection(
        messagesFlow = messagesFlow,
        IP = App.preferences.REMOTE_IP,
        port = App.preferences.REMOTE_PORT,
        password = if(App.preferences.REMOTE_NEEDS_AUTHCODE && App.preferences.REMOTE_AUTHCODE >= 0) App.preferences.REMOTE_AUTHCODE else null
    )
}
