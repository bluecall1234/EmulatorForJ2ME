package emulator.core.api.javax.microedition.lcdui

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Implementation of J2ME's javax.microedition.lcdui.Font
 */
object Font {
    // Face constants
    const val FACE_MONOSPACE = 32
    const val FACE_PROPORTIONAL = 64
    const val FACE_SYSTEM = 0

    // Size constants
    const val SIZE_LARGE = 16
    const val SIZE_MEDIUM = 0
    const val SIZE_SMALL = 8

    // Style constants
    const val STYLE_BOLD = 1
    const val STYLE_ITALIC = 2
    const val STYLE_PLAIN = 0
    const val STYLE_UNDERLINED = 4

    /**
     * static Font.getFont(face, style, size)
     */
    fun getFont(frame: ExecutionFrame) {
        val size = frame.popInt()
        val style = frame.popInt()
        val face = frame.popInt()
        
        val fontObj = HeapObject("javax/microedition/lcdui/Font")
        fontObj.instanceFields["face:I"] = face
        fontObj.instanceFields["style:I"] = style
        fontObj.instanceFields["size:I"] = size
        
        frame.push(fontObj)
        println("[J2ME Font] getFont(face=$face, style=$style, size=$size)")
    }

    /**
     * static Font.getDefaultFont()
     */
    fun getDefaultFont(frame: ExecutionFrame) {
        val fontObj = HeapObject("javax/microedition/lcdui/Font")
        fontObj.instanceFields["face:I"] = FACE_SYSTEM
        fontObj.instanceFields["style:I"] = STYLE_PLAIN
        fontObj.instanceFields["size:I"] = SIZE_MEDIUM
        
        frame.push(fontObj)
        println("[J2ME Font] getDefaultFont()")
    }

    /**
     * Font.getHeight()I
     */
    fun getHeight(frame: ExecutionFrame) {
        val thisFont = frame.pop() as? HeapObject
        // We'll use a fixed height for now, but in the future we might want to query the bridge
        frame.push(16)
    }

    /**
     * Font.stringWidth(Ljava/lang/String;)I
     */
    fun stringWidth(frame: ExecutionFrame) {
        val strObj = frame.pop()
        frame.pop() // this
        
        val text = getTextFromObject(strObj)
        val width = NativeGraphicsBridge.measureString(text)
        frame.push(width)
    }

    /**
     * Font.substringWidth(Ljava/lang/String;II)I
     */
    fun substringWidth(frame: ExecutionFrame) {
        val len = frame.popInt()
        val offset = frame.popInt()
        val strObj = frame.pop()
        frame.pop() // this
        
        val fullText = getTextFromObject(strObj)
        val text = try {
            fullText.substring(offset, offset + len)
        } catch (e: Exception) {
            ""
        }
        val width = NativeGraphicsBridge.measureString(text)
        frame.push(width)
    }

    /**
     * Font.charWidth(C)I
     */
    fun charWidth(frame: ExecutionFrame) {
        val char = frame.popInt().toChar()
        frame.pop() // this
        
        val width = NativeGraphicsBridge.measureString(char.toString())
        frame.push(width)
    }

    /**
     * Font.charsWidth([CII)I
     */
    fun charsWidth(frame: ExecutionFrame) {
        val len = frame.popInt()
        val offset = frame.popInt()
        val chars = frame.pop() as? CharArray ?: CharArray(0)
        frame.pop() // this
        
        val text = try {
            chars.concatToString(offset, offset + len)
        } catch (e: Exception) {
            ""
        }
        val width = NativeGraphicsBridge.measureString(text)
        frame.push(width)
    }

    /**
     * Font.getFace()I
     */
    fun getFace(frame: ExecutionFrame) {
        val thisFont = frame.pop() as? HeapObject
        val face = thisFont?.instanceFields?.get("face:I") as? Int ?: FACE_SYSTEM
        frame.push(face)
    }

    /**
     * Font.getStyle()I
     */
    fun getStyle(frame: ExecutionFrame) {
        val thisFont = frame.pop() as? HeapObject
        val style = thisFont?.instanceFields?.get("style:I") as? Int ?: STYLE_PLAIN
        frame.push(style)
    }

    /**
     * Font.getSize()I
     */
    fun getSize(frame: ExecutionFrame) {
        val thisFont = frame.pop() as? HeapObject
        val size = thisFont?.instanceFields?.get("size:I") as? Int ?: SIZE_MEDIUM
        frame.push(size)
    }

    /**
     * Font.isBold()Z
     */
    fun isBold(frame: ExecutionFrame) {
        val thisFont = frame.pop() as? HeapObject
        val style = thisFont?.instanceFields?.get("style:I") as? Int ?: STYLE_PLAIN
        frame.push(if ((style and STYLE_BOLD) != 0) 1 else 0)
    }

    /**
     * Font.isItalic()Z
     */
    fun isItalic(frame: ExecutionFrame) {
        val thisFont = frame.pop() as? HeapObject
        val style = thisFont?.instanceFields?.get("style:I") as? Int ?: STYLE_PLAIN
        frame.push(if ((style and STYLE_ITALIC) != 0) 1 else 0)
    }

    /**
     * Font.isPlain()Z
     */
    fun isPlain(frame: ExecutionFrame) {
        val thisFont = frame.pop() as? HeapObject
        val style = thisFont?.instanceFields?.get("style:I") as? Int ?: STYLE_PLAIN
        frame.push(if (style == STYLE_PLAIN) 1 else 0)
    }

    /**
     * Font.isUnderlined()Z
     */
    fun isUnderlined(frame: ExecutionFrame) {
        val thisFont = frame.pop() as? HeapObject
        val style = thisFont?.instanceFields?.get("style:I") as? Int ?: STYLE_PLAIN
        frame.push(if ((style and STYLE_UNDERLINED) != 0) 1 else 0)
    }

    private fun getTextFromObject(obj: Any?): String {
        return when (obj) {
            is String -> obj
            is HeapObject -> {
                if (obj.className == "java/lang/String") {
                    obj.instanceFields["value"] as? String ?: ""
                } else {
                    ""
                }
            }
            else -> ""
        }
    }
}
