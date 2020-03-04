package athena.control

import athena.starter.BootProperties
import athena.service.FileInteractionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
@RequestMapping("/athena")
class AthenaControllers {

    @Autowired
    private lateinit var reqService: FileInteractionService

    @GetMapping("/c")
    fun roomCheck() : String {
        println(
            BootProperties.TITLE.
                plus(BootProperties.BANNER).
                plus(BootProperties.TITLE))
        return BootProperties.TITLE
            .plus("\n${BootProperties.BANNER}\n")
    }

}

data class FeedBack (
    val code: Int,
    val result: String
)
