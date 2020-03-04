package athena.tools

import athena.starter.BootProperties
import java.io.File
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*


object FileObjectUtils {

    /**
     * get whole bytes if there is any
     */
    fun getFileBytes(packName: String, sign: String) : ByteArray? {
        if (packName == "") return null
        if (BootProperties.FILE_BYTES.containsKey(packName)) {
            return BootProperties.FILE_BYTES[packName]
        } else {
            val fileLoc = "${BootProperties.FILE_LOC}luluthecat${packName}${sign}.jar"
            val file = File(fileLoc)
            if (!file.exists()) return null
            val bs = file.readBytes()
            BootProperties.FILE_BYTES[packName] = bs
            return bs
        }
    }

    /**
     * split package into 2048 per pack.
     */
    fun packageSplit(packName: String, bytes: ByteArray) : List<Array<Byte>> {

        fun addIndex(indexOfPack: Int, b: Array<Byte>) : Array<Byte> {
            var lenStr = Integer.toHexString(indexOfPack)
            if (lenStr.length < 4) {
                lenStr = "0000".substring(0, 4 - lenStr.length) + lenStr
            }
            val lenBytes = CommonUtils.hexToByteArr(lenStr)
            var lb: Array<Byte> = arrayOf()
            lenBytes.forEachIndexed { i, byte -> run { lb[i] = byte } }
            lb = lb.plus(b)
            return lb
        }

        val coll: MutableList<Array<Byte>> = mutableListOf()
        val size = bytes.size
        val perPack = 2048
        val packs: Int
        packs = if (size > 2048) {
            1
        } else {
            if (size % perPack == 0) {
                size / perPack
            } else {
                size / perPack + 1
            }
        }
        var position = 0
        while (position < packs) {
            val start = position * perPack
            val end = if (size % perPack == 0) {
                start + perPack
            } else {
                if (position == packs - 1) {
                    size
                } else {
                    start + perPack
                }
            }
            val packPer = bytes.copyOfRange(start, end)
            var arr: Array<Byte> = arrayOf()
            packPer.forEachIndexed { index, byte -> arr[index] = byte }
            arr = addIndex(position, arr)
            coll.add(arr)
            position = position.inc()
        }
        BootProperties.FILE_CURRENT_PACKS[packName] = coll.toList()
        return coll.toList()
    }

}

object CommonUtils {

    fun hexToByteArr(inHex: String): ByteArray {

        fun hexToByte(inHex: String): Byte {
            return Integer.parseInt(inHex, 16).toByte()
        }

        fun isOdd(num: Int): Int {
            return num and 1
        }

        var hexStr = inHex
        val result: ByteArray
        var hexLen = inHex.length
        if (isOdd(hexLen) == 1) {
            hexLen++
            result = ByteArray(hexLen / 2)
            hexStr = "0$inHex"
        } else {
            result = ByteArray(hexLen / 2)
        }
        var j = 0
        var i = 0
        while (i < hexLen) {
            result[j] = hexToByte(hexStr.substring(i, i + 2))
            j++
            i += 2
        }
        return result
    }

    fun numberToHex(number: Int, bit: Int): String? {
        var hex = Integer.toHexString(number)
        if (hex.length < bit) {
            val len = hex.length
            for (i in 0 until bit - len) {
                hex = "0$hex"
            }
        }
        return hex.toUpperCase()
    }

    fun contentToHex(content: String) : String? {
        return String.format("%010x", BigInteger(1, content.toByteArray(StandardCharsets.UTF_8))).toUpperCase()
    }

    fun hexStringToContent(hex: String) : String? {
        val bytes = hex.toByteArray(StandardCharsets.UTF_8)
        return hexToContent(bytes)
    }

    private fun hexToContent(hex: ByteArray) : String? {
        return if (hex.size <= 5) {
            val sb = StringBuilder()
            if (hex.isNotEmpty()) {
                for (i in hex.indices.reversed()) {
                    if (hex[i].toInt() == 0) {
                        break
                    }
                    sb.append(String(byteArrayOf(hex[i]), StandardCharsets.UTF_8))
                }
            }
            sb.reverse().toString()
        } else {
            String(hex, StandardCharsets.UTF_8)
        }
    }

    fun reverseHexString(hex: String): String? {
        val rtn = StringBuilder()
        var i = hex.length - 2
        while (i >= 0) {
            rtn.append(hex.substring(i, i + 2))
            i -= 2
        }
        return rtn.toString().toUpperCase()
    }
}

class MiniCommonUtils {

    companion object {
        @JvmStatic
        fun currentDateTimeStr() : String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(System.currentTimeMillis()))
        }
        @JvmStatic
        fun currentMillis() : Long {
            return Date().time
        }
    }
}
