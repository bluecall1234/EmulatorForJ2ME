package emulator.core.api.javax.microedition.lcdui

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Implementation of J2ME's javax.microedition.lcdui.Graphics
 */
object Graphics {
    // Anchor constants
    const val HCENTER = 1
    const val VCENTER = 2
    const val LEFT = 4
    const val RIGHT = 8
    const val TOP = 16
    const val BOTTOM = 32
    const val BASELINE = 64

    /**
     * javax.microedition.lcdui.Graphics.setColor(I)V
     * Sets the current color from a packed ARGB integer.
     */
    fun setColor(frame: ExecutionFrame) {
        val rgb = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject
        // J2ME setColor ignores the alpha and forces it to opaque
        val opaqueRgb = (0xFF shl 24) or (rgb and 0xFFFFFF)
        thisGraphics?.instanceFields?.put("color:I", opaqueRgb)
        println("[J2ME Graphics] setColor(0x${rgb.toString(16).padStart(8, '0')}) -> 0x${opaqueRgb.toString(16)}")
    }

    /**
     * javax.microedition.lcdui.Graphics.setColor(III)V
     * Overload: setColor(int red, int green, int blue)
     */
    fun setColorRGB(frame: ExecutionFrame) {
        val b = frame.popInt()
        val g = frame.popInt()
        val r = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject
        val packed = (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
        thisGraphics?.instanceFields?.put("color:I", packed)
        println("[J2ME Graphics] setColorRGB($r, $g, $b) -> 0x${packed.toString(16).padStart(8, '0')}")
    }

    /**
     * javax.microedition.lcdui.Graphics.translate(II)V
     */
    fun translate(frame: ExecutionFrame) {
        val dy = frame.popInt()
        val dx = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject
        val translateX = thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0
        val translateY = thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0
        thisGraphics?.instanceFields?.put("translateX:I", translateX + dx)
        thisGraphics?.instanceFields?.put("translateY:I", translateY + dy)
    }

    /**
     * javax.microedition.lcdui.Graphics.fillRect(IIII)V
     */
    fun fillRect(frame: ExecutionFrame) {
        val h = frame.popInt()
        val w = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject

        val translateX = thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0
        val translateY = thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0xFF000000.toInt()

        val targetImage = thisGraphics?.instanceFields?.get("_targetImage") as? HeapObject
        if (targetImage != null) {
            val pixels = targetImage.instanceFields["rgb:[I"] as? IntArray
            val imgW = targetImage.instanceFields["width:I"] as? Int ?: 0
            val imgH = targetImage.instanceFields["height:I"] as? Int ?: 0
            if (pixels != null) {
                drawRectToBuffer(pixels, imgW, imgH, x + translateX, y + translateY, w, h, color)
            }
        } else {
            NativeGraphicsBridge.fillRect(x + translateX, y + translateY, w, h, color)
        }
        println("[J2ME Graphics] fillRect(x=${x + translateX}, y=${y + translateY}, w=$w, h=$h) color=0x${color.toString(16).padStart(8, '0')}")
    }

    private fun drawRectToBuffer(pixels: IntArray, imgW: Int, imgH: Int, x: Int, y: Int, w: Int, h: Int, color: Int) {
        for (iy in y until (y + h)) {
            if (iy < 0 || iy >= imgH) continue
            for (ix in x until (x + w)) {
                if (ix < 0 || ix >= imgW) continue
                pixels[iy * imgW + ix] = color
            }
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.drawImage(Ljavax/microedition/lcdui/Image;III)V
     */
    fun drawImage(frame: ExecutionFrame) {
        val anchor = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val imageObj = frame.pop() as? HeapObject
        val thisGraphics = frame.pop() as? HeapObject

        if (imageObj == null) return
        val w = imageObj.instanceFields["width:I"] as? Int ?: 0
        val h = imageObj.instanceFields["height:I"] as? Int ?: 0
        val srcPixels = imageObj.instanceFields["rgb:[I"] as? IntArray ?: return

        val translateX = thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0
        val translateY = thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0

        val targetImage = thisGraphics?.instanceFields?.get("_targetImage") as? HeapObject
        if (targetImage != null) {
            val destPixels = targetImage.instanceFields["rgb:[I"] as? IntArray
            val destW = targetImage.instanceFields["width:I"] as? Int ?: 0
            val destH = targetImage.instanceFields["height:I"] as? Int ?: 0
            if (destPixels != null) {
                drawImageToBuffer(srcPixels, w, h, destPixels, destW, destH, x + translateX, y + translateY, anchor)
            }
        } else {
            NativeGraphicsBridge.drawImage(srcPixels, w, h, x + translateX, y + translateY, anchor)
        }
        println("[J2ME Graphics] drawImage(w=$w, h=$h, x=${x + translateX}, y=${y + translateY}, anchor=$anchor)")
    }

    private fun drawImageToBuffer(src: IntArray, sw: Int, sh: Int, dest: IntArray, dw: Int, dh: Int, x: Int, y: Int, anchor: Int) {
        var drawX = x
        var drawY = y

        // Horizontal
        if ((anchor and HCENTER) != 0) drawX -= sw / 2
        else if ((anchor and RIGHT) != 0) drawX -= sw
        
        // Vertical
        if ((anchor and BOTTOM) != 0) drawY -= sh
        else if ((anchor and VCENTER) != 0) drawY -= sh / 2
        // TOP is default (16)

        for (iy in 0 until sh) {
            val dy = drawY + iy
            if (dy < 0 || dy >= dh) continue
            for (ix in 0 until sw) {
                val dx = drawX + ix
                if (dx < 0 || dx >= dw) continue
                val pixel = src[iy * sw + ix]
                if (((pixel shr 24) and 0xFF) > 0) { // Alpha check
                    dest[dy * dw + dx] = pixel
                }
            }
        }
    }

    private fun getStringValue(obj: Any?): String {
        return when (obj) {
            is String -> obj
            is HeapObject -> {
                val chars = obj.instanceFields["value:[C"] as? CharArray
                chars?.concatToString() ?: ""
            }
            else -> ""
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.drawString:(Ljava/lang/String;III)V
     */
    fun drawString(frame: ExecutionFrame) {
        val anchor = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val strObj = frame.pop()
        val thisGraphics = frame.pop() as? HeapObject
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0
        
        val translateX = thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0
        val translateY = thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0

        val text = getStringValue(strObj)
        println("[Graphics] drawString: text=\"$text\" x=$x y=$y anchor=$anchor color=0x${color.toString(16)}")

        val targetImage = thisGraphics?.instanceFields?.get("_targetImage") as? HeapObject
        if (targetImage != null) {
            val pixels = targetImage.instanceFields["rgb:[I"] as? IntArray
            val imgW = targetImage.instanceFields["width:I"] as? Int ?: 0
            val imgH = targetImage.instanceFields["height:I"] as? Int ?: 0
            if (pixels != null) {
                NativeGraphicsBridge.drawTextToBuffer(pixels, imgW, imgH, text, x + translateX, y + translateY, anchor, color)
            }
        } else {
            NativeGraphicsBridge.drawString(text, x + translateX, y + translateY, anchor, color)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.drawSubstring:(Ljava/lang/String;IIIII)V
     */
    fun drawSubstring(frame: ExecutionFrame) {
        val anchor = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val len = frame.popInt()
        val offset = frame.popInt()
        val strObj = frame.pop()
        val thisGraphics = frame.pop() as? HeapObject
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0
        
        val translateX = thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0
        val translateY = thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0

        val fullText = getStringValue(strObj)
        val text = try {
            fullText.substring(offset, offset + len)
        } catch (e: Exception) {
            println("  [Graphics] drawSubstring error: offset=$offset, len=$len, fullTextLength=${fullText.length}")
            ""
        }
        println("[Graphics] drawSubstring: text=\"$text\" x=$x y=$y anchor=$anchor color=0x${color.toString(16)}")

        val targetImage = thisGraphics?.instanceFields?.get("_targetImage") as? HeapObject
        if (targetImage != null) {
            val pixels = targetImage.instanceFields["rgb:[I"] as? IntArray
            val imgW = targetImage.instanceFields["width:I"] as? Int ?: 0
            val imgH = targetImage.instanceFields["height:I"] as? Int ?: 0
            if (pixels != null) {
                NativeGraphicsBridge.drawTextToBuffer(pixels, imgW, imgH, text, x + translateX, y + translateY, anchor, color)
            }
        } else {
            NativeGraphicsBridge.drawString(text, x + translateX, y + translateY, anchor, color)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.drawChar:(CIII)V
     */
    fun drawChar(frame: ExecutionFrame) {
        val anchor = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val char = frame.popInt().toChar()
        val thisGraphics = frame.pop() as? HeapObject
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0
        val translateX = thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0
        val translateY = thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0

        val text = char.toString()
        val targetImage = thisGraphics?.instanceFields?.get("_targetImage") as? HeapObject
        if (targetImage != null) {
            val pixels = targetImage.instanceFields["rgb:[I"] as? IntArray
            val imgW = targetImage.instanceFields["width:I"] as? Int ?: 0
            val imgH = targetImage.instanceFields["height:I"] as? Int ?: 0
            if (pixels != null) {
                NativeGraphicsBridge.drawTextToBuffer(pixels, imgW, imgH, text, x + translateX, y + translateY, anchor, color)
            }
        } else {
            NativeGraphicsBridge.drawString(text, x + translateX, y + translateY, anchor, color)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.drawChars:([CIIIII)V
     */
    fun drawChars(frame: ExecutionFrame) {
        val anchor = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val len = frame.popInt()
        val offset = frame.popInt()
        val chars = frame.pop() as? CharArray ?: CharArray(0)
        val thisGraphics = frame.pop() as? HeapObject
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0
        val translateX = thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0
        val translateY = thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0

        val text = try {
            chars.concatToString(offset, offset + len)
        } catch (e: Exception) {
            ""
        }
        
        val targetImage = thisGraphics?.instanceFields?.get("_targetImage") as? HeapObject
        if (targetImage != null) {
            val pixels = targetImage.instanceFields["rgb:[I"] as? IntArray
            val imgW = targetImage.instanceFields["width:I"] as? Int ?: 0
            val imgH = targetImage.instanceFields["height:I"] as? Int ?: 0
            if (pixels != null) {
                NativeGraphicsBridge.drawTextToBuffer(pixels, imgW, imgH, text, x + translateX, y + translateY, anchor, color)
            }
        } else {
            NativeGraphicsBridge.drawString(text, x + translateX, y + translateY, anchor, color)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.setClip(IIII)V
     */
    fun setClip(frame: ExecutionFrame) {
        val height = frame.popInt()
        val width = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject

        val translateX = thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0
        val translateY = thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0

        thisGraphics?.instanceFields?.put("clipX:I", x)
        thisGraphics?.instanceFields?.put("clipY:I", y)
        thisGraphics?.instanceFields?.put("clipW:I", width)
        thisGraphics?.instanceFields?.put("clipH:I", height)

        NativeGraphicsBridge.setClip(x + translateX, y + translateY, width, height)
    }

    fun setFont(frame: ExecutionFrame) {
        val font = frame.pop() as? HeapObject
        val thisGraphics = frame.pop() as? HeapObject
        thisGraphics?.instanceFields?.put("font:Ljavax/microedition/lcdui/Font;", font)
    }

    fun getFont(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        val font = thisGraphics?.instanceFields?.get("font:Ljavax/microedition/lcdui/Font;") as? HeapObject
            ?: HeapObject("javax/microedition/lcdui/Font")
        frame.push(font)
    }

    fun getClipX(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        frame.push(thisGraphics?.instanceFields?.get("clipX:I") as? Int ?: 0)
    }
    fun getClipY(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        frame.push(thisGraphics?.instanceFields?.get("clipY:I") as? Int ?: 0)
    }
    fun getClipWidth(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        val w = thisGraphics?.instanceFields?.get("clipW:I") as? Int
        frame.push(w ?: 240)
    }
    fun getClipHeight(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        val h = thisGraphics?.instanceFields?.get("clipH:I") as? Int
        frame.push(h ?: 320)
    }

    /**
     * javax.microedition.lcdui.Graphics.drawRegion(Ljavax/microedition/lcdui/Image;IIIIIIII)V
     */
    fun drawRegion(frame: ExecutionFrame) {
        val anchor = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val transform = frame.popInt() // Ignored for basic implementation
        val height = frame.popInt()
        val width = frame.popInt()
        val srcY = frame.popInt()
        val srcX = frame.popInt()
        val imageObj = frame.pop() as? HeapObject
        val thisGraphics = frame.pop() as? HeapObject

        if (imageObj == null || thisGraphics == null) return
        val srcPixels = imageObj.instanceFields["rgb:[I"] as? IntArray ?: return
        val srcW = imageObj.instanceFields["width:I"] as? Int ?: 0
        val srcH = imageObj.instanceFields["height:I"] as? Int ?: 0

        val translateX = thisGraphics.instanceFields["translateX:I"] as? Int ?: 0
        val translateY = thisGraphics.instanceFields["translateY:I"] as? Int ?: 0

        val destX = x + translateX
        val destY = y + translateY

        val targetImage = thisGraphics.instanceFields["_targetImage"] as? HeapObject
        if (targetImage != null) {
            val destPixels = targetImage.instanceFields["rgb:[I"] as? IntArray
            val destW = targetImage.instanceFields["width:I"] as? Int ?: 0
            val destH = targetImage.instanceFields["height:I"] as? Int ?: 0
            if (destPixels != null) {
                drawRegionToBuffer(srcPixels, srcW, srcH, srcX, srcY, width, height, destPixels, destW, destH, destX, destY, anchor)
            }
        } else {
            // Draw to screen via NativeGraphicsBridge
            // For now, we reuse drawImage with a source sub-region if NativeGraphicsBridge supports it.
            // If not, we could slice the array here.
            // Let's slice the array to be safe for now.
            val subPixels = IntArray(width * height)
            for (iy in 0 until height) {
                for (ix in 0 until width) {
                    val sy = srcY + iy
                    val sx = srcX + ix
                    if (sy in 0 until srcH && sx in 0 until srcW) {
                        subPixels[iy * width + ix] = srcPixels[sy * srcW + sx]
                    }
                }
            }
            NativeGraphicsBridge.drawImage(subPixels, width, height, destX, destY, anchor)
        }
        println("[J2ME Graphics] drawRegion(srcX=$srcX, srcY=$srcY, w=$width, h=$height, destX=$destX, destY=$destY, anchor=$anchor)")
    }

    private fun drawRegionToBuffer(src: IntArray, sw: Int, sh: Int, sx: Int, sy: Int, w: Int, h: Int, 
                                   dest: IntArray, dw: Int, dh: Int, dx: Int, dy: Int, anchor: Int) {
        var drawX = dx
        var drawY = dy

        // Horizontal
        if ((anchor and HCENTER) != 0) drawX -= w / 2
        else if ((anchor and RIGHT) != 0) drawX -= w
        
        // Vertical
        if ((anchor and BOTTOM) != 0) drawY -= h
        else if ((anchor and VCENTER) != 0) drawY -= h / 2
        // TOP is default

        for (iy in 0 until h) {
            val dyy = drawY + iy
            val syy = sy + iy
            if (dyy < 0 || dyy >= dh || syy < 0 || syy >= sh) continue
            for (ix in 0 until w) {
                val dxx = drawX + ix
                val sxx = sx + ix
                if (dxx < 0 || dxx >= dw || sxx < 0 || sxx >= sw) continue
                val pixel = src[syy * sw + sxx]
                if (((pixel shr 24) and 0xFF) > 0) {
                    dest[dyy * dw + dxx] = pixel
                }
            }
        }
    }
}

