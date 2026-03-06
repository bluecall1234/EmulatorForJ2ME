package javax.microedition.lcdui

/**
 * Class Image của J2ME dùng chung cho cả Android và iOS
 */
class Image(val width: Int, val height: Int) {
    
    companion object {
        fun createImage(width: Int, height: Int): Image {
            return Image(width, height)
        }
        
        fun createImage(data: ByteArray, offset: Int, length: Int): Image {
            // Logic decode ảnh từ byte array
            return Image(100, 100)
        }
    }
}
