package pl.chb.youtubeprogram

import org.json.JSONArray
import org.json.JSONObject

const val LANG_PL = "Polski"
const val LANG_EN = "English"

fun pointsFor(level:String) = when(level) {
    "Średni" -> 2
    "Trudny" -> 3
    else -> 1
}

data class QuizQuestion(
    var difficulty:String = "Łatwy",
    var flagUri:String = "",
    var backgroundUri:String = "",
    var answerCodes:MutableList<String> = MutableList(4){""},
    var answers:MutableList<String> = MutableList(4){""},
    var correctIndex:Int = 0,
)

data class QuizProject(
    var title:String = "Can You Guess 30 Flags?",
    var subtitle:String = "Easy to Impossible Flag Quiz",
    var language:String = LANG_EN,
    var questionSeconds:Double = 6.0,
    var revealSeconds:Double = 2.0,
    var introSeconds:Double = 2.2,
    var levelIntroSeconds:Double = 1.4,
    var outroSeconds:Double = 3.0,
    var answerCount:Int = 4,
    var soundEffects:Boolean = true,
    var retentionMode:Boolean = true,
    var backgroundMode:String = "none",
    var globalBackgroundUri:String = "",
    var levelBackgroundUris:MutableMap<String,String> = mutableMapOf("Łatwy" to "", "Średni" to "", "Trudny" to ""),
    var backgroundBlur:Int = 20,
    var backgroundDim:Int = 45,
    var musicUri:String = "",
    var musicVolume:Int = 20,
    var questions:MutableList<QuizQuestion> = mutableListOf()
) {
    fun toJson():String {
        val o=JSONObject()
        o.put("title",title); o.put("subtitle",subtitle); o.put("language",language)
        o.put("questionSeconds",questionSeconds); o.put("revealSeconds",revealSeconds)
        o.put("introSeconds",introSeconds); o.put("levelIntroSeconds",levelIntroSeconds); o.put("outroSeconds",outroSeconds)
        o.put("answerCount",answerCount); o.put("soundEffects",soundEffects); o.put("retentionMode",retentionMode)
        o.put("backgroundMode",backgroundMode); o.put("globalBackgroundUri",globalBackgroundUri)
        o.put("backgroundBlur",backgroundBlur); o.put("backgroundDim",backgroundDim)
        o.put("musicUri",musicUri); o.put("musicVolume",musicVolume)
        val levels=JSONObject(); levelBackgroundUris.forEach{(k,v)->levels.put(k,v)}; o.put("levelBackgroundUris",levels)
        val qs=JSONArray()
        questions.forEach { q ->
            val jq=JSONObject(); jq.put("difficulty",q.difficulty); jq.put("flagUri",q.flagUri); jq.put("backgroundUri",q.backgroundUri)
            jq.put("answerCodes",JSONArray(q.answerCodes)); jq.put("answers",JSONArray(q.answers)); jq.put("correctIndex",q.correctIndex); qs.put(jq)
        }
        o.put("questions",qs)
        return o.toString(2)
    }

    companion object {
        fun fromJson(text:String):QuizProject {
            val o=JSONObject(text); val p=QuizProject()
            p.title=o.optString("title",p.title); p.subtitle=o.optString("subtitle",p.subtitle); p.language=o.optString("language",p.language)
            p.questionSeconds=o.optDouble("questionSeconds",p.questionSeconds); p.revealSeconds=o.optDouble("revealSeconds",p.revealSeconds)
            p.introSeconds=o.optDouble("introSeconds",p.introSeconds); p.levelIntroSeconds=o.optDouble("levelIntroSeconds",p.levelIntroSeconds); p.outroSeconds=o.optDouble("outroSeconds",p.outroSeconds)
            p.answerCount=o.optInt("answerCount",p.answerCount); p.soundEffects=o.optBoolean("soundEffects",p.soundEffects); p.retentionMode=o.optBoolean("retentionMode",p.retentionMode)
            p.backgroundMode=o.optString("backgroundMode",p.backgroundMode); p.globalBackgroundUri=o.optString("globalBackgroundUri","")
            p.backgroundBlur=o.optInt("backgroundBlur",20); p.backgroundDim=o.optInt("backgroundDim",45)
            p.musicUri=o.optString("musicUri",""); p.musicVolume=o.optInt("musicVolume",20)
            val lv=o.optJSONObject("levelBackgroundUris"); listOf("Łatwy","Średni","Trudny").forEach{p.levelBackgroundUris[it]=lv?.optString(it,"") ?: ""}
            val qs=o.optJSONArray("questions") ?: JSONArray(); p.questions.clear()
            for(i in 0 until qs.length()) {
                val qj=qs.getJSONObject(i); val q=QuizQuestion(); q.difficulty=qj.optString("difficulty","Łatwy"); q.flagUri=qj.optString("flagUri",""); q.backgroundUri=qj.optString("backgroundUri","")
                val codes=qj.optJSONArray("answerCodes"); val ans=qj.optJSONArray("answers")
                q.answerCodes=MutableList(4){idx->codes?.optString(idx,"") ?: ""}; q.answers=MutableList(4){idx->ans?.optString(idx,"") ?: ""}; q.correctIndex=qj.optInt("correctIndex",0).coerceIn(0,3)
                p.questions.add(q)
            }
            return p
        }
    }
}
