package athena.repository

import athena.socket.core.MessageContext
import athena.socket.manager.DataProcess
import athena.starter.BootProperties
import athena.tools.FileObjectUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * {name: version, content: "versionID=xxx"}
 */
data class GenericModel (
    val name: String = "",
    val content: String = ""
)

data class FileObject (
    val transferIndex: Int = 0,
    val packageName: String = "",
    val pushTime: Long = 0,
    val fileBuffer: Array<Byte> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileObject

        if (transferIndex != other.transferIndex) return false
        if (packageName != other.packageName) return false
        if (pushTime != other.pushTime) return false
        if (!fileBuffer.contentEquals(other.fileBuffer)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transferIndex
        result = 31 * result + packageName.hashCode()
        result = 31 * result + pushTime.hashCode()
        result = 31 * result + fileBuffer.contentHashCode()
        return result
    }
}

@Component
class CommandTrade(@Autowired private val rawPartiesRepository: RawPartiesRepository) {

    private val t: ExecutorService = Executors.newFixedThreadPool(65535 * 2)

    /**
     * get file and send to client
     */
    fun trade(trade: MessageContext) {
        try {
            val version = rawPartiesRepository.findPartyByPartyName(trade.protocolName)
            if (version != null && version > trade.gateConnectId) {
                val tradeProcess = TradeProcess(trade)
                var future = t.submit(tradeProcess)
                var count = 0
                while (true) {
                    if (count >= 2) return
                    if (future.isDone) {
                        println(future.get().content)
                        if (future.get().content == "FAILURE") {
                            future = t.submit(tradeProcess)
                            count = count.inc()
                        } else {
                            return
                        }
                    }
                    Thread.sleep(3000)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private inner class TradeProcess(trade: MessageContext) : Callable<MessageContext> {

        private var trade: MessageContext? = null
        init {
            this.trade = trade
        }

        /**
         * to send bytes to every client.
         * bytes are from files.
         */
        override fun call(): MessageContext? {
            try {
                if (this.trade == null) return null
                val packName = this.trade!!.gateConnectId.toString()

                if (BootProperties.FILE_CURRENT_PACKS.containsKey(packName) &&
                    BootProperties.FILE_CURRENT_PACKS[packName] != null) {
                    val sendFileBytes = BootProperties.FILE_CURRENT_PACKS[packName]
                    sendFileBytes!!.forEach { e ->
                        run {
                            this.trade!!.content = e
                            DataProcess.send(this.trade!!)
                        }
                    }
                } else {
                    val sign = this.trade!!.gateService
                    val bytes = FileObjectUtils.getFileBytes(packName, sign)
                    val sendFileBytes = FileObjectUtils.packageSplit(packName, bytes!!)
                    sendFileBytes.forEach { e ->
                        run {
                            this.trade!!.content = e
                            DataProcess.send(this.trade!!)
                        }
                    }
                }
                this.trade!!.content = "SUCCESS"
                return this.trade
            } catch (ex: Exception) {
                ex.printStackTrace()
                this.trade!!.content = "FAILURE"
                return this.trade
            }
        }
    }
}
