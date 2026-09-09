package pl.chb.youtubeprogram

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object FrameRenderer {
    const val W=1920; const val H=1080
    private val bg=Color.rgb(17,20,28); private val panel=Color.rgb(29,34,46); private val gold=Color.rgb(240,190,62); private val green=Color.rgb(55,186,120)

    private fun paint(size:Float,bold:Boolean=false,color:Int=Color.WHITE)=Paint(Paint.ANTI_ALIAS_FLAG).apply{ textSize=size;color.also{this.color=it};typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT }
    private fun textCenter(c:Canvas,t:String,y:Float,p:Paint){c.drawText(t,(W-p.measureText(t))/2f,y,p)}
    private fun round(c:Canvas,l:Float,t:Float,r:Float,b:Float,rad:Float,color:Int){val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{this.color=color};c.drawRoundRect(l,t,r,b,rad,rad,p)}

    private fun decode(context:Context, uriText:String):Bitmap? = try { if(uriText.isBlank()) null else context.contentResolver.openInputStream(Uri.parse(uriText))?.use{BitmapFactory.decodeStream(it)} } catch(_:Exception){null}
    private fun background(context:Context, uri:String, blur:Int, dim:Int):Bitmap {
        val base=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888); val c=Canvas(base); c.drawColor(bg)
        val src=decode(context,uri) ?: return base
        val scale=max(W/src.width.toFloat(),H/src.height.toFloat()); val dw=(src.width*scale).toInt(); val dh=(src.height*scale).toInt(); val x=(W-dw)/2f; val y=(H-dh)/2f
        var work=Bitmap.createScaledBitmap(src,dw,dh,true)
        if(blur>0){ val factor=(1f-blur.coerceIn(0,100)/110f).coerceAtLeast(.08f); val sw=max(12,(dw*factor).toInt()); val sh=max(12,(dh*factor).toInt()); work=Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(work,sw,sh,true),dw,dh,true) }
        c.drawBitmap(work,x,y,null); c.drawColor(Color.argb((dim.coerceIn(0,100)*2.55).toInt(),0,0,0)); return base
    }

    fun backgroundUri(p:QuizProject,q:QuizQuestion?=null,level:String?=null):String = when(p.backgroundMode){
        "global"->p.globalBackgroundUri; "level"->p.levelBackgroundUris[level ?: q?.difficulty] ?: ""; "country"->q?.backgroundUri ?: ""; else->""
    }

    fun intro(context:Context,p:QuizProject,totalPoints:Int):Bitmap { val b=background(context,backgroundUri(p),p.backgroundBlur,p.backgroundDim); val c=Canvas(b); textCenter(c,if(p.language==LANG_EN)"FLAG QUIZ" else "QUIZ FLAG",250f,paint(72f,true,gold)); textCenter(c,p.title,390f,paint(58f,true)); textCenter(c,p.subtitle,470f,paint(34f,false,Color.LTGRAY)); textCenter(c,if(p.language==LANG_EN)"How many points can you score?" else "Ile punktów zdobędziesz?",610f,paint(42f,true)); textCenter(c,"MAX $totalPoints",720f,paint(54f,true,gold)); return b }
    fun level(context:Context,p:QuizProject,level:String,index:Int,total:Int):Bitmap { val b=background(context,backgroundUri(p,level=level),p.backgroundBlur,p.backgroundDim); val c=Canvas(b); val pts=pointsFor(level); textCenter(c,if(p.language==LANG_EN)"LEVEL $index / $total" else "POZIOM $index / $total",300f,paint(42f,true,gold)); val name=if(p.language==LANG_EN) when(level){"Łatwy"->"EASY";"Średni"->"MEDIUM";else->"HARD"} else level.uppercase(); textCenter(c,name,480f,paint(88f,true)); textCenter(c,"+$pts",650f,paint(88f,true,gold)); textCenter(c,if(p.language==LANG_EN)"for every correct answer" else "za każdą poprawną odpowiedź",730f,paint(34f)); return b }

    fun question(context:Context,p:QuizProject,q:QuizQuestion,index:Int,total:Int,remaining:Double,reveal:Boolean):Bitmap {
        val b=background(context,backgroundUri(p,q),p.backgroundBlur,p.backgroundDim); val c=Canvas(b)
        c.drawText("${index}/${total}",80f,70f,paint(30f,true)); val diff=if(p.language==LANG_EN) when(q.difficulty){"Łatwy"->"EASY";"Średni"->"MEDIUM";else->"HARD"} else q.difficulty.uppercase(); c.drawText(diff,1650f,70f,paint(30f,true,gold))
        round(c,80f,105f,1840f,118f,7f,Color.rgb(48,54,68)); round(c,80f,105f,80f+1760f*(index.toFloat()/total),118f,7f,gold)
        val left=total-index+1; if(p.retentionMode && left<=3){ val label=if(p.language==LANG_EN) if(left==1)"FINAL QUESTION" else "FINAL $left" else if(left==1)"OSTATNIE PYTANIE" else "OSTATNIE $left"; textCenter(c,label,85f,paint(24f,true,gold)) }
        textCenter(c,if(p.language==LANG_EN)"Which country is this?" else "Jaki to kraj?",180f,paint(42f,true))
        decode(context,q.flagUri)?.let { flag -> val maxW=760f; val maxH=410f; val sc=min(maxW/flag.width,maxH/flag.height); val dw=(flag.width*sc).toInt(); val dh=(flag.height*sc).toInt(); val x=(W-dw)/2f; val y=215f; val dst=RectF(x,y,x+dw,y+dh); c.drawBitmap(flag,null,dst,Paint(Paint.ANTI_ALIAS_FLAG)) }
        val sec=max(1,ceil(remaining).toInt()); if(!reveal){ round(c,820f,610f,1100f,665f,18f,Color.rgb(39,45,58)); textCenter(c,sec.toString(),652f,paint(38f,true,if(sec<=2)Color.rgb(255,95,70) else gold)) }
        val count=p.answerCount.coerceIn(2,4); val startY=710f; val pw=780f; val ph=112f; val gapX=40f; val gapY=28f
        for(i in 0 until count){ val row=i/2; val col=i%2; val x=160f+col*(pw+gapX); val y=startY+row*(ph+gapY); val correct=reveal && i==q.correctIndex; round(c,x,y,x+pw,y+ph,22f,if(correct)green else panel); val country=q.answerCodes.getOrNull(i)?.let{code->CountryCatalog.all.firstOrNull{it.code==code}}; val label=country?.let{CountryCatalog.localized(it,p.language)} ?: q.answers.getOrElse(i){""}; c.drawText("${('A'.code+i).toChar()}. $label",x+35f,y+70f,paint(34f,true)); if(correct)c.drawText("+${pointsFor(q.difficulty)}",x+pw-95f,y+70f,paint(36f,true,Color.WHITE)) }
        return b
    }
    fun outro(context:Context,p:QuizProject,totalPoints:Int):Bitmap { val b=background(context,backgroundUri(p),p.backgroundBlur,p.backgroundDim); val c=Canvas(b); textCenter(c,if(p.language==LANG_EN)"HOW MANY POINTS DID YOU GET?" else "ILE ZDOBYŁEŚ PUNKTÓW?",400f,paint(64f,true)); textCenter(c,"/ $totalPoints",545f,paint(72f,true,gold)); textCenter(c,if(p.language==LANG_EN)"Share your score in the comments!" else "Pochwal się wynikiem w komentarzu!",690f,paint(38f,true)); return b }
    fun writePng(bitmap:Bitmap,file:File){file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,95,it)}}
}
