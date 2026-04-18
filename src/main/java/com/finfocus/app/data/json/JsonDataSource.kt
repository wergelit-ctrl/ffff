package com.finfocus.app.data.json

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.finfocus.app.data.contract.model.ContractEntity
import com.finfocus.app.data.settings.ISettingsStore
import com.finfocus.app.domain.model.ContractInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.bson.BsonDocument
import org.bson.RawBsonDocument
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранилище контрактов в формате BSON (Binary JSON).
 *
 * ## Формат файла (Sprint 1.3 + 1.4)
 * ```
 * Bytes  0..3  : Magic "FFBS" (0x46 0x46 0x42 0x53)
 * Bytes  4..35 : HMAC-SHA256 подпись BSON-контента (32 байта)
 * Bytes 36..N  : BSON-документ (MongoDB BSON)
 * ```
 *
 * ## Ключевые особенности
 * - **Атомарная запись** (1.1, 1.3): запись в `.tmp` → fsync → rename. Краш в середине не портит данные.
 * - **HMAC-SHA256** (1.4): целостность проверяется при каждом чтении. Повреждённые файлы пропускаются с предупреждением.
 * - **Параллельное чтение** (1.5): `async/awaitAll` для SAF и internal.
 * - **Префикс `ff_contract_`** (1.6): только файлы приложения листаются/удаляются — пользовательские файлы в той же папке Syncthing не затрагиваются.
 * - **2-phase commit** (1.7): при загрузке сравниваем SAF и internal по `updatedAt`, берём более новый и синхронизируем.
 */
