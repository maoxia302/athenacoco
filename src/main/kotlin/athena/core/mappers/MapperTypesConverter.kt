package athena.core.mappers

import athena.core.repo.AssetCoreLink
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import java.sql.*

/**
 * This class generate a [ObjectMapper] to serialize or deserialize object or list.
 * TypeHandler would not implement this directly,
 * but can be adapted to create
 * [com.fasterxml.jackson.databind.ObjectReader] and
 * [com.fasterxml.jackson.databind.ObjectWriter] to
 * ensure thread-safe and optimize serialize or deserialize efficiency
 */
internal object MyBatisObjectMapper {
    val objectMapper = ObjectMapper().apply{
        //changes of configurations are optional:
        //example: this.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        this.registerModule(KotlinModule())
    }
}

@MappedJdbcTypes(JdbcType.VARCHAR)
class JsonObjectTypeHandler<Any>(clazz: Class<Any>) : BaseTypeHandler<Any>() {

    private val objectReader = MyBatisObjectMapper.objectMapper.readerFor(clazz)
    private val objectWriter = MyBatisObjectMapper.objectMapper.writerFor(clazz)

    private fun jsonToObject(json: String?): Any? {
        return if (!json.isNullOrBlank()) {
            objectReader.readValue(json)
        } else {
            null
        }
    }

    @Throws(SQLException::class)
    override fun setNonNullParameter(ps: PreparedStatement,
                                     i: Int,
                                     parameter: Any?,
                                     jdbcType: JdbcType
    ) {
        ps.setString(i, objectWriter.writeValueAsString(parameter))
    }

    @Throws(SQLException::class)
    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): Any? {
        val json = cs.getString(columnIndex)
        return jsonToObject(json)
    }

    @Throws(SQLException::class)
    override fun getNullableResult(rs: ResultSet, columnIndex: Int): Any? {
        val json = rs.getString(columnIndex)
        return jsonToObject(json)
    }

    @Throws(SQLException::class)
    override fun getNullableResult(rs: ResultSet, columnName: String): Any? {
        val json = rs.getString(columnName)
        return jsonToObject(json)
    }

    override fun setParameter(ps: PreparedStatement?, i: Int, parameter: Any, jdbcType: JdbcType?) {
        if (parameter == null) {
            ps?.setString(i, "")
        } else {
            super.setParameter(ps, i, parameter, jdbcType)
        }
    }
}

@MappedJdbcTypes(JdbcType.VARCHAR)
class JsonListTypeHandlerForDevice : BaseTypeHandler<List<AssetCoreLink>>() {

    private fun jsonToList(json: String?): List<AssetCoreLink>? {
        return if (!json.isNullOrBlank()) {
            val turnsType = object : TypeToken<List<AssetCoreLink>>() {}.type
            Gson().fromJson<List<AssetCoreLink>>(json, turnsType)
        } else {
            listOf()
        }
    }

    @Throws(SQLException::class)
    override fun setNonNullParameter(ps: PreparedStatement,
                                     i: Int,
                                     parameter: List<AssetCoreLink>?,
                                     jdbcType: JdbcType) {
        ps.setString(i, Gson().toJson(parameter))
    }

    @Throws(SQLException::class)
    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): List<AssetCoreLink>? {
        val json = cs.getString(columnIndex)
        return jsonToList(json)
    }

    @Throws(SQLException::class)
    override fun getNullableResult(rs: ResultSet, columnIndex: Int): List<AssetCoreLink>? {
        val json = rs.getString(columnIndex)
        return jsonToList(json)
    }

    @Throws(SQLException::class)
    override fun getNullableResult(rs: ResultSet, columnName: String): List<AssetCoreLink>? {
        val json = rs.getString(columnName)
        return jsonToList(json)
    }

}


@MappedJdbcTypes(JdbcType.ARRAY)
@MappedTypes(List::class)
class TheArrayTypeHandler : BaseTypeHandler<List<Int>>() {

    @Throws(SQLException::class)
    override fun setNonNullParameter(ps: PreparedStatement,
                                     index: Int,
                                     parameter: List<Int>?,
                                     jdbcType: JdbcType
    ) {
        val conn: Connection = ps.connection
        val array = conn.createArrayOf("integer", parameter!!.toTypedArray())
        ps.setArray(index, array)
    }

    @Throws(SQLException::class)
    override fun getNullableResult(rs: ResultSet, columnName: String) : List<Int>? {
        try {
            if (rs.getArray(columnName) == null) {
                return listOf()
            }
            return getArray(arrayOf(rs.getArray(columnName).array))
        } catch (ex: Exception) {
            return listOf()
        }
    }

    @Throws(SQLException::class)
    override fun getNullableResult(rs: ResultSet, index: Int) : List<Int>? {
        return getArray(arrayOf(rs.getArray(index).array))
    }

    @Throws(SQLException::class)
    override fun getNullableResult(callableStatement: CallableStatement, columnIndex: Int) : List<Int>? {
        return getArray(arrayOf(callableStatement.getArray(columnIndex).array))
    }

    @Throws(Exception::class)
    private fun getArray(array: Array<Any>) : List<Int>? {
        val rtn: MutableList<Int> = mutableListOf()
        (array[0] as Array<*>).asList().stream().forEach { t -> rtn.add(t.toString().toInt()) }
        return rtn.toList()
    }

}
