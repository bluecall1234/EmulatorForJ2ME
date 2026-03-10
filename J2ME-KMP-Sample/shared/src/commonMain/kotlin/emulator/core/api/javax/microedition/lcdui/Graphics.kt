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
    const val SOLID = 0
    const val DOTTED = 1

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

    /**
     * javax.microedition.lcdui.Graphics.drawRect(IIII)V
     */
    fun drawRect(frame: ExecutionFrame) {
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
                drawRectOutlineToBuffer(pixels, imgW, imgH, x + translateX, y + translateY, w, h, color)
            }
        } else {
            NativeGraphicsBridge.drawRect(x + translateX, y + translateY, w, h, color)
        }
    }

    private fun drawRectOutlineToBuffer(pixels: IntArray, imgW: Int, imgH: Int, x: Int, y: Int, w: Int, h: Int, color: Int) {
        // Top and Bottom
        for (ix in x..x + w) {
            if (ix !in 0 until imgW) continue
            if (y in 0 until imgH) pixels[y * imgW + ix] = color
            if (y + h in 0 until imgH) pixels[(y + h) * imgW + ix] = color
        }
        // Left and Right
        for (iy in y..y + h) {
            if (iy !in 0 until imgH) continue
            if (x in 0 until imgW) pixels[iy * imgW + x] = color
            if (x + w in 0 until imgW) pixels[iy * imgW + (x + w)] = color
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.drawLine(IIII)V
     */
    fun drawLine(frame: ExecutionFrame) {
        val y2 = frame.popInt()
        val x2 = frame.popInt()
        val y1 = frame.popInt()
        val x1 = frame.popInt()
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
                drawLineToBuffer(pixels, imgW, imgH, x1 + translateX, y1 + translateY, x2 + translateX, y2 + translateY, color)
            }
        } else {
            NativeGraphicsBridge.drawLine(x1 + translateX, y1 + translateY, x2 + translateX, y2 + translateY, color)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.drawArc(IIIIII)V
     */
    fun drawArc(frame: ExecutionFrame) {
        val arcAngle = frame.popInt()
        val startAngle = frame.popInt()
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
            // Stub for buffer path
        } else {
            NativeGraphicsBridge.drawArc(x + translateX, y + translateY, w, h, startAngle, arcAngle, color, false)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.fillArc(IIIIII)V
     */
    fun fillArc(frame: ExecutionFrame) {
        val arcAngle = frame.popInt()
        val startAngle = frame.popInt()
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
            // Stub for buffer path
        } else {
            NativeGraphicsBridge.drawArc(x + translateX, y + translateY, w, h, startAngle, arcAngle, color, true)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.drawRoundRect(IIIIII)V
     */
    fun drawRoundRect(frame: ExecutionFrame) {
        val arcH = frame.popInt()
        val arcW = frame.popInt()
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
            // Stub for buffer path
        } else {
            NativeGraphicsBridge.drawRoundRect(x + translateX, y + translateY, w, h, arcW, arcH, color, false)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.fillRoundRect(IIIIII)V
     */
    fun fillRoundRect(frame: ExecutionFrame) {
        val arcH = frame.popInt()
        val arcW = frame.popInt()
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
            // Stub for buffer path
        } else {
            NativeGraphicsBridge.drawRoundRect(x + translateX, y + translateY, w, h, arcW, arcH, color, true)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.drawTriangle(IIIIII)V
     */
    fun drawTriangle(frame: ExecutionFrame) {
        val y3 = frame.popInt()
        val x3 = frame.popInt()
        val y2 = frame.popInt()
        val x2 = frame.popInt()
        val y1 = frame.popInt()
        val x1 = frame.popInt()
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
                drawLineToBuffer(pixels, imgW, imgH, x1 + translateX, y1 + translateY, x2 + translateX, y2 + translateY, color)
                drawLineToBuffer(pixels, imgW, imgH, x2 + translateX, y2 + translateY, x3 + translateX, y3 + translateY, color)
                drawLineToBuffer(pixels, imgW, imgH, x3 + translateX, y3 + translateY, x1 + translateX, y1 + translateY, color)
            }
        } else {
            NativeGraphicsBridge.drawLine(x1 + translateX, y1 + translateY, x2 + translateX, y2 + translateY, color)
            NativeGraphicsBridge.drawLine(x2 + translateX, y2 + translateY, x3 + translateX, y3 + translateY, color)
            NativeGraphicsBridge.drawLine(x3 + translateX, y3 + translateY, x1 + translateX, y1 + translateY, color)
        }
    }

    /**
     * javax.microedition.lcdui.Graphics.fillTriangle(IIIIII)V
     */
    fun fillTriangle(frame: ExecutionFrame) {
        val y3 = frame.popInt()
        val x3 = frame.popInt()
        val y2 = frame.popInt()
        val x2 = frame.popInt()
        val y1 = frame.popInt()
        val x1 = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject
        // Stub for now
    }

    private fun drawLineToBuffer(pixels: IntArray, imgW: Int, imgH: Int, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        // Simple Bresenham's line algorithm
        var cx = x1
        var cy = y1
        val dx = kotlin.math.abs(x2 - x1)
        val dy = kotlin.math.abs(y2 - y1)
        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1
        var err = dx - dy

        while (true) {
            if (cx in 0 until imgW && cy in 0 until imgH) {
                pixels[cy * imgW + cx] = color
            }
            if (cx == x2 && cy == y2) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                cx += sx
            }
            if (e2 < dx) {
                err += dx
                cy += sy
            }
        }
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
                if (obj.className == "java/lang/String") {
                    obj.instanceFields["value"] as? String ?: ""
                } else {
                    ""
                }
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

    /**
     * javax.microedition.lcdui.Graphics.clipRect(IIII)V
     */
    fun clipRect(frame: ExecutionFrame) {
        val h = frame.popInt()
        val w = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject

        val translateX = thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0
        val translateY = thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0

        val oldX = thisGraphics?.instanceFields?.get("clipX:I") as? Int ?: 0
        val oldY = thisGraphics?.instanceFields?.get("clipY:I") as? Int ?: 0
        val oldW = thisGraphics?.instanceFields?.get("clipW:I") as? Int ?: 240
        val oldH = thisGraphics?.instanceFields?.get("clipH:I") as? Int ?: 320

        // Intersection
        val newX = kotlin.math.max(oldX, x)
        val newY = kotlin.math.max(oldY, y)
        val newW = kotlin.math.min(oldX + oldW, x + w) - newX
        val newH = kotlin.math.min(oldY + oldH, y + h) - newY

        thisGraphics?.instanceFields?.put("clipX:I", newX)
        thisGraphics?.instanceFields?.put("clipY:I", newY)
        thisGraphics?.instanceFields?.put("clipW:I", if (newW < 0) 0 else newW)
        thisGraphics?.instanceFields?.put("clipH:I", if (newH < 0) 0 else newH)

        NativeGraphicsBridge.setClip(newX + translateX, newY + translateY, newW, newH)
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

    fun getColor(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0xFF000000.toInt()
        frame.push(color and 0xFFFFFF)
    }

    fun getRedComponent(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0
        frame.push((color shr 16) and 0xFF)
    }

    fun getGreenComponent(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0
        frame.push((color shr 8) and 0xFF)
    }

    fun getBlueComponent(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0
        frame.push(color and 0xFF)
    }

    fun getGrayScale(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        frame.push((r + g + b) / 3)
    }

    fun setGrayScale(frame: ExecutionFrame) {
        val value = frame.popInt() and 0xFF
        val thisGraphics = frame.pop() as? HeapObject
        val packed = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
        thisGraphics?.instanceFields?.put("color:I", packed)
    }

    fun getTranslateX(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        frame.push(thisGraphics?.instanceFields?.get("translateX:I") as? Int ?: 0)
    }

    fun getTranslateY(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        frame.push(thisGraphics?.instanceFields?.get("translateY:I") as? Int ?: 0)
    }

    fun getStrokeStyle(frame: ExecutionFrame) {
        val thisGraphics = frame.pop() as? HeapObject
        frame.push(thisGraphics?.instanceFields?.get("strokeStyle:I") as? Int ?: SOLID)
    }

    fun setStrokeStyle(frame: ExecutionFrame) {
        val style = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject
        thisGraphics?.instanceFields?.put("strokeStyle:I", style)
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

    /**
     * javax.microedition.lcdui.Graphics.drawRGB([IIIIIIIZ)V
     */
    fun drawRGB(frame: ExecutionFrame) {
        val processAlpha = frame.popInt() != 0
        val height = frame.popInt()
        val width = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val scanlength = frame.popInt()
        val offset = frame.popInt()
        val rgbData = frame.pop() as? IntArray ?: IntArray(0)
        val thisGraphics = frame.pop() as? HeapObject

        if (thisGraphics == null) return
        val translateX = thisGraphics.instanceFields["translateX:I"] as? Int ?: 0
        val translateY = thisGraphics.instanceFields["translateY:I"] as? Int ?: 0

        val targetImage = thisGraphics.instanceFields["_targetImage"] as? HeapObject
        if (targetImage != null) {
            val destPixels = targetImage.instanceFields["rgb:[I"] as? IntArray
            val destW = targetImage.instanceFields["width:I"] as? Int ?: 0
            val destH = targetImage.instanceFields["height:I"] as? Int ?: 0
            if (destPixels != null) {
                drawRGBToBuffer(rgbData, offset, scanlength, x + translateX, y + translateY, width, height, processAlpha, destPixels, destW, destH)
            }
        } else {
            // Screen path: create a slice of pixels and draw
            val subPixels = IntArray(width * height)
            for (iy in 0 until height) {
                for (ix in 0 until width) {
                    val srcIdx = offset + iy * scanlength + ix
                    if (srcIdx in rgbData.indices) {
                        var pixel = rgbData[srcIdx]
                        if (!processAlpha) pixel = pixel or 0xFF000000.toInt()
                        subPixels[iy * width + ix] = pixel
                    }
                }
            }
            NativeGraphicsBridge.drawImage(subPixels, width, height, x + translateX, y + translateY, 20) // 20 = TOP | LEFT
        }
    }

    private fun drawRGBToBuffer(src: IntArray, offset: Int, scan: Int, dx: Int, dy: Int, w: Int, h: Int, alpha: Boolean, dest: IntArray, dw: Int, dh: Int) {
        for (iy in 0 until h) {
            val dyy = dy + iy
            if (dyy < 0 || dyy >= dh) continue
            for (ix in 0 until w) {
                val dxx = dx + ix
                if (dxx < 0 || dxx >= dw) continue
                val srcIdx = offset + iy * scan + ix
                if (srcIdx in src.indices) {
                    var pixel = src[srcIdx]
                    if (!alpha) pixel = pixel or 0xFF000000.toInt()
                    if (((pixel shr 24) and 0xFF) > 0) {
                        dest[dyy * dw + dxx] = pixel
                    }
                }
            }
        }
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

