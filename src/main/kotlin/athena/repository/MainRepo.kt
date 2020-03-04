package athena.repository

import athena.socket.core.MessageContext
import athena.tools.MiniCommonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.*
import javax.annotation.PostConstruct
import javax.persistence.*

/**
 * logs are going to be stored in mongodb
 */
data class Logger(
    val lvl: String,
    val tag: String,
    val sign: String,
    val content: String,
    val time: String,
    val millis: Long
)

@Component
class RawPartiesManagement(@Autowired private val rawPartiesRepository: RawPartiesRepository) {

    @PostConstruct
    private fun init() {
        /**
         * the data for every client.
         * this must be initiated before the jobs.
         */
        realTimeSync()
    }

    fun realTimeSync() {
        val r = RawParties()
        r.partyName = "76D34EAF7ABB240BD25BE43AFFD6F488"
        r.authentication = "76D34EAF7ABB240BD25BE43AFFD6F488".reversed()
        r.origin = "LINDE"
        r.directory = 498601
        r.signDate = Date()
        r.signTime = MiniCommonUtils.currentMillis()
        r.token = "76D34EAF7ABB240BD25BE43AFFD6F488".hashCode()
        r.details = ""
        r.authorization = 1
        rawPartiesRepository.save(r)
    }
}

@Repository
interface RawPartiesRepository : JpaRepository<RawParties, Int> {
    @Query("select directory from RawParties where partyName = ?1")
    fun findPartyByPartyName(partyName: String) : Int?
}

@Entity
@Table(name = "RAW_PARTIES")
class RawParties {

    @Id
    @GeneratedValue
    @Column(name = "id")
    var id: Int? = null
    @Column(name = "party_name")
    var partyName: String? = null
    @Column(name = "authentication")
    var authentication: String? = null
    @Column(name = "origin")
    var origin: String? = null
    @Column(name = "directory")
    var directory: Int? = null
    @Column(name = "sign_date")
    var signDate: Date? = null
    @Column(name = "sign_time")
    var signTime: Long? = null
    @Column(name = "token")
    var token: Int? = null
    @Column(name = "details")
    var details: String? = null
    @Column(name = "authorization")
    var authorization: Int? = null

    companion object {

        private const val PARTY_NAME_FLAG: String = "<PN>"
        private const val AUTHENTICATION_FLAG: String = "<AE>"
        private const val ORIGIN_FLAG: String = "<OR>"
        private const val DIRECTORY_FLAG: String = "<DI>"
        private const val TOKEN_FLAG: String = "<TK>"
        private const val DETAILS_FLAG: String = "<DE>"
        private const val AUTHORIZATION_FLAG: String = "<AO>"

        //<PN>partyName<PN><AE>authentication<AE><OR>origin<OR><DI>directory<DI><TK>token<TK><DE>details<DE><AO>authorization<AO>
        fun parseMessageContext(cipher: String) : RawParties? {
            try {
                val rawParties = RawParties()
                var ci = cipher.substring(PARTY_NAME_FLAG.length)
                var arr = ci.split(PARTY_NAME_FLAG)
                rawParties.partyName = arr[0]
                ci = arr[1].substring(AUTHENTICATION_FLAG.length)
                arr = ci.split(AUTHENTICATION_FLAG)
                rawParties.authentication = arr[0]
                ci = arr[1].substring(ORIGIN_FLAG.length)
                arr = ci.split(ORIGIN_FLAG)
                rawParties.origin = arr[0]
                ci = arr[1].substring(DIRECTORY_FLAG.length)
                arr = ci.split(DIRECTORY_FLAG)
                rawParties.directory = arr[0].toInt()
                ci = arr[1].substring(TOKEN_FLAG.length)
                arr = ci.split(TOKEN_FLAG)
                rawParties.token = arr[0].toInt()
                ci = arr[1].substring(DETAILS_FLAG.length)
                arr = ci.split(DETAILS_FLAG)
                rawParties.details = arr[0]
                ci = arr[1].substring(AUTHORIZATION_FLAG.length).replace(AUTHORIZATION_FLAG, "")
                rawParties.authorization = ci.toInt()
                return rawParties
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return null
        }

        /**
         *  PartyName  <===>  ProtocolName            ===>  <PN><PN>
            Authentication  <===>  Content (start)    ===>  <AE><AE>
            Origin  <===>  origin                     ===>  <OR><OR>
            Directory  <===>  GateConnectId           ===>  <DI><DI>
            Token  <===>  ControlCode                 ===>  <TK><TK>
            Details  <===>  GateService               ===>  <DE><DE>
            Authorization  <===>  Priority			  ===>  <AO><AO>
         */
        fun convertToMessageContext(rawParties: RawParties, content: String, address: String) : MessageContext {
            val mc = MessageContext()
            mc.protocolName = rawParties.partyName
            mc.origin = rawParties.origin
            mc.gateConnectId = rawParties.directory?:9
            mc.controlCode = rawParties.token?:10
            mc.gateService = rawParties.details
            mc.priority = rawParties.authorization?:1
            mc.content = content
            mc.address = address
            return mc
        }
    }
}
