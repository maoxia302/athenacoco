package athena.core.repo

import athena.repository.RawParties
import athena.tools.MiniCommonUtils
import java.util.*

data class AssetCoreLinkLog (
    val markedBy: String? = null,
    val markedWith: String? = null,
    val fallAligned: String? = null,
    val paramConfig: String? = null
)

data class AssetCoreLink (
    val pinIdentity: String? = null,
    val imeiMain: String? = null,
    val madeCode: String? = null,
    val applicationCode: String? = null,
    val logInfo: AssetCoreLinkLog? = null,
    val versionDirectory: String? = null,
    val signLogTime: String? = null, //yyyy-MM-dd HH:mm:ss[xxxxxxxxxxx]
    val token: String? = null,
    val authorization: Int? = null
)

object TransformRawParties {

    private const val PIN_IDENTITY_TAG = "<PI>"
    private const val IMEI_MAIN_TAG = "<IM>"
    private const val MADE_CODE_TAG = "<MC>"
    private const val ORIGIN_TAG = "<OR>"
    private const val APPLICATION_CODE_TAG = "<AC>"
    private const val MARKED_TAG = "<MK>"
    private const val LOG_INFO_TAG = "<LI>"
    private const val TOKEN_TAG = "<TK>"
    private const val AUTHORIZATION_TAG = "<AR>"
    private const val CONTENTS_TAG = "<U>"

    //device: first two are give, madeCode is platform gen.
    fun toRawParties(acl: AssetCoreLink?) : RawParties? {
        if (acl == null) return null
        val rp = RawParties()
        rp.origin = acl.logInfo!!.fallAligned
        rp.partyName = "$${acl.applicationCode}-${acl.pinIdentity}-${acl.imeiMain}-$${acl.madeCode}"
        rp.signDate = MiniCommonUtils.formDate(acl.signLogTime!!.split("[")[0])
        rp.signTime = acl.signLogTime.split("[")[1].trimEnd(']').toLong()
        rp.details = acl.logInfo.markedWith
        rp.authentication = acl.logInfo.markedBy
        rp.token = acl.token.hashCode()
        rp.authorization = acl.authorization
        rp.directory = acl.versionDirectory!!.toInt()
        return rp
    }

    /*
        e.g. :
        <PI>A1234567890<PI>
        <IM>1234567890<IM>
        <MC>76D34EAF7ABB240BD25BE43AFFD6F488<MC>
        <OR>LINDE<OR>
        <AC>B1Rt#TC01<AC>
        <MK>4U9D[]<MK>
        <LI><U>center_host=40.73.119.174:9688<U>indoor_host=40.73.119.174:9698<U>chain_gate=139.217.233.69:9529<U>pulse_gap=60000<U>sync_gap=600000<U><LI>
        <TK>xxxxxxxx<TK>
        <AR>200<AR>   // 200=user privilege above 200   100=user privilege below 100   130-170=user privilege in between
        format :
        <PI>pinIdentity<PI><IM>imeiMain<IM><MC>madeCode<MC><OR>logInfo.fallAligned<OR><AC>applicationCode<AC>
        <MK>logInfo.markedBy[markedWith]<MK><LI>logInfo.paramConfig<LI><TK>token<TK><AR>authorization<AR>
     */
    fun toCypher(acl: AssetCoreLink) : String {

        fun makeContents() : String {
            return "$CONTENTS_TAG${acl.logInfo!!.paramConfig!!.replace(";", CONTENTS_TAG)}$CONTENTS_TAG"
        }

        val c = StringBuilder()
        c.append(PIN_IDENTITY_TAG).append(acl.pinIdentity).append(PIN_IDENTITY_TAG)
        c.append(IMEI_MAIN_TAG).append(acl.imeiMain).append(IMEI_MAIN_TAG)
        c.append(MADE_CODE_TAG).append(acl.madeCode).append(MADE_CODE_TAG)
        c.append(ORIGIN_TAG).append(acl.logInfo!!.fallAligned).append(ORIGIN_TAG)
        c.append(APPLICATION_CODE_TAG).append(acl.applicationCode).append(APPLICATION_CODE_TAG)
        c.append(MARKED_TAG).append(acl.logInfo.markedBy).append("[").append(acl.logInfo.markedWith).append("]").append(MARKED_TAG)
        c.append(LOG_INFO_TAG).append(makeContents()).append(LOG_INFO_TAG)
        c.append(TOKEN_TAG).append(acl.token).append(TOKEN_TAG)
        c.append(AUTHORIZATION_TAG).append(acl.authorization).append(AUTHORIZATION_TAG)
        return c.toString()
    }

}
