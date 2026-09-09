package pl.chb.youtubeprogram

import android.content.Context
import android.net.Uri
import java.io.File

object AssetCache {
    fun copyUri(context:Context, uriText:String, prefix:String):File? {
        if(uriText.isBlank()) return null
        val uri=Uri.parse(uriText)
        val ext = when(context.contentResolver.getType(uri)) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "audio/mpeg" -> ".mp3"
            "audio/wav","audio/x-wav" -> ".wav"
            else -> ".dat"
        }
        val out=File(context.cacheDir,"${prefix}_${kotlin.math.abs(uriText.hashCode())}$ext")
        if(out.exists() && out.length()>0) return out
        context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } } ?: return null
        return out
    }
}
