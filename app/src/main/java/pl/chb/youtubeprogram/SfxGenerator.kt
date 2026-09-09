package pl.chb.youtubeprogram

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

object SfxGenerator {
    data class Event(val kind:String,val time:Double)
    fun writeWav(file:File,duration:Double,events:List<Event>,rate:Int=44100) {
        val total=(duration*rate).toInt().coerceAtLeast(1)
        val samples=FloatArray(total)
        fun tone(start:Double,len:Double,freq:Double,volume:Double) {
            val begin=(start*rate).toInt().coerceAtLeast(0); val count=(len*rate).toInt().coerceAtLeast(1)
            for(i in 0 until count) { val idx=begin+i; if(idx>=total) break; val decay=1f-i.toFloat()/count; samples[idx]+=(sin(2*PI*freq*i/rate)*volume*decay).toFloat() }
        }
        events.forEach { e -> when(e.kind) { "tick"->tone(e.time,.055,880.0,.12); "urgent"->tone(e.time,.07,1040.0,.18); "reveal"->tone(e.time,.16,760.0,.2); "level"->tone(e.time,.2,560.0,.16); "start"->tone(e.time,.2,640.0,.16); "finish"->tone(e.time,.3,520.0,.17) } }
        val peak=samples.maxOf{ kotlin.math.abs(it) }.coerceAtLeast(1f)
        RandomAccessFile(file,"rw").use { r ->
            fun le16(v:Int){r.write(byteArrayOf((v and 255).toByte(),((v shr 8) and 255).toByte()))}
            fun le32(v:Int){r.write(byteArrayOf((v and 255).toByte(),((v shr 8) and 255).toByte(),((v shr 16) and 255).toByte(),((v shr 24) and 255).toByte()))}
            r.setLength(0); r.writeBytes("RIFF"); le32(36+total*2); r.writeBytes("WAVEfmt "); le32(16); le16(1); le16(1); le32(rate); le32(rate*2); le16(2); le16(16); r.writeBytes("data"); le32(total*2)
            samples.forEach { x -> le16(((x/peak).coerceIn(-1f,1f)*32767).toInt()) }
        }
    }
}
