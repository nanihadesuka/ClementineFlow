package my.nanihadesuka.clementineflow.backend

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import my.nanihadesuka.clementineflow.backend.pb.Remotecontrolmessages
import my.nanihadesuka.clementineflow.ui.utils.utils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

@ExperimentalCoroutinesApi
class RemoteConnection(
    private val messagesFlow: MessagesFlow,
    val IP: String,
    val port: Int,
    val password: Int?
)
{
    companion object {
        var gcount = 0
    }

    val count = gcount
    init
    {
        gcount += 1
    }

    private lateinit var socket: Socket
    private lateinit var dataIn: DataInputStream
    private lateinit var dataOut: DataOutputStream

    private val jobsScope = CoroutineScope(Dispatchers.IO)

    private val disconnectJob  = CoroutineScope(Dispatchers.IO).launch(start = CoroutineStart.LAZY) {
        runCatching {
            sendMessageSync(MessageBuilder.disconnect())
            jobsScope.cancel()
            reconnectRoutineJob.join()
            connectionJob.join()
        }
    }

    suspend fun disconnect()
    {
        disconnectJob.start()
        disconnectJob.join()
    }

    private suspend fun getMessage(): Remotecontrolmessages.Message?
    {
        return runCatching {
            val len = dataIn.readInt()
            val data = ByteArray(len)
            dataIn.readFully(data, 0, len)
            return Remotecontrolmessages.Message.parseFrom(data)
        }.getOrDefault(null)
    }

    suspend fun sendMessageSync(message: Remotecontrolmessages.Message)
    {
        val data: ByteArray = message.toByteArray()
        runCatching {
            dataOut.writeInt(data.size)
            dataOut.write(data)
            dataOut.flush()
        }
        utils.log("message sent sync ($count)", message.type.name)
    }

    fun sendMessage(message: Remotecontrolmessages.Message) = jobsScope.launch { sendMessageSync(message = message) }

    private var connectionJob: Job = createConnectionJob()

    private fun createConnectionJob() = jobsScope.launch {
        messagesFlow.socketConnectionError.emitter.emit(MessagesFlow.SocketConnectionError.NONE)
        socket = Socket()
        socket.soTimeout = 5 * 1000
        run {
            for (tries in 1..Int.MAX_VALUE)
            {
                if (::dataOut.isInitialized) runCatching { dataOut.close() }
                if (::dataIn.isInitialized) runCatching { dataIn.close() }

                val res = runCatching {
                    messagesFlow.socketConnectionState.emitter.emit(MessagesFlow.SocketConnectionState.CONNECTING)
                    socket.connect(InetSocketAddress(IP, port), 3000)
                    if (!isActive) return@run
                    dataIn = DataInputStream(socket.getInputStream())
                    dataOut = DataOutputStream(socket.getOutputStream().buffered())
                }

                if (res.isSuccess)
                    break

                val maxTries = 5
                if (tries >= maxTries)
                {
                    messagesFlow.socketConnectionError.emitter.emit(MessagesFlow.SocketConnectionError.ERROR)
                    utils.log("connectionJob ($count)", "Failed to connect times: $tries/$maxTries, max tries reached. Stopped trying.")
                    return@run
                }

                val reconnectWaitTime = 5.toLong()
                utils.log("connectionJob ($count)", "Failed to connect, trying to connect again in $reconnectWaitTime seconds. Try $tries/$maxTries")
                delay(reconnectWaitTime * 1000)
                if (!isActive)
                    return@run
            }

            messagesFlow.socketConnectionState.emitter.emit(MessagesFlow.SocketConnectionState.CONNECTED)
            sendMessage(MessageBuilder.requestConnect(password = password))

            while (isActive)
            {
                try
                {
                    val message = getMessage()
                    if (isActive && message != null)
                    {
                        utils.log("RECEVIED MESSAGE TYPE ($count)", message.type.name)
                        jobsScope.launch(Dispatchers.Default) { messagesFlow.pipeMessage(message) }
                    }
                } catch (e: TimeoutException)
                {
                    utils.log("getMessage ($count)", "TimeoutException")
                } catch (e: Exception)
                {
                    utils.log("getMessage ($count)", "Error occurred\n" + e.printStackTrace())
                }
            }
        }

        utils.log("connectionJob ($count)", "------ JOB ENDED -------")
        messagesFlow.socketConnectionState.emitter.emit(MessagesFlow.SocketConnectionState.DISCONNECTED)
        if (::dataIn.isInitialized) runCatching { dataIn.close() }
        if (::dataOut.isInitialized) runCatching { dataOut.close() }
        if (::socket.isInitialized) runCatching { socket.close() }
    }

    private val reconnectRoutineJob = jobsScope.launch {
        messagesFlow
            .clementineConnectionState
            .filter { it == MessagesFlow.ClementineConnectionState.DEAD }
            .collect {
                delay(1000 * 4)
                utils.log("RECONNECT ($count)", "Trying to restart server-client connection")
                runCatching {
                    connectionJob.cancel()
                    connectionJob.join()
                    if (isActive)
                        createConnectionJob()
                }
            }
    }
}
