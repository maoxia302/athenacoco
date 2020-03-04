package athena.service

import athena.repository.CommandTrade
import athena.repository.Logger
import athena.repository.RawParties
import athena.socket.core.Initiation
import athena.socket.core.MessageContext
import athena.socket.manager.DataProcess
import athena.starter.BootProperties
import athena.tools.CommonUtils
import athena.tools.MiniCommonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import javax.annotation.PostConstruct

/** **********************
 *  ######################
 *  THIS CLASS IS THE BRIDGE. the bridge for apps to req stuff to socket service.
 *  if this program chooses to save requested results into any storage, this must send it to a simultaneous queue.
 *  needed: overlapping time range, wait time, feed-backs to clients.
 *  ######################
 * **/
@Service
class FileInteractionService {

    companion object {
        val MESSAGE_TUBE: BlockingQueue<MessageContext> = LinkedBlockingQueue()
    }

    @Autowired
    private val commandTrade: CommandTrade? = null

    @PostConstruct
    fun initFileInteractionService() {
        //main service initiation
        try {
            initProcess(BootProperties.DEFAULT_SOCKET_PORT)   //socket server
            Thread { fileInteractionProcess() }.start()   //main handler
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun initProcess(defaultPort: Int?) {
        try {
            val p = defaultPort ?: Initiation.DEFAULT_PORT
            val dataProcess = Initiation.init(p).start()
            val threadMain = Thread(Process(dataProcess))
            threadMain.isDaemon = true
            threadMain.start()
        } catch (ex: Exception) {
            throw ex
        }
    }

    fun fileInteractionProcess() {

        fun parseHead(cypher: MessageContext) : MessageContext? {

            var content = cypher.content.toString()
            content = CommonUtils.hexStringToContent(CommonUtils.reverseHexString(content)!!)?:""
            if (!content.isBlank()) {
                val rawParties = RawParties.parseMessageContext(content)
                if (rawParties != null) {
                    return RawParties.convertToMessageContext(rawParties, content, cypher.address)
                }
            }
            return null
        }

        while (true) {
            try {
                val data = MESSAGE_TUBE.take()
                val parsedData = parseHead(data)
                if (parsedData != null) {
                    Thread { commandTrade!!.trade(parsedData) }.start()
                    val l = Logger(
                        lvl = "main_log",
                        tag = "FileInteractionService.fileInteractionProcess",
                        sign = "MessageContext|RawParties",
                        content = parsedData.toStringPlay(),
                        time = MiniCommonUtils.currentDateTimeStr(),
                        millis = MiniCommonUtils.currentMillis()
                    )
                    println(l.toString())
                }
                Thread.sleep(50)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

class Process(dataProcess: DataProcess) : Runnable {

    private var dataProcess: DataProcess? = null
    private var isEnabled: Boolean? = null
    private val lock = Object()

    init {
        this.dataProcess = dataProcess
        isEnabled = false
        if (dataProcess.socket != null) {
            isEnabled = true
        }
    }

    override fun run() {
        var gauge = 0
        mainProcess@while (true) {
            val data = dataProcess!!.receive()
            if (data != null) {
                FileInteractionService.MESSAGE_TUBE.offer(data)
                synchronized(lock) { lock.wait(50) }
                continue@mainProcess
            }
            synchronized(lock) { lock.wait(50) }
            gauge = gauge.inc()
            if (gauge >= 10) {
                synchronized(lock) { lock.wait(1000) }
                gauge = 0
            }
        }
    }

}
