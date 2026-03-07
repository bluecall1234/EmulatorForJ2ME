package emulator.core.api.javax.microedition.rms

class RecordStore(val storeName: String) {

    private val storage = RmsStorage(storeName)
    
    // RMS records are 1-indexed. We store them here.
    private val records = mutableMapOf<Int, ByteArray>()
    var nextRecordId = 1

    init {
        // Load on creation
        val db = storage.readStore()
        if (db != null) {
            records.putAll(db)
            if (records.isNotEmpty()) {
                nextRecordId = records.keys.maxOrNull()!! + 1
            }
        }
    }

    fun addRecord(data: ByteArray, offset: Int, numBytes: Int): Int {
        val bytesToStore = data.copyOfRange(offset, offset + numBytes)
        val id = nextRecordId++
        records[id] = bytesToStore
        
        // Save immediately for safety
        storage.writeStore(records)
        return id
    }

    fun getRecord(recordId: Int): ByteArray? {
        val rec = records[recordId] ?: return null
        return rec.copyOf()
    }

    fun getRecord(recordId: Int, buffer: ByteArray, offset: Int): Int {
        val rec = records[recordId] ?: throw RuntimeException("RecordStore.getRecord: Invalid recordId $recordId")
        rec.copyInto(buffer, offset, 0, rec.size)
        return rec.size
    }

    fun setRecord(recordId: Int, newData: ByteArray, offset: Int, numBytes: Int) {
        if (!records.containsKey(recordId)) {
            throw RuntimeException("RecordStore.setRecord: Invalid recordId $recordId")
        }
        val bytesToStore = newData.copyOfRange(offset, offset + numBytes)
        records[recordId] = bytesToStore
        storage.writeStore(records)
    }

    fun deleteRecord(recordId: Int) {
        if (records.remove(recordId) == null) {
             throw RuntimeException("RecordStore.deleteRecord: Invalid recordId $recordId")
        }
        storage.writeStore(records)
    }

    fun getNumRecords(): Int {
        return records.size
    }

    fun closeRecordStore() {
        // Explicitly flush to disk
        storage.writeStore(records)
        openStores.remove(storeName)
    }

    companion object {
        private val openStores = mutableMapOf<String, RecordStore>()

        fun openRecordStore(name: String, createIfNecessary: Boolean): RecordStore {
            var store = openStores[name]
            if (store == null) {
                store = RecordStore(name)
                // If it shouldn't be created and doesn't exist, technically we should throw
                // RecordStoreNotFoundException. But let's simplify for now.
                openStores[name] = store
            }
            return store
        }

        fun deleteRecordStore(name: String) {
            openStores.remove(name)
            RmsStorage(name).deleteStore()
        }
    }
}
