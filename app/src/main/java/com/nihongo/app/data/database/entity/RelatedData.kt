package com.nihongo.app.data.database.entity

import org.json.JSONArray

data class RelatedWord(
    val word: String,
    val reading: String,
    val meaning: String,
    val breakdown: String? = null
)

data class RelatedData(
    val relatedWords: List<RelatedWord>
) {
    companion object {
        fun fromJson(jsonString: String?): RelatedData? {
            if (jsonString.isNullOrBlank()) return null
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<RelatedWord>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val word = obj.optString("word")
                    val reading = obj.optString("reading")
                    val meaning = obj.optString("meaning")
                    val breakdown = if (obj.has("breakdown")) obj.optString("breakdown") else null
                    if (word.isNotBlank()) {
                        list.add(RelatedWord(word, reading, meaning, breakdown))
                    }
                }
                if (list.isEmpty()) null else RelatedData(list)
            } catch (e: Exception) {
                null
            }
        }
    }
}
