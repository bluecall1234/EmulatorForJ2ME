package emulator.core.api.java.io

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Kotlin implementation of java.io.DataInputStream for the J2ME emulator.
 */
object DataInputStream {

    fun init(frame: ExecutionFrame) {
        val inputStream = frame.pop() as? HeapObject
        val thisObj = frame.pop() as? HeapObject
        thisObj?.instanceFields?.set("_stream", inputStream)
    }

    fun readUnsignedShort(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            
            if (pos + 1 < data.size) {
                val b1 = data[pos].toInt() and 0xFF
                val b2 = data[pos + 1].toInt() and 0xFF
                inputStream.instanceFields["_pos:I"] = pos + 2
                frame.push((b1 shl 8) or b2)
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        } else {
            throw RuntimeException("java/io/EOFException")
        }
    }

    fun readShort(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos + 1 < data.size) {
                val b1 = data[pos].toInt()
                val b2 = data[pos + 1].toInt() and 0xFF
                inputStream.instanceFields["_pos:I"] = pos + 2
                frame.push(((b1 shl 8) or b2).toShort().toInt())
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        } else {
            frame.push(0)
        }
    }

    fun readInt(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos + 3 < data.size) {
                val b1 = data[pos].toInt() and 0xFF
                val b2 = data[pos + 1].toInt() and 0xFF
                val b3 = data[pos + 2].toInt() and 0xFF
                val b4 = data[pos + 3].toInt() and 0xFF
                inputStream.instanceFields["_pos:I"] = pos + 4
                frame.push((b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4)
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        } else {
            frame.push(0)
        }
    }

    fun readByte(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos < data.size) {
                val b = data[pos].toInt()
                inputStream.instanceFields["_pos:I"] = pos + 1
                frame.push(b)
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        } else {
            frame.push(0)
        }
    }

    fun readUnsignedByte(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos < data.size) {
                val b = data[pos].toInt() and 0xFF
                inputStream.instanceFields["_pos:I"] = pos + 1
                frame.push(b)
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        } else {
            throw RuntimeException("java/io/EOFException")
        }
    }

    fun readBoolean(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos < data.size) {
                val b = data[pos].toInt() != 0
                inputStream.instanceFields["_pos:I"] = pos + 1
                frame.push(if (b) 1 else 0)
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        } else {
            frame.push(0)
        }
    }

    fun readChar(frame: ExecutionFrame) {
        // readChar in Java is equivalent to readUnsignedShort
        readUnsignedShort(frame)
    }

    fun readUTF(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            
            if (pos + 1 < data.size) {
                val len = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
                pos += 2
                if (pos + len <= data.size) {
                    val utf8 = data.decodeToString(pos, pos + len)
                    inputStream.instanceFields["_pos:I"] = pos + len
                    frame.push(utf8)
                } else {
                    frame.push("")
                }
            } else {
                frame.push("")
            }
        } else {
            frame.push("")
        }
    }

    fun skipBytes(frame: ExecutionFrame) {
        val n = frame.popInt()
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            val skipped = minOf(n, data.size - pos)
            inputStream.instanceFields["_pos:I"] = pos + skipped
            frame.push(skipped)
        } else {
            frame.push(0)
        }
    }

    fun readLong(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos + 7 < data.size) {
                val b1 = data[pos].toLong() and 0xFF
                val b2 = data[pos + 1].toLong() and 0xFF
                val b3 = data[pos + 2].toLong() and 0xFF
                val b4 = data[pos + 3].toLong() and 0xFF
                val b5 = data[pos + 4].toLong() and 0xFF
                val b6 = data[pos + 5].toLong() and 0xFF
                val b7 = data[pos + 6].toLong() and 0xFF
                val b8 = data[pos + 7].toLong() and 0xFF
                inputStream.instanceFields["_pos:I"] = pos + 8
                val res = (b1 shl 56) or (b2 shl 48) or (b3 shl 40) or (b4 shl 32) or
                          (b5 shl 24) or (b6 shl 16) or (b7 shl 8) or b8
                frame.push(res)
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        } else {
            frame.push(0L)
        }
    }

    fun readFloat(frame: ExecutionFrame) {
        // We need to read an int and convert to float
        // But readInt pushes to stack. Let's do it manually.
        val thisObj = frame.peek() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos + 3 < data.size) {
                val b1 = data[pos].toInt() and 0xFF
                val b2 = data[pos + 1].toInt() and 0xFF
                val b3 = data[pos + 2].toInt() and 0xFF
                val b4 = data[pos + 3].toInt() and 0xFF
                inputStream.instanceFields["_pos:I"] = pos + 4
                val bits = (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
                frame.pop() // this
                frame.push(Float.fromBits(bits))
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        } else {
            frame.pop()
            frame.push(0.0f)
        }
    }

    fun readDouble(frame: ExecutionFrame) {
        val thisObj = frame.peek() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos + 7 < data.size) {
                val b1 = data[pos].toLong() and 0xFF
                val b2 = data[pos + 1].toLong() and 0xFF
                val b3 = data[pos + 2].toLong() and 0xFF
                val b4 = data[pos + 3].toLong() and 0xFF
                val b5 = data[pos + 4].toLong() and 0xFF
                val b6 = data[pos + 5].toLong() and 0xFF
                val b7 = data[pos + 6].toLong() and 0xFF
                val b8 = data[pos + 7].toLong() and 0xFF
                inputStream.instanceFields["_pos:I"] = pos + 8
                val bits = (b1 shl 56) or (b2 shl 48) or (b3 shl 40) or (b4 shl 32) or
                          (b5 shl 24) or (b6 shl 16) or (b7 shl 8) or b8
                frame.pop() // this
                frame.push(Double.fromBits(bits))
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        } else {
            frame.pop()
            frame.push(0.0)
        }
    }

    fun readFully(frame: ExecutionFrame) {
        val b = frame.pop() as? ByteArray
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        
        if (inputStream != null && b != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            val len = b.size
            if (pos + len <= data.size) {
                data.copyInto(b, 0, pos, pos + len)
                inputStream.instanceFields["_pos:I"] = pos + len
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        }
    }

    fun readFullyRegion(frame: ExecutionFrame) {
        val len = frame.popInt()
        val off = frame.popInt()
        val b = frame.pop() as? ByteArray
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        
        if (inputStream != null && b != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            if (pos + len <= data.size) {
                data.copyInto(b, off, pos, pos + len)
                inputStream.instanceFields["_pos:I"] = pos + len
            } else {
                throw RuntimeException("java/io/EOFException")
            }
        }
    }

    fun available(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject
        val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
        if (inputStream != null) {
            val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
            val pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
            frame.push(maxOf(0, data.size - pos))
        } else {
            frame.push(0)
        }
    }
    
    fun close(frame: ExecutionFrame) {
        frame.pop() // this
    }
}
