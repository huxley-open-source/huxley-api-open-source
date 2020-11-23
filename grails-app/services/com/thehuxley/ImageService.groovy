package com.thehuxley

import net.coobird.thumbnailator.Thumbnails
import org.springframework.security.crypto.codec.Hex
import org.springframework.web.multipart.commons.CommonsMultipartFile

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.security.MessageDigest

class ImageService {

    def uploadImage(String path, CommonsMultipartFile file) {
        File dir = new File(path)
        dir.mkdirs()

        def originalFilename = file.originalFilename
        def index = originalFilename.lastIndexOf('.')
        def extension = ""
        if ((index > 0) && (originalFilename.size() > index)) {
            extension = originalFilename.substring(index - 1)
        }

        def filename = new String(Hex.encode(MessageDigest.getInstance("SHA1").digest(file.bytes))) + extension
        def destFile = new File(dir, filename)

        file.transferTo(destFile)

        return destFile
    }

    def crop(String path, String filename, Integer x, Integer y, Integer width, Integer height) {
        def file = new File(path, filename)
        BufferedImage image = ImageIO.read(file).getSubimage(x, y, width, height)

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        baos.flush()

        def newFilename = new String(Hex.encode(MessageDigest.getInstance("SHA1").digest(baos.toByteArray()))) + ".png"

        ImageIO.write(image, "png", new File(path, newFilename))

        return newFilename
    }

    def getImage(String path, String key, Integer width = 0, Integer height = 0) {
        String temp = path + System.getProperty("file.separator") + "tmp" + System.getProperty("file.separator")

        def originalFile = new File(path, key)

        BufferedImage avatar = ImageIO.read(originalFile)
        def resizedFile = new File(temp, originalFile.name)
        resizedFile.mkdirs()

        if ((width > 0) && !(height > 0)) {
            height = (3 / 4) * width
        }

        if ((height > 0) && !(width > 0)) {
            width = (4 / 3) * height
        }

        if (width > 0 && height > 0) {
            if (height == width) {
                def min = Math.min(avatar.width, avatar.height)
                avatar = avatar.getSubimage(
                        (avatar.width > avatar.height ? ((avatar.width - avatar.height) / 2).abs() : 0) as Integer,
                        0,
                        min,
                        min
                )
            }
            ImageIO.write(resizeImage(avatar, width, height), "png", resizedFile)

            return resizedFile
        }


        ImageIO.write(avatar, "png", resizedFile)

        return resizedFile
    }

    def resizeImage(BufferedImage originalImage, int width, int height) {
        return Thumbnails.of(originalImage).forceSize(width, height).asBufferedImage()
    }

}
