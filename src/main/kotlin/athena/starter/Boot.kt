package athena.starter

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.boot.web.server.ConfigurableWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component


@SpringBootApplication
@EnableJpaRepositories("athena.repository")
@EntityScan("athena.repository")
@ComponentScan(basePackages = ["athena"])
@EnableScheduling
class Boot

fun main(args: Array<String>) {
    println(BootProperties.BANNER)
    println(BootProperties.TITLE)
    runApplication<Boot>(*args) {
        setBannerMode(Banner.Mode.CONSOLE)
    }
}

@Component
class AppContainerCustomizer : WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    override fun customize(factory: ConfigurableWebServerFactory) {
        factory.setPort(BootProperties.PORT)
    }
}
