package com.ljyh.mei.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

/** Android 10 cannot place a text sidecar under Music through MediaStore's file collection. */
internal object Android10LyricTree {
    private const val PREFS_NAME = "lyric_sidecar_storage"
    private const val TREE_URI_KEY = "android_10_music_tree_uri"

    fun directorySegments(relativeAudioPath: String, treeDocumentId: String): List<String>? {
        val prefix = when (treeDocumentId) {
            "primary:Music" -> listOf("Mei")
            "primary:Music/Mei" -> emptyList()
            else -> return null
        }
        val playlist = relativeAudioPath.removePrefix("Music/Mei/")
        if (playlist == relativeAudioPath || playlist.isBlank() ||
            playlist.split('/').any { it.isBlank() || it == "." || it == ".." }
        ) return null
        return prefix + playlist.split('/')
    }

    fun grantedTree(context: Context): Uri? {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TREE_URI_KEY, null) ?: return null
        return runCatching {
            val uri = Uri.parse(saved)
            if (directorySegments("Music/Mei/check", DocumentsContract.getTreeDocumentId(uri)) == null) {
                return@runCatching null
            }
            uri.takeIf { tree ->
                context.contentResolver.persistedUriPermissions.any {
                    it.uri == tree && it.isWritePermission
                }
            }
        }.getOrNull()
    }

    fun persistTree(context: Context, uri: Uri): Boolean {
        return runCatching {
            if (directorySegments("Music/Mei/check", DocumentsContract.getTreeDocumentId(uri)) == null) {
                return@runCatching false
            }
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(TREE_URI_KEY, uri.toString()).commit()
        }.getOrDefault(false)
    }

    fun write(context: Context, relativeAudioPath: String, fileName: String, lyric: String) {
        val tree = grantedTree(context) ?: error("Music folder access was not granted")
        val segments = directorySegments(
            relativeAudioPath,
            DocumentsContract.getTreeDocumentId(tree),
        ) ?: error("Invalid lyric destination: $relativeAudioPath")
        val resolver = context.contentResolver
        var parent = DocumentsContract.buildDocumentUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree),
        )
        for (segment in segments) {
            parent = findChild(context, tree, parent, segment)?.also { child ->
                require(child.second == DocumentsContract.Document.MIME_TYPE_DIR) {
                    "$segment is not a folder"
                }
            }?.first ?: DocumentsContract.createDocument(
                resolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, segment,
            ) ?: error("Cannot create lyric folder $segment")
        }
        val existing = findChild(context, tree, parent, fileName)
        // The generic MIME type preserves .lrc/.ttml; text/plain may append .txt.
        val destination = existing?.first ?: DocumentsContract.createDocument(
            resolver, parent, "application/octet-stream", fileName,
        ) ?: error("Cannot create lyric file $fileName")
        try {
            resolver.openOutputStream(destination, "wt")?.use {
                it.write(lyric.toByteArray(Charsets.UTF_8))
            } ?: error("Cannot write lyric file $fileName")
        } catch (e: Exception) {
            if (existing == null) runCatching { DocumentsContract.deleteDocument(resolver, destination) }
            throw e
        }
    }

    private fun findChild(
        context: Context,
        tree: Uri,
        parent: Uri,
        name: String,
    ): Pair<Uri, String>? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getDocumentId(parent),
        )
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        return context.contentResolver.query(children, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name) {
                    return@use DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)) to
                        cursor.getString(2)
                }
            }
            null
        }
    }
}
