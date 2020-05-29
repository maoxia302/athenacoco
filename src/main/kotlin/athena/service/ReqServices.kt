package athena.service

import athena.core.mappers.AthenaCoreLinkRepository
import athena.core.repo.AssetCoreLink
import athena.core.repo.TransformRawParties
import athena.repository.CommandTrade
import athena.repository.Logger
import athena.repository.RawParties
import athena.repository.RawPartiesRepository
import athena.socket.core.Initiation
import athena.socket.core.MessageContext
import athena.socket.manager.DataProcess
import athena.starter.BootProperties
import athena.tools.MiniCommonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
        private val t: ExecutorService = Executors.newFixedThreadPool(65535)
    }

    @Autowired
    private val commandTrade: CommandTrade? = null

    @Autowired
    private var rawPartiesRepository: RawPartiesRepository? = null

    @Autowired
    private var athenaCoreLinkRepository: AthenaCoreLinkRepository? = null

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
            val content = cypher.content.toString()
            println("content:::$content")
            if (content.isNotBlank()) {
                val rawParties = RawParties.parseMessageContext(content)
                if (rawParties != null) {
                    return RawParties.convertToMessageContext(rawParties, content, cypher.address)
                }
            }
            return null
        }

        fun extraction(linkId: String) : AssetCoreLink? {
            return athenaCoreLinkRepository!!.getMainAthenaCoreLink(linkId, 1)
        }

        fun fragment(made: String) : AssetCoreLink? {
            return athenaCoreLinkRepository!!.getMainAthenaCoreLink(made, 0)
        }

        fun makeFragmentCypher(mCoreLink: AssetCoreLink) : String {
            return TransformRawParties.toCypher(mCoreLink)
        }

        mainLoop@while (true) {
            try {
                val data = MESSAGE_TUBE.take()
                if (data.content.toString().startsWith("<KA>")) {
                    // EXT: pulse info management
                    println(data.content.toString())
                    continue@mainLoop
                }
                val parsedData = parseHead(data)
                if (parsedData != null) {
                    /*
                        CORE PART 1: GET MAIN CONFIG INFO & PUBLIC_ID  <KB>AssetCoreLink.cypher<KB>
                     */
                    if (data.content.toString().startsWith("<KB>")) {
                        val mCoreLink = fragment(parsedData.protocolName)
                        if (Objects.isNull(mCoreLink)) {
                            parsedData.content = "<QUIET>"
                        } else {
                            parsedData.content = "<KB>${makeFragmentCypher(mCoreLink!!)}<KB>"
                        }
                    } else {
                        /*
                            CORE PART 2: fetch for mappings of version.
                        */
                        val main = extraction(parsedData.protocolName)
                        if (main == null) {
                            parsedData.content = "<QUIET>"
                        } else {
                            // val version = rawPartiesRepository!!.findPartyByPartyName(main.applicationCode!!)
                            val rawParty = TransformRawParties.toRawParties(main)
                            val version = rawParty!!.directory //main version check
                            if (version != null && version > parsedData.gateConnectId) {
                                parsedData.content = "<RENEW>${BootProperties.DOWNLOAD}"
                            } else {
                                parsedData.content = "<QUIET>"
                            }
                        }
                    }
                    DataProcess.send(parsedData)
                    val l = Logger(
                        lvl = "main_log",
                        tag = "FileInteractionService.fileInteractionProcess",
                        sign = "MessageContext|RawParties",
                        content = parsedData.toStringPlay(),
                        time = MiniCommonUtils.currentDateTimeStr(),
                        millis = MiniCommonUtils.currentMillis()
                    )
                    println(l.toString())
                    /*
                        use this to send files directly
                        Thread { commandTrade!!.trade(parsedData) }.start()
                    */
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
                println("DataProcessReceived::$data")
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
