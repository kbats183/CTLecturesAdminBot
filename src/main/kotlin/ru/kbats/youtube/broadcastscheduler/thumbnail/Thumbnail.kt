package ru.kbats.youtube.broadcastscheduler.thumbnail

import ru.kbats.youtube.broadcastscheduler.data.LectureThumbnails
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.isReadable


object Thumbnail {
    private const val fontSize = 210
    private val fontInputStream = Thumbnail::class.java.classLoader.getResourceAsStream("Golos-Text_Bold.ttf")
    private val font = Font.createFont(Font.TRUETYPE_FONT, fontInputStream).deriveFont(fontSize.toFloat())

    fun generate(templateDir: Path, thumbnails: LectureThumbnails, lectureTitleNumber: String): File {
        val thumbnailsPath = templateDir.resolve(thumbnails.fileName)
        if (!thumbnailsPath.isReadable()) {
            throw ThumbnailGenerationException("No thumbnails template file $thumbnailsPath")
        }
        val thumbnailsGeneratePath = thumbnailsPath.resolveSibling("generate")
        Files.createDirectories(thumbnailsGeneratePath)
        val outFilePath = thumbnailsGeneratePath.resolve(thumbnails.fileName)
        generate(
            thumbnailsPath,
            getColor(thumbnails.textColor),
            lectureTitleNumber,
            outFilePath,
        )
        return outFilePath.toFile()
    }

    fun generate(templateFile: Path, color: Color?, text: String, outFilePath: Path, positionX: Int = 439, positionY: Int = 992) {
        val image: BufferedImage = ImageIO.read(Files.newInputStream(templateFile))
        val graphics = image.graphics
        graphics.font = font
        graphics.color = color ?: Color.WHITE
        graphics.drawString(text, positionX, positionY)
        ImageIO.createImageInputStream(image)
        ImageIO.write(image, "png", outFilePath.toFile())
    }

    fun getColor(string: String): Color? {
        try {
            return Color.decode(
                "0x" + when (string) {
                    "tart" -> "f9393f"
                    "honey" -> "ffcc33"
                    "yellow" -> "ffff33"
                    "green" -> "d0ff14"
                    "capri" -> "00ccff"
                    "bluetiful" -> "0b68fe"
                    "violet" -> "7f00ff"
                    "pink" -> "fc74fd"
                    else -> string
                }
            )
        } catch (e: NumberFormatException) {
            return null
        }
    }

    class ThumbnailGenerationException(override val message: String) : Exception()
}

fun main() {
    val fileName = "math_stat_3338-9 (3).png"
    Thumbnail.generate(Path.of(fileName), Color.decode("0xffee00"), "P1", Path.of("gen_$fileName"))
}
