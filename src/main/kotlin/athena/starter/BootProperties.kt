package athena.starter

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

class BootProperties {

    companion object {
        const val TITLE: String = "ATHENA-MULTI-PASS"
        const val BANNER: String = "( ͡° ͜ʖ ͡°)"
        const val PORT: Int = 8888
        const val DEFAULT_SOCKET_PORT: Int = 9999
        const val FILE_LOC = "C:\\lulu\\"
        @JvmField
        var FILE_BYTES: MutableMap<String, ByteArray> = mutableMapOf()
        @JvmField
        var FILE_CURRENT_PACKS: MutableMap<String, List<Array<Byte>>> = mutableMapOf()
    }
}

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = ["athena.repository"]
)
class DataSourceConfigs {

    @Primary
    @Bean(name = ["dataSource"])
    @ConfigurationProperties(prefix = "spring.datasource")
    fun dataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

}
