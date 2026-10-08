package com.wake.alarm.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Sounds and backgrounds are discovered by listing the asset folders, so adding or
 * replacing files needs no code change. Folders (relative to the assets directory):
 *   audio, then one subfolder per category, holding mp3, ogg, wav or m4a files
 *   backgrounds, holding jpg, png or webp files
 * (Avoid writing a slash followed by an asterisk in comments: Kotlin comments nest.)
 */
object AssetCatalog {
    val knownCategories = listOf("classic", "funny", "dramatic", "weird", "calm")
    private val audioExt = setOf("mp3", "ogg", "wav", "m4a", "aac", "flac")
    private val imageExt = setOf("jpg", "jpeg", "png", "webp")

    private fun ext(name: String) = name.substringAfterLast('.', "").lowercase()

    /** category -> asset paths, known categories first. */
    fun sounds(context: Context): Map<String, List<String>> {
        val dirs = runCatching { context.assets.list("audio")?.toList() }.getOrNull().orEmpty()
        val ordered = knownCategories + dirs.filter { it !in knownCategories }.sorted()
        val result = LinkedHashMap<String, List<String>>()
        for (cat in ordered) {
            val files = runCatching { context.assets.list("audio/$cat")?.toList() }.getOrNull().orEmpty()
                .filter { ext(it) in audioExt }
                .sorted()
                .map { "audio/$cat/$it" }
            result[cat] = files
        }
        return result
    }

    fun backgrounds(context: Context): List<String> =
        runCatching { context.assets.list("backgrounds")?.toList() }.getOrNull().orEmpty()
            .filter { ext(it) in imageExt }
            .sorted()
            .map { "backgrounds/$it" }

    fun defaultSound(context: Context): String {
        val all = sounds(context)
        return all["classic"]?.firstOrNull() ?: all.values.flatten().firstOrNull() ?: ""
    }

    fun defaultBackground(context: Context): String = backgrounds(context).firstOrNull() ?: ""

    fun prettyName(path: String): String =
        path.substringAfterLast('/').substringBeforeLast('.')
            .replace('_', ' ').replace('-', ' ')
            .replaceFirstChar { it.uppercase() }

    fun prettyCategory(cat: String): String = cat.replaceFirstChar { it.uppercase() }

    /** Decode an asset image, downsampled so big photos don't blow up memory. */
    fun decodeImage(context: Context, path: String, maxPx: Int): ImageBitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxPx || bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
