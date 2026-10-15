package com.nihongo.app.data.database.entity

import org.json.JSONObject

data class BreakdownData(
    val subKanjis: List<SubKanji>,
    val collectiveMeaning: String?
) {
    companion object {
        fun fromJson(jsonString: String?): BreakdownData? {
            if (jsonString.isNullOrBlank()) return null
            return try {
                val obj = JSONObject(jsonString)
                val subKanjisArray = obj.optJSONArray("subKanjis")
                val list = mutableListOf<SubKanji>()
                if (subKanjisArray != null) {
                    for (i in 0 until subKanjisArray.length()) {
                        val subObj = subKanjisArray.getJSONObject(i)
                        list.add(
                            SubKanji(
                                kanji = subObj.optString("kanji"),
                                meaning = subObj.optString("meaning")
                            )
                        )
                    }
                }
                val collectiveMeaning = if (obj.has("collectiveMeaning")) obj.optString("collectiveMeaning") else null
                BreakdownData(list, collectiveMeaning)
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class SubKanji(
    val kanji: String,
    val meaning: String
)
