package athena.service

/**
 * MODE on cyphering:
 *
 * req:<KB><IM>IM-ID<IM></KB>
 * resp:<KB><IM>IM-ID<IM><ID>IDENTITY<ID><VN>000000<VN><MAIN>m=127.0.0.1:9688;u=127.0.0.1:9530;c=127.0.0.1:9529<MAIN></KB>
 */

const val START = "<KB>"
const val END = "</KB>"
const val IM_ID_SIGN = "<IM>"
const val ID_SIGN = "<ID>"
const val VERSION_SIGN = "<VN>"
const val MAIN_IPS = "<MAIN>"

data class MetaMan (
    val identity: String? = null,
    val meta: String? = null,
    val code: String? = null,
    val ver: String? = null
) {
    fun toCypher() : String {
        val sb: StringBuilder? = StringBuilder()
        sb!!.append(START)
            .append(IM_ID_SIGN).append(identity).append(IM_ID_SIGN)
            .append(ID_SIGN).append(meta).append(ID_SIGN)
            .append(VERSION_SIGN).append(ver).append(VERSION_SIGN)
            .append(MAIN_IPS).append(code).append(MAIN_IPS)
            .append(END)
        return sb.toString()
    }
}

class VersionMeta(cypher: String) {

    private var cypher: String? = null

    init {
        this.cypher = cypher
    }

    fun parse() : VersionMeta {
        this.cypher = this.cypher!!.replace(START, "").replace(END, "").replace(IM_ID_SIGN, "")
        //TODO get from database the MetaData for device by imei code
        return this
    }

    fun findMeta() : MetaMan? {
        return null
    }
}