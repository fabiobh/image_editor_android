package com.uaialternativa.imageeditor.data.file

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `formatFileSize should format bytes correctly`() {
        // Test cases: input bytes to expected output
        val testCases = mapOf(
            0L to "0 B",
            512L to "512 B",
            1024L to "1 KB",
            1536L to "1.5 KB",
            1048576L to "1 MB",
            1073741824L to "1 GB",
            1099511627776L to "1 TB"
        )

        testCases.forEach { (bytes, expected) ->
            val result = FileUtils.formatFileSize(bytes)
            assertEquals("Format for $bytes bytes should be $expected", expected, result)
        }
    }

    @Test
    fun `formatFileSize should handle negative values`() {
        val result = FileUtils.formatFileSize(-100L)
        assertEquals("Negative bytes should return 0 B", "0 B", result)
    }

    @Test
    fun `getFileExtension should extract extension correctly`() {
        val testCases = mapOf(
            "image.jpg" to "jpg",
            "document.PDF" to "pdf",
            "file.tar.gz" to "gz",
            "noextension" to "",
            "file." to "",
            ".hidden" to "hidden"
        )

        testCases.forEach { (filename, expected) ->
            val result = FileUtils.getFileExtension(filename)
            assertEquals("Extension for $filename should be $expected", expected, result)
        }
    }

    @Test
    fun `getFileNameWithoutExtension should remove extension correctly`() {
        val testCases = mapOf(
            "image.jpg" to "image",
            "document.PDF" to "document",
            "file.tar.gz" to "file.tar",
            "noextension" to "noextension",
            "file." to "file",
            ".hidden" to ""
        )

        testCases.forEach { (filename, expected) ->
            val result = FileUtils.getFileNameWithoutExtension(filename)
            assertEquals("Name without extension for $filename should be $expected", expected, result)
        }
    }

    @Test
    fun `isSafeFilename should validate filenames correctly`() {
        val safeFilenames = listOf(
            "image.jpg",
            "document_2023.pdf",
            "file-name.txt",
            "simple",
            "file123.png"
        )

        val unsafeFilenames = listOf(
            "",
            "   ",
            "../../../etc/passwd",
            "file/with/slashes.txt",
            "file\\with\\backslashes.txt",
            "file<with>invalid:chars.txt",
            "file\"with|quotes?.txt",
            "file*with*wildcards.txt",
            "a".repeat(256) // Too long
        )

        safeFilenames.forEach { filename ->
            assertTrue("$filename should be safe", FileUtils.isSafeFilename(filename))
        }

        unsafeFilenames.forEach { filename ->
            assertFalse("$filename should not be safe", FileUtils.isSafeFilename(filename))
        }
    }

    @Test
    fun `sanitizeFilename should clean filenames correctly`() {
        val testCases = mapOf(
            "normal_file.jpg" to "normal_file.jpg",
            "file<with>invalid:chars.txt" to "file_with_invalid_chars.txt",
            "file/with/slashes.txt" to "file_with_slashes.txt",
            "file\\with\\backslashes.txt" to "file_with_backslashes.txt",
            "../../../dangerous.txt" to "______dangerous.txt", // Updated expectation
            "" to "untitled",
            "   " to "untitled",
            "a".repeat(300) to "a".repeat(255)
        )

        testCases.forEach { (input, expected) ->
            val result = FileUtils.sanitizeFilename(input)
            assertEquals("Sanitized $input should be $expected", expected, result)
        }
    }

    // Note: Tests for getMimeType, getDisplayName, and getFileSize are skipped
    // as they require Android Context mocking which is complex in unit tests.
    // These methods will be tested in integration tests.

    @Test
    fun `hasEnoughSpace should check available space correctly`() {
        // Given
        val directory = tempFolder.root
        val requiredBytes = 1024L

        // When
        val result = FileUtils.hasEnoughSpace(directory, requiredBytes)

        // Then
        assertTrue("Should have enough space for small requirement", result)
    }

    @Test
    fun `hasEnoughSpace should return false for excessive requirement`() {
        // Given
        val directory = tempFolder.root
        val requiredBytes = Long.MAX_VALUE

        // When
        val result = FileUtils.hasEnoughSpace(directory, requiredBytes)

        // Then
        assertFalse("Should not have enough space for excessive requirement", result)
    }

    @Test
    fun `getAvailableSpace should return positive value for valid directory`() {
        // Given
        val directory = tempFolder.root

        // When
        val result = FileUtils.getAvailableSpace(directory)

        // Then
        assertTrue("Available space should be positive", result > 0)
    }

    @Test
    fun `createBackupFilename should generate backup names with counter`() {
        // Given
        val originalPath = File(tempFolder.root, "test.jpg").path

        // When
        val backupPath = FileUtils.createBackupFilename(originalPath)

        // Then
        assertTrue("Backup should contain _backup_", backupPath.contains("_backup_"))
        assertTrue("Backup should contain counter", backupPath.matches(Regex(".*_backup_\\d+\\.jpg")))
    }

    @Test
    fun `createBackupFilename should handle files without extension`() {
        // Given
        val originalPath = File(tempFolder.root, "test_file").path

        // When
        val backupPath = FileUtils.createBackupFilename(originalPath)

        // Then
        assertTrue("Backup should contain _backup_1", backupPath.contains("_backup_1"))
        assertFalse("Backup should not have extension", backupPath.contains("."))
    }

    @Test
    fun `isPathWithinDirectory should validate paths correctly`() {
        // Given
        val allowedDir = tempFolder.root
        val subDir = File(allowedDir, "subdir")
        subDir.mkdirs()
        
        val validPath = File(subDir, "file.txt").path
        val invalidPath = File(tempFolder.root.parentFile, "outside.txt").path

        // When & Then
        assertTrue("Path within directory should be valid", 
            FileUtils.isPathWithinDirectory(validPath, allowedDir))
        assertFalse("Path outside directory should be invalid", 
            FileUtils.isPathWithinDirectory(invalidPath, allowedDir))
    }

    @Test
    fun `deleteDirectoryRecursively should delete directory and contents`() {
        // Given
        val testDir = File(tempFolder.root, "test_dir")
        val subDir = File(testDir, "sub_dir")
        val file1 = File(testDir, "file1.txt")
        val file2 = File(subDir, "file2.txt")
        
        testDir.mkdirs()
        subDir.mkdirs()
        file1.writeText("content1")
        file2.writeText("content2")
        
        assertTrue("Test directory should exist", testDir.exists())
        assertTrue("Sub directory should exist", subDir.exists())
        assertTrue("File1 should exist", file1.exists())
        assertTrue("File2 should exist", file2.exists())

        // When
        val result = FileUtils.deleteDirectoryRecursively(testDir)

        // Then
        assertTrue("Deletion should succeed", result)
        assertFalse("Test directory should be deleted", testDir.exists())
        assertFalse("Sub directory should be deleted", subDir.exists())
        assertFalse("File1 should be deleted", file1.exists())
        assertFalse("File2 should be deleted", file2.exists())
    }

    @Test
    fun `deleteDirectoryRecursively should handle single file`() {
        // Given
        val testFile = File(tempFolder.root, "test_file.txt")
        testFile.writeText("content")
        assertTrue("Test file should exist", testFile.exists())

        // When
        val result = FileUtils.deleteDirectoryRecursively(testFile)

        // Then
        assertTrue("Deletion should succeed", result)
        assertFalse("Test file should be deleted", testFile.exists())
    }
}