package pl.chb.youtubeprogram

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import kotlin.math.ceil

object VideoExporter {
    data class Result(val file:File,val duration:Double)
    private fun q(path:String)="'${path.replace("'", "'\\''")}'"

    fun render(context:Context,p:QuizProject,onProgress:(Int,String)->Unit = { _, _ -> }):Result {
        val valid=p.questions.filter{it.flagUri.isNotBlank() && it.answers.take(p.answerCount).any(String::isNotBlank)}
        require(valid.isNotEmpty()){"Brak kompletnych pytań."}
        val work=File(context.cacheDir,"render_${System.currentTimeMillis()}").apply{mkdirs()}
        val segments=mutableListOf<Pair<File,Double>>(); val events=mutableListOf<SfxGenerator.Event>(); var timeline=0.0; var n=0
        fun add(bitmap:android.graphics.Bitmap,duration:Double,name:String){ val f=File(work,"${n.toString().padStart(4,'0')}_${name}.png"); FrameRenderer.writePng(bitmap,f); segments+=f to duration; n++; timeline+=duration; onProgress((segments.size*100/(valid.size*8+8)).coerceAtMost(88),"Przygotowywanie klatek…") }
        val totalPoints=valid.sumOf{pointsFor(it.difficulty)}
        events+=SfxGenerator.Event("start",.05); add(FrameRenderer.intro(context,p,totalPoints),p.introSeconds,"intro")
        val levels=valid.map{it.difficulty}.distinct(); var qi=0
        levels.forEachIndexed { li,level ->
            events+=SfxGenerator.Event("level",timeline+.04); add(FrameRenderer.level(context,p,level,li+1,levels.size),p.levelIntroSeconds,"level")
            valid.filter{it.difficulty==level}.forEach { question ->
                qi++
                for(sec in ceil(p.questionSeconds).toInt() downTo 1){
                    events+=SfxGenerator.Event(if(sec<=2)"urgent" else "tick",timeline+.03)
                    add(FrameRenderer.question(context,p,question,qi,valid.size,sec.toDouble(),false),minOf(1.0,p.questionSeconds-(ceil(p.questionSeconds).toInt()-sec)),"q${qi}_$sec")
                }
                events+=SfxGenerator.Event("reveal",timeline+.02); add(FrameRenderer.question(context,p,question,qi,valid.size,0.0,true),p.revealSeconds,"q${qi}_reveal")
            }
        }
        events+=SfxGenerator.Event("finish",timeline+.05); add(FrameRenderer.outro(context,p,totalPoints),p.outroSeconds,"outro")

        val concat=File(work,"frames.txt")
        concat.bufferedWriter().use { w ->
            segments.forEach { (f,d)-> w.appendLine("file ${q(f.absolutePath)}"); w.appendLine("duration ${"%.4f".format(java.util.Locale.US,d)}") }
            w.appendLine("file ${q(segments.last().first.absolutePath)}")
        }
        val sfx=File(work,"sfx.wav"); if(p.soundEffects)SfxGenerator.writeWav(sfx,timeline+.2,events)
        val music=AssetCache.copyUri(context,p.musicUri,"music")
        val out=File(work,"quiz.mp4")
        val audio=when {
            p.soundEffects && music!=null && p.musicVolume>0 -> "-i ${q(sfx.absolutePath)} -stream_loop -1 -i ${q(music.absolutePath)} -filter_complex \"[1:a]volume=1.0[s];[2:a]volume=${p.musicVolume/100.0}[m];[s][m]amix=inputs=2:duration=first[a]\" -map 0:v -map \"[a]\" -c:a aac -b:a 192k"
            p.soundEffects -> "-i ${q(sfx.absolutePath)} -map 0:v -map 1:a -c:a aac -b:a 160k"
            music!=null && p.musicVolume>0 -> "-stream_loop -1 -i ${q(music.absolutePath)} -filter_complex \"[1:a]volume=${p.musicVolume/100.0}[a]\" -map 0:v -map \"[a]\" -c:a aac -b:a 192k -shortest"
            else -> "-an"
        }
        val command="-y -f concat -safe 0 -i ${q(concat.absolutePath)} $audio -vf fps=30,format=yuv420p -c:v h264_mediacodec -b:v 6000k -movflags +faststart ${q(out.absolutePath)}"
        onProgress(90,"Kodowanie MP4…")
        var session=FFmpegKit.execute(command)
        if(!ReturnCode.isSuccess(session.returnCode)) {
            val fallback=command.replace("-c:v h264_mediacodec -b:v 6000k","-c:v mpeg4 -q:v 3")
            session=FFmpegKit.execute(fallback)
        }
        if(!ReturnCode.isSuccess(session.returnCode)) error(session.allLogsAsString.takeLast(2500))
        onProgress(100,"Gotowe")
        return Result(out,timeline)
    }

    fun copyToUri(context:Context,file:File,destination:Uri){ context.contentResolver.openOutputStream(destination,"w")!!.use{out->file.inputStream().use{it.copyTo(out)}} }
}
