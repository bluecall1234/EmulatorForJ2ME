package javax.microedition.lcdui

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Lớp Graphics J2ME được bọc (wrap) quanh DrawScope của Compose Multiplatform
 */
class Graphics(private val drawScope: DrawScope) {
    
    private var color: Color = Color.Black

    fun setColor(r: Int, g: Int, b: Int) {
        color = Color(r, g, b)
    }

    fun fillRect(x: Int, y: Int, width: Int, height: Int) {
        drawScope.drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat()),
            size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
        )
    }

    fun drawImage(img: Image, x: Int, y: Int, anchor: Int) {
        // Logic vẽ ảnh dùng drawScope.drawImage
    }
    
    // ... implement các hàm drawString, drawLine tương tự
}
