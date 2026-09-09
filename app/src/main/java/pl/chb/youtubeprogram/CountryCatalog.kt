package pl.chb.youtubeprogram

import java.text.Normalizer
import java.util.Locale

data class Country(val code:String, val pl:String, val en:String)

object CountryCatalog {
    private val polish = Locale.forLanguageTag("pl-PL")
    private val english = Locale.ENGLISH

    private val overrides = mapOf(
        "GB" to ("Wielka Brytania" to "United Kingdom"),
        "US" to ("Stany Zjednoczone" to "United States"),
        "CZ" to ("Czechy" to "Czechia"),
        "KR" to ("Korea Południowa" to "South Korea"),
        "KP" to ("Korea Północna" to "North Korea"),
        "VA" to ("Watykan" to "Vatican City"),
        "CI" to ("Côte d’Ivoire" to "Côte d’Ivoire"),
        "TR" to ("Turcja" to "Türkiye"),
        "PS" to ("Terytoria Palestyńskie" to "Palestinian Territories")
    )

    val all: List<Country> = Locale.getISOCountries().map { code ->
        val pair = overrides[code]
        Country(
            code,
            pair?.first ?: Locale("", code).getDisplayCountry(polish),
            pair?.second ?: Locale("", code).getDisplayCountry(english)
        )
    }.sortedBy { it.en }

    fun normalize(value:String):String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("ł", "l")
        .replace("[^a-z0-9]".toRegex(), "")

    fun find(text:String):Country? {
        val raw=text.trim()
        if(raw.isBlank()) return null
        val parts=raw.split("/").map(::normalize)
        return all.firstOrNull { c ->
            val values=listOf(normalize(c.pl), normalize(c.en), normalize(c.code))
            parts.any { p -> values.any { it == p } }
        }
    }

    fun search(query:String):List<Country> {
        val q=normalize(query)
        if(q.isBlank()) return all
        return all.filter {
            normalize(it.pl).contains(q) || normalize(it.en).contains(q) || normalize(it.code).contains(q)
        }
    }

    fun label(c:Country, language:String) = if(language == LANG_EN) "${c.en} / ${c.pl}" else "${c.pl} / ${c.en}"
    fun localized(c:Country, language:String) = if(language == LANG_EN) c.en else c.pl
}
