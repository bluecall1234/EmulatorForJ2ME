package emulator.core.api.javax.microedition.rms

expect class RmsStorage {
    constructor(storeName: String)

    fun readStore(): Map<Int, ByteArray>?
    fun writeStore(records: Map<Int, ByteArray>)
    fun deleteStore()
}
