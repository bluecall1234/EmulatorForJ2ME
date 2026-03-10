package emulator.core.api.java.io

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Kotlin implementation of java.io.InputStream for the J2ME emulator.
 */
object InputStream {

    fun read(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject ?: thisObj
        
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            val pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos < data.size) {
                val b = data[pos].toInt() and 0xFF
                inputStream.instanceFields["_pos:I"] = pos + 1
                frame.push(b)
            } else {
                frame.push(-1)
            }
        } else {
            frame.push(-1)
        }
    }

    fun readArray(frame: ExecutionFrame) {
        val b = frame.pop() as? ByteArray
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject ?: thisObj
        
        if (inputStream != null && b != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            val pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos >= data.size) {
                frame.push(-1) // EOF
            } else {
                val count = minOf(b.size, data.size - pos)
                data.copyInto(b, 0, pos, pos + count)
                inputStream.instanceFields["_pos:I"] = pos + count
                frame.push(count)
            }
        } else {
            frame.push(-1)
        }
    }

    fun readArrayRegion(frame: ExecutionFrame) {
        val len = frame.popInt()
        val offset = frame.popInt()
        val b = frame.pop() as? ByteArray
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject ?: thisObj
        
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            val pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos >= data.size) {
                frame.push(-1) // EOF
            } else {
                val count = minOf(len, data.size - pos)
                if (b != null) {
                    data.copyInto(b, offset, pos, pos + count)
                }
                inputStream.instanceFields["_pos:I"] = pos + count
                frame.push(count)
            }
        } else {
            frame.push(-1)
        }
    }

    fun skip(frame: ExecutionFrame) {
        val n = frame.popLong()
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject ?: thisObj
        val data = inputStream?.instanceFields?.get("_data:[B") as? ByteArray ?: ByteArray(0)
        val pos = inputStream?.instanceFields?.get("_pos:I") as? Int ?: 0
        
        val toSkip = minOf(n, (data.size - pos).toLong()).toInt()
        inputStream?.instanceFields?.set("_pos:I", pos + toSkip)
        frame.push(toSkip.toLong())
    }

    fun available(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject ?: thisObj
        val data = inputStream?.instanceFields?.get("_data:[B") as? ByteArray ?: ByteArray(0)
        val pos = inputStream?.instanceFields?.get("_pos:I") as? Int ?: 0
        frame.push(maxOf(0, data.size - pos))
    }

    fun close(frame: ExecutionFrame) {
        frame.pop() // this
    }

    // ByteArrayInputStream specific
    fun initByteArray(frame: ExecutionFrame) {
        val data = frame.pop() as? ByteArray ?: ByteArray(0)
        val thisObj = frame.pop() as? HeapObject
        thisObj?.instanceFields?.set("_data:[B", data)
        thisObj?.instanceFields?.set("_pos:I", 0)
    }

    fun initByteArrayRegion(frame: ExecutionFrame) {
        val len = frame.popInt()
        val off = frame.popInt()
        val data = frame.pop() as? ByteArray ?: ByteArray(0)
        val thisObj = frame.pop() as? HeapObject
        
        // We simulate the region by copying or tracking offset. 
        // For simplicity, let's copy a slice if it's a ByteArrayInputStream
        val subData = if (off == 0 && len == data.size) data else data.copyOfRange(off, off + len)
        thisObj?.instanceFields?.set("_data:[B", subData)
        thisObj?.instanceFields?.set("_pos:I", 0)
    }
}
