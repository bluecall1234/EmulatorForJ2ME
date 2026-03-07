package emulator.core.api.javax.microedition.rms

// For now, stub the iOS implementation. Full serialization using 
// NSFileManager and NSData would go here in the future.
actual class RmsStorage actual constructor(private val storeName: String) {

    private val inMemoryStore = mutableMapOf<Int, ByteArray>()

    actual fun readStore(): Map<Int, ByteArray>? {
        if (inMemoryStore.isEmpty()) return null
        return inMemoryStore.toMap()
    }

    actual fun writeStore(records: Map<Int, ByteArray>) {
        inMemoryStore.clear()
        inMemoryStore.putAll(records)
    }

    actual fun deleteStore() {
        inMemoryStore.clear()
    }
}
