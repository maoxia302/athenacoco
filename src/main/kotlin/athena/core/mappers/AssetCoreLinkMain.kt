package athena.core.mappers

import athena.core.repo.AssetCoreLink
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Result
import org.apache.ibatis.annotations.Results
import org.apache.ibatis.annotations.SelectProvider
import org.springframework.stereotype.Component

object CommonWrappers {

    fun athenaCoreLinkCountAllWrapper() : LambdaQueryWrapper<AssetCoreLink> {
        return QueryWrapper<AssetCoreLink>().lambda().ge(AssetCoreLink::authorization, -1)!!
    }

    fun athenaCoreLinkPage(current: Long, size: Long, total: Long) : Page<AssetCoreLink> {
        return Page<AssetCoreLink>(current, size, total)
    }

    fun athenaCoreLinkPagedList() : LambdaQueryWrapper<AssetCoreLink>? {
        return QueryWrapper<AssetCoreLink>().lambda().ge(AssetCoreLink::authorization, -1).orderByAsc(AssetCoreLink::signLogTime)!!
    }
}

class AthenaCoreLinkMapper {

    fun fetchOne(mainId: String, d: Int) : String {
        var sql = "SELECT " +
                "pin_identity as pinIdentity, " +
                "imei_main as imeiMain, " +
                "made_code as madeCode, " +
                "application_code as applicationCode, " +
                "log_info as logInfo, " +
                "version_directory as versionDirectory, " +
                "sign_log_time as signLogTime, " +
                "\"token\" as token, " +
                "\"authorization\" as authorization\n" +
                "FROM assschema.assets_core_link"
        sql = if (d == 0 && mainId.contains("-")) {
            val pin = mainId.split("-")[0]
            val imei = mainId.split("-")[1]
            String.format("%s %s", sql, "where pin_identity = '$pin' and imei_main = '$imei'")
        } else if (d == 0 && mainId.indexOf("-") == -1) {
            val pin = mainId.split("-")[0]
            String.format("%s %s", sql, "where pin_identity = '$pin'")
        } else {
            String.format("%s %s", sql, "where made_code = '$mainId'")
        }
        return "$sql and authorization > -1"
    }

    fun countAll() : String {
        return "select count(1) as all from assschema.assets_core_link where authorization > -1"
    }

    fun getAllAthenaCoreLink(page: Int, limit: Int) : String {
        var sql = "SELECT " +
                "pin_identity as pinIdentity, " +
                "imei_main as imeiMain, " +
                "made_code as madeCode, " +
                "application_code as applicationCode, " +
                "log_info as logInfo, " +
                "version_directory as versionDirectory, " +
                "sign_log_time as signLogTime, " +
                "\"token\" as token, " +
                "\"authorization\" as authorization\n" +
                "FROM assschema.assets_core_link where authorization > -1"
        return ""
    }
}

@Mapper
interface AthenaCoreLinkRepository : BaseMapper<AssetCoreLink> {

    /**
     * d = 0 means looking for link with imei
     * d = 1 means looking for link with public_id (madeCode)
     */
    @SelectProvider(type = AthenaCoreLinkMapper::class, method = "fetchOne")
    @Results(id = "MainAthenaCoreLink", value = [
        Result(column = "pinIdentity", property = "pinIdentity"),
        Result(column = "imeiMain", property = "imeiMain"),
        Result(column = "madeCode", property = "madeCode"),
        Result(column = "applicationCode", property = "applicationCode"),
        Result(column = "logInfo", property = "logInfo", typeHandler = JsonObjectTypeHandler::class),
        Result(column = "datadate", property = "dataDate"),
        Result(column = "versionDirectory", property = "versionDirectory"),
        Result(column = "signLogTime", property = "signLogTime"),
        Result(column = "token", property = "token"),
        Result(column = "authorization", property = "authorization")
    ])
    fun getMainAthenaCoreLink(@Param("mainId") mainId: String, @Param("d") d: Int) : AssetCoreLink?
    
}