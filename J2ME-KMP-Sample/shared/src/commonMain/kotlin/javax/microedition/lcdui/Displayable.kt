package javax.microedition.lcdui

/**
 * Lớp cơ sở Displayable cho toàn bộ UI của J2ME (Canvas, Form, List...)
 */
abstract class Displayable {
    
    private var title: String? = null
    private var ticker: Ticker? = null

    fun getTitle(): String? = title
    fun setTitle(s: String?) {
        title = s
    }

    fun getTicker(): Ticker? = ticker
    fun setTicker(t: Ticker?) {
        ticker = t
    }

    // Các hàm xử lý size change (thường gọi khi xoay màn hình)
    protected open fun sizeChanged(w: Int, h: Int) {}

    fun getWidth(): Int = 240 // Placeholder, thực tế sẽ lấy từ màn hình thiết bị
    fun getHeight(): Int = 320 // Placeholder
}

/**
 * Một class Ticker đơn giản (dòng chữ chạy)
 */
class Ticker(val text: String)