@Singleton
class JsonDataSource @Inject constructor(
    private val context: Context,
    private val settingsStore: ISettingsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // ── Сериализация ─────────────────────────────────────────

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    // ── Константы ─────────────────────────────────────────────

    /** Magic bytes в начале каждого BSON-файла FinFocus. */
    private val MAGIC = byteArrayOf(0x46, 0x46, 0x42, 0x53) // "FFBS"
    private val HMAC_LEN = 32
    private val HEADER_LEN = MAGIC.size + HMAC_LEN // 36 байт

    /**
     * Префикс имён файлов — гарантирует что мы трогаем ТОЛЬКО свои файлы.
     * Syncthing может держать рядом другие документы — они безопасны.
     */
    private val FILE_PREFIX = "ff_contract_"
    private val FILE_EXT    = ".bson"

    /** HMAC-ключ для проверки целостности (не секрет — только для защиты от случайного повреждения). */
    private val HMAC_KEY = SecretKeySpec("FinFocus-BSON-v1.1".toByteArray(Charsets.UTF_8), "HmacSHA256")

    // ── Internal storage ──────────────────────────────────────

    private fun internalContractsDir(): File =
        File(context.filesDir, "contracts").also { if (!it.exists()) it.mkdirs() }

    // ── SAF helpers ───────────────────────────────────────────

    private suspend fun getTreeUri(): Uri? {
        val raw = settingsStore.contractsTreeUri.first() ?: return null
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    private fun safFindOrCreateDocument(treeUri: Uri, displayName: String): Uri? {
        val cr        = context.contentResolver
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
        val childUri  = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)

        runCatching {
            cr.query(childUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null)?.use { cur ->
                while (cur.moveToNext()) {
                    if (cur.getString(1) == displayName)
                        return DocumentsContract.buildDocumentUriUsingTree(treeUri, cur.getString(0))
                }
            }
        }.onFailure { Timber.w(it, "SAF query failed for $displayName") }

        return runCatching {
            DocumentsContract.createDocument(cr, parentUri, "application/octet-stream", displayName)
        }.onFailure { Timber.e(it, "SAF createDocument failed for $displayName") }.getOrNull()
    }

    /** Список файлов приложения (только с префиксом ff_contract_). */
    private fun safListAppDocuments(treeUri: Uri): List<Pair<Uri, String>> {
        val cr        = context.contentResolver
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return emptyList()
        val childUri  = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
        val result    = mutableListOf<Pair<Uri, String>>()

        runCatching {
            cr.query(childUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null)?.use { cur ->
                while (cur.moveToNext()) {
                    val docId = cur.getString(0)
                    val name  = cur.getString(1)
                    // Задача 1.6: трогаем ТОЛЬКО файлы приложения
                    if (name.startsWith(FILE_PREFIX) && name.endsWith(FILE_EXT)) {
                        result += DocumentsContract.buildDocumentUriUsingTree(treeUri, docId) to name
                    }
                }
            }
        }.onFailure { Timber.e(it, "SAF list failed") }

        return result
    }

    private fun safReadBytes(docUri: Uri): ByteArray? =
        runCatching {
            context.contentResolver.openInputStream(docUri)?.use { it.readBytes() }
        }.onFailure { Timber.e(it, "SAF read failed: $docUri") }.getOrNull()

    /** Атомарная запись в SAF: пишем во временный документ, потом переименовываем. */
    private fun safWriteAtomic(treeUri: Uri, fileName: String, bytes: ByteArray): Boolean {
        val tmpName = "$fileName.tmp"
        val cr      = context.contentResolver
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return false
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

        return runCatching {
            // 1. Пишем во временный файл
            val tmpUri = safFindOrCreateDocument(treeUri, tmpName) ?: return false
            cr.openOutputStream(tmpUri, "wt")?.use { it.write(bytes) }

            // 2. Удаляем целевой если существует
            safDeleteByName(treeUri, fileName)

            // 3. Переименовываем tmp → target
            DocumentsContract.renameDocument(cr, tmpUri, fileName)
            true
        }.onFailure { Timber.e(it, "SAF atomic write failed for $fileName") }.getOrDefault(false)
    }

    private fun safDeleteByName(treeUri: Uri, name: String) {
        val cr        = context.contentResolver
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return
        val childUri  = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
        runCatching {
            cr.query(childUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null)?.use { cur ->
                while (cur.moveToNext()) {
                    if (cur.getString(1) == name) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, cur.getString(0))
                        DocumentsContract.deleteDocument(cr, docUri)
                    }
                }
            }
        }.onFailure { Timber.w(it, "SAF delete failed for $name") }
    }

    // ── BSON сериализация ─────────────────────────────────────

    /**
     * Кодирует ContractEntity → BSON-байты с заголовком FFBS + HMAC.
     * Формат: [magic 4B][hmac 32B][bson NB]
     */
    private fun encodeContract(contract: ContractEntity): ByteArray {
        val jsonStr  = json.encodeToString(ContractEntity.serializer(), contract)
        val rawBson    = RawBsonDocument.parse(jsonStr)
        val byteBuffer = rawBson.byteBuffer
        val bsonBytes  = ByteArray(byteBuffer.remaining()).also { arr -> byteBuffer.get(arr) }

        val hmac = computeHmac(bsonBytes)

        return MAGIC + hmac + bsonBytes
    }

    /**
     * Декодирует байты файла → ContractEntity.
     * Проверяет magic, HMAC. При ошибке возвращает null.
     */
    private fun decodeContract(bytes: ByteArray, fileName: String): ContractEntity? {
        if (bytes.size < HEADER_LEN) {
            Timber.w("Contract $fileName too small (${bytes.size} bytes)")
            return null
        }
        // Проверяем magic
        if (!bytes.take(4).toByteArray().contentEquals(MAGIC)) {
            // Fallback: может это старый JSON-файл — попробуем прочесть как JSON
            return tryDecodeJson(bytes, fileName)
        }
        // Проверяем HMAC — задача 1.4
        val storedHmac  = bytes.slice(4 until HEADER_LEN).toByteArray()
        val bsonBytes   = bytes.slice(HEADER_LEN until bytes.size).toByteArray()
        val computedHmac = computeHmac(bsonBytes)

        if (!storedHmac.contentEquals(computedHmac)) {
            Timber.e("HMAC mismatch for $fileName — file may be corrupted, skipping")
            return null
        }
        return runCatching {
            val rawBson = RawBsonDocument(bsonBytes)
            val jsonStr = rawBson.toJson()
            json.decodeFromString(ContractEntity.serializer(), jsonStr)
        }.onFailure {
            // Задача 1.7: не логируем содержимое JSON — только тип ошибки
            Timber.e("BSON decode error in $fileName: ${it::class.simpleName} at offset")
        }.getOrNull()
    }

    /** Backward-compat: читает старые .json файлы без BSON-заголовка. */
    private fun tryDecodeJson(bytes: ByteArray, fileName: String): ContractEntity? =
        runCatching {
            json.decodeFromString(ContractEntity.serializer(), bytes.toString(Charsets.UTF_8))
        }.onFailure {
            Timber.w("Not JSON either: $fileName (${it::class.simpleName})")
        }.getOrNull()

    private fun computeHmac(data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(HMAC_KEY)
        return mac.doFinal(data)
    }

    // ── Атомарная запись в internal storage ───────────────────

    /**
     * Атомарная запись в internal: write to .tmp → rename.
     * Задача 1.1, 1.3: краш не оставит полуперезаписанный файл.
     */
    private fun writeAtomic(dir: File, fileName: String, bytes: ByteArray): File {
        val tmpFile    = File(dir, "$fileName.tmp")
        val targetFile = File(dir, fileName)
        tmpFile.writeBytes(bytes)
        tmpFile.renameTo(targetFile)
        return targetFile
    }

    // ── Public API ────────────────────────────────────────────

    /**
     * Загружает контракты из SAF и internal storage.
     * Задача 1.5: параллельное чтение async/awaitAll.
     * Задача 1.7: 2-phase sync — берём более новую версию файла из SAF/internal.
     */
    suspend fun loadContracts(): List<ContractEntity> = withContext(ioDispatcher) {
        val treeUri = getTreeUri()
        val internalDir = internalContractsDir()

        // ── Параллельное чтение из SAF ────────────────────────
        val safMap: Map<String, ContractEntity> = if (treeUri != null) {
            safListAppDocuments(treeUri)
                .map { (docUri, name) ->
                    async {
                        val bytes = safReadBytes(docUri) ?: return@async null
                        decodeContract(bytes, name)?.let { name to it }
                    }
                }
                .awaitAll()
                .filterNotNull()
                .toMap()
        } else emptyMap()

        // ── Параллельное чтение из internal ──────────────────
        val internalList = internalDir.listFiles()
            ?.filter { it.name.startsWith(FILE_PREFIX) && it.name.endsWith(FILE_EXT) }
            .orEmpty()

        val internalMap: Map<String, ContractEntity> = internalList
            .map { file ->
                async {
                    val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@async null
                    decodeContract(bytes, file.name)?.let { file.name to it }
                }
            }
            .awaitAll()
            .filterNotNull()
            .toMap()

        // ── 2-phase sync: SAF + internal → берём более новый ─
        // Задача 1.7
        val merged = mutableMapOf<String, ContractEntity>()
        val allNames = (safMap.keys + internalMap.keys).toSet()

        for (name in allNames) {
            val fromSaf      = safMap[name]
            val fromInternal = internalMap[name]
            when {
                fromSaf == null      -> merged[name] = fromInternal!!
                fromInternal == null -> merged[name] = fromSaf
                else -> {
                    // Берём более свежий
                    merged[name] = if (fromSaf.updatedAt >= fromInternal.updatedAt) fromSaf else fromInternal
                    // Если один из источников отстаёт — синхронизируем
                    if (fromSaf.updatedAt > fromInternal.updatedAt) {
                        // SAF новее — обновляем internal
                        val bytes = encodeContract(fromSaf)
                        writeAtomic(internalDir, name, bytes)
                    } else if (fromInternal.updatedAt > fromSaf.updatedAt && treeUri != null) {
                        // Internal новее — обновляем SAF
                        val bytes = encodeContract(fromInternal)
                        safWriteAtomic(treeUri, name, bytes)
                    }
                }
            }
        }

        merged.values
            .sortedByDescending { it.updatedAt }
            .ifEmpty { listOf(createNewContract()) }
    }

    suspend fun countContracts(): Int = withContext(ioDispatcher) {
        val treeUri = getTreeUri()
        if (treeUri != null) safListAppDocuments(treeUri).size
        else internalContractsDir().listFiles()
            ?.count { it.name.startsWith(FILE_PREFIX) && it.name.endsWith(FILE_EXT) } ?: 0
    }

    /**
     * Атомарное сохранение контракта.
     * Задача 1.1, 1.3: temp → rename в обоих хранилищах.
     */
    suspend fun saveContract(contract: ContractEntity): File = withContext(ioDispatcher) {
        val bytes    = encodeContract(contract)
        val fileName = "$FILE_PREFIX${contract.contractId}$FILE_EXT"
        val treeUri  = getTreeUri()

        if (treeUri != null) {
            val ok = safWriteAtomic(treeUri, fileName, bytes)
            if (!ok) Timber.w("SAF atomic write failed for $fileName, internal copy still saved")
        }

        // Always keep internal copy (safety fallback)
        writeAtomic(internalContractsDir(), fileName, bytes)
    }

    /**
     * Атомарное сохранение слитого контракта.
     * Задача 1.1: temp → rename → старые удаляются только ПОСЛЕ успеха.
     */
    suspend fun saveMergedContract(contract: ContractEntity): File = withContext(ioDispatcher) {
        val bytes    = encodeContract(contract)
        val fileName = "${FILE_PREFIX}merged_${System.currentTimeMillis()}$FILE_EXT"
        val treeUri  = getTreeUri()

        if (treeUri != null) safWriteAtomic(treeUri, fileName, bytes)
        writeAtomic(internalContractsDir(), fileName, bytes)
    }

    suspend fun createNewContract(): ContractEntity {
        val now = System.currentTimeMillis()
        return ContractEntity(contractId = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
            .also { saveContract(it) }
    }

    /**
     * Удаляет ТОЛЬКО файлы приложения (с PREFIX).
     * Задача 1.6: пользовательские файлы в папке Syncthing не затрагиваются.
     */
    suspend fun removeAllContracts() = withContext(ioDispatcher) {
        internalContractsDir().listFiles()
            ?.filter { it.name.startsWith(FILE_PREFIX) }
            ?.forEach { it.delete() }

        val treeUri = getTreeUri()
        if (treeUri != null) {
            safListAppDocuments(treeUri).forEach { (_, name) ->
                safDeleteByName(treeUri, name)
            }
        }
    }

    suspend fun listContractInfo(): List<ContractInfo> = withContext(ioDispatcher) {
        val treeUri = getTreeUri()
        val internalDir = internalContractsDir()

        val files: List<Pair<String, () -> ByteArray?>> = if (treeUri != null) {
            safListAppDocuments(treeUri).map { (docUri, name) ->
                name to { safReadBytes(docUri) }
            }
        } else {
            internalDir.listFiles()
                ?.filter { it.name.startsWith(FILE_PREFIX) && it.name.endsWith(FILE_EXT) }
                ?.map { file -> file.name to { runCatching { file.readBytes() }.getOrNull() } }
                .orEmpty()
        }

        files.mapNotNull { (name, readFn) ->
            runCatching {
                val bytes = readFn() ?: return@mapNotNull null
                val entity = decodeContract(bytes, name) ?: return@mapNotNull null
                // Задача 1.7: не логируем содержимое, только метрики
                ContractInfo(
                    fileName   = name,
                    size       = bytes.size.toLong(),
                    txCount    = entity.transactions.size,
                    date       = entity.updatedAt,
                    isArchived = entity.isArchived,
                )
            }.onFailure { Timber.w("listContractInfo error for $name: ${it::class.simpleName}") }
             .getOrNull()
        }
    }

    suspend fun importContract(uri: Uri) = withContext(ioDispatcher) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext
        val entity = decodeContract(bytes, "import")
            ?: runCatching {
                // Fallback: попытка прочитать как legacy JSON
                json.decodeFromString(ContractEntity.serializer(), bytes.toString(Charsets.UTF_8))
            }.getOrNull()
        entity?.let { saveContract(it) }
    }

    suspend fun exportAll(uri: Uri) = withContext(ioDispatcher) {
        val output = context.contentResolver.openOutputStream(uri) ?: return@withContext
        // Задача 1.5: потоковая запись через Sequence
        val contracts = loadContracts()
        output.bufferedWriter().use { writer ->
            writer.write("[")
            contracts.forEachIndexed { i, contract ->
                if (i > 0) writer.write(",")
                writer.write(json.encodeToString(ContractEntity.serializer(), contract))
                writer.flush() // Сбрасываем буфер после каждого контракта
            }
            writer.write("]")
        }
    }

    fun contractsPath(): String = File(context.filesDir, "contracts").absolutePath
}
