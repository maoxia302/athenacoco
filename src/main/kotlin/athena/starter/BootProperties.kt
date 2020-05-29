package athena.starter

import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionFactoryBean
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.HashMap
import javax.persistence.Entity
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

class BootProperties {

    companion object {
        const val TITLE: String = "ATHENA-MULTI-PASS"
        const val BANNER: String = "( ͡° ͜ʖ ͡°)"
        const val PORT: Int = 8888
        const val DEFAULT_SOCKET_PORT: Int = 9530
        const val FILE_LOC = "C:\\lulu\\"
        const val DOWNLOAD = "http://40.73.119.13:8080/luluthecat.apk"
        @JvmField
        var FILE_BYTES: MutableMap<String, ByteArray> = mutableMapOf()
        @JvmField
        var FILE_CURRENT_PACKS: MutableMap<String, List<Array<Byte>>> = mutableMapOf()
    }
}

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "cacheDataManagerFactory",
    transactionManagerRef = "cacheDataTransactionManager",
    basePackages = ["athena.repository"]
)
@EntityScan("athena.repository")
class CacheDataSourceConfigs {

    @Value("\${spring.cache-datasource.jpa.hibernate.ddl-auto}")
    private val ddlAuto: String? = null
    @Value("\${spring.cache-datasource.jpa.database-platform}")
    private val dialect: String? = null

    @Primary
    @Bean(name = ["cacheDataSource"])
    @ConfigurationProperties(prefix = "spring.cache-datasource")
    fun cacheDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Primary
    @Bean(name = ["cacheDataManagerFactory"])
    fun cacheDataManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean {
        val properties = HashMap<String, Any>()
        properties["hibernate.hbm2ddl.auto"] = this.ddlAuto!!
        properties["hibernate.dialect"] = this.dialect!!
        return builder
            .dataSource(cacheDataSource())
            .properties(properties)
            .packages("athena.repository")
            .build()
    }

    @Primary
    @Bean(name = ["cacheDataTransactionManager"])
    fun cacheDataTransactionManager(@Qualifier("cacheDataManagerFactory") entityManagerFactory: EntityManagerFactory)
            : PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "assetsCoreLinkManagerFactory",
    transactionManagerRef = "assetsCoreLinkTransactionManager",
    basePackages = ["athena.core.repo"]
)
class AssetCoreSourceConfigs {

    @Value("\${spring.postgres-datasource.jpa.hibernate.ddl-auto}")
    private val ddlAuto: String? = null
    @Value("\${spring.postgres-datasource.jpa.database-platform}")
    private val dialect: String? = null

    @Bean(name = ["assetsCoreLinkDataSource"])
    @ConfigurationProperties(prefix = "spring.postgres-datasource")
    fun assetsCoreLinkDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Bean(name = ["assetsCoreLinkManagerFactory"])
    fun assetsCoreLinkManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean {
        val properties = HashMap<String, Any>()
        properties["hibernate.hbm2ddl.auto"] = this.ddlAuto!!
        properties["hibernate.dialect"] = this.dialect!!
        return builder
            .dataSource(assetsCoreLinkDataSource())
            .properties(properties)
            .packages("athena.core.repo")
            .build()
    }

    @Bean(name = ["assetsCoreLinkTransactionManager"])
    fun assetsCoreLinkTransactionManager(
        @Qualifier("assetsCoreLinkManagerFactory") entityManagerFactory: EntityManagerFactory) : PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }

    @Bean(name = ["assetsCoreLinkSqlSessionFactory"], destroyMethod = "")
    fun assetsCoreLinkSqlSessionFactory(
        @Qualifier("assetsCoreLinkDataSource") assetsCoreLinkDataSource: DataSource) : SqlSessionFactoryBean {
        val sqlSessionFactoryBean: SqlSessionFactoryBean? = SqlSessionFactoryBean()
        sqlSessionFactoryBean?.setDataSource(assetsCoreLinkDataSource)
        val sqlSessionFactory: SqlSessionFactory? = sqlSessionFactoryBean?.getObject()
        sqlSessionFactory?.configuration!!.addMappers("athena.core.mappers")
        return sqlSessionFactoryBean
    }

}
