package pl.stapik.media.data.backup

import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import pl.stapik.common.document.BackupArchive
import pl.stapik.common.document.BackupContents
import pl.stapik.common.document.SafetyCopies
import pl.stapik.media.data.library.ArchiveDirectory
import pl.stapik.media.data.library.DocumentEditor
import pl.stapik.media.data.library.MediaLibrary
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails
import pl.stapik.media.data.serialization.MediaPayload

class BackupServiceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private class FakeEditor(var payload: MediaPayload = MediaPayload()) : DocumentEditor {
        var failNextEdit = false

        override suspend fun current(): MediaPayload = payload

        override suspend fun edit(transform: (MediaPayload) -> MediaPayload) {
            if (failNextEdit) {
                failNextEdit = false
                throw IllegalStateException("disk full")
            }
            payload = transform(payload)
        }
    }

    private class FakeArchives(val editors: MutableMap<Int, FakeEditor> = mutableMapOf()) : ArchiveDirectory {
        override fun loadedYears(): Set<Int> = editors.keys

        override fun remoteYears(): Set<Int>? = emptySet()

        override fun editorFor(year: Int): DocumentEditor = editors.getOrPut(year) { FakeEditor() }
    }

    private val clock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC)
    private val main = FakeEditor()
    private val archives = FakeArchives()
    private var nextId = 0
    private val library = MediaLibrary(main, archives, clock, { "id-${++nextId}" }, { true })
    private val copies by lazy { SafetyCopies(directory = folder.newFolder("safety").toPath()) }
    private val service by lazy { BackupService(library, copies, clock, "1.2.0") }

    private fun entry(title: String, consumedYear: Int = 2026, id: String = title.lowercase()) = MediaEntry(
        category = MediaCategory.MOVIE,
        title = title,
        releaseDate = ReleaseDate(LocalDate.of(2020, 1, 1), DatePrecision.DAY),
        consumed = ConsumedDate(month = 3, year = consumedYear),
        details = MediaDetails.Screen(director = "D", genre = "G"),
        id = id,
    )

    private fun archiveOf(appId: String, document: String?): ByteArray =
        ByteArrayOutputStream().also { BackupArchive.write(it, BackupContents(appId, clock.instant(), document, appVersion = "1")) }.toByteArray()

    private fun desktopArchive(document: String): ByteArray {
        val manifest = """{"format":"stapik-archive","formatVersion":1,"app":"stapikmedia","appVersion":"1.0.0","createdAt":"2026-01-01T00:00:00Z","assets":[]}"""
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            mapOf("document.json" to document, "settings.json" to "{}", "manifest.json" to manifest).forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun failureOf(action: () -> Unit): BackupFailure = assertThrows(BackupException::class.java) { action() }.failure

    @Test
    fun anExportedBackupOpensWithTheSameEntriesFromEveryLoadedDocument() = runBlocking {
        main.payload = MediaPayload(listOf(entry("Dune")))
        archives.editors[2024] = FakeEditor(MediaPayload(listOf(entry("Arrival", consumedYear = 2024))))

        val exported = service.export()
        val opened = service.open(exported.bytes)

        assertEquals(2, exported.entries)
        assertEquals(listOf("Dune", "Arrival"), opened.payload.entries.map { it.title })
        assertEquals(clock.instant(), opened.createdAt)
    }

    @Test
    fun anArchiveOfAnotherApplicationIsRefused() {
        val bytes = archiveOf("stapikmoney", """{"schemaVersion":2,"document":{"entries":[]}}""")

        assertEquals(BackupFailure.WRONG_APPLICATION, failureOf { service.open(bytes) })
    }

    @Test
    fun aFileThatIsNotAnArchiveIsRefused() {
        assertEquals(BackupFailure.NOT_A_BACKUP, failureOf { service.open("hello".toByteArray()) })
    }

    @Test
    fun anArchiveWithoutADocumentIsDamaged() {
        assertEquals(BackupFailure.DAMAGED, failureOf { service.open(archiveOf("stapikmedia", null)) })
    }

    @Test
    fun anArchiveMadeByTheDesktopIsAcceptedAndItsSettingsAreIgnored() {
        val document = BackupDocument.write(MediaPayload(listOf(entry("Dune"))), "2026-01-01T00:00:00Z")

        val opened = service.open(desktopArchive(document))

        assertEquals(listOf("Dune"), opened.payload.entries.map { it.title })
    }

    @Test
    fun restoringReplacesTheListAndKeepsTheOldOneAsASafetyCopy() = runBlocking {
        main.payload = MediaPayload(listOf(entry("Old")))
        main.payload = MediaPayload(listOf(entry("Restored")))
        val backup = service.open(service.export().bytes)
        main.payload = MediaPayload(listOf(entry("Old")))

        val report = service.restore(backup)

        assertEquals(listOf("Restored"), main.payload.entries.map { it.title })
        assertEquals(1, report.applied)
        val saved = service.savedCopies()
        assertEquals(1, saved.size)
        assertEquals(listOf("Old"), service.openSafetyCopy(saved.single()).payload.entries.map { it.title })
    }

    @Test
    fun aFailureWhileApplyingRestoresThePreviousList() = runBlocking {
        main.payload = MediaPayload(listOf(entry("Old")))
        archives.editors[2024] = FakeEditor(MediaPayload(listOf(entry("OldArchived", consumedYear = 2024))))
        val backup = BackupService.Opened(MediaPayload(listOf(entry("New"))), clock.instant())
        main.failNextEdit = true

        val failure = assertThrows(BackupException::class.java) { runBlocking { service.restore(backup) } }.failure

        assertEquals(BackupFailure.UNREADABLE, failure)
        assertEquals(listOf("Old"), main.payload.entries.map { it.title })
        assertEquals(listOf("OldArchived"), archives.editors.getValue(2024).payload.entries.map { it.title })
    }

    @Test
    fun withoutARestoreThereAreNoSafetyCopies() = runBlocking {
        assertTrue(service.savedCopies().isEmpty())
    }
}
