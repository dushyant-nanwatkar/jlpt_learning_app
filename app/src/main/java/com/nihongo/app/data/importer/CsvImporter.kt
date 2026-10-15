package com.nihongo.app.data.importer

import android.content.Context
import com.nihongo.app.data.database.entity.GrammarEntity
import com.nihongo.app.data.database.entity.VocabularyEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class CsvImporter(private val context: Context) {

    fun importGrammarFromStream(inputStream: InputStream): List<GrammarEntity> {
        val list = mutableListOf<GrammarEntity>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = readCsvLines(reader)
        reader.close()

        if (lines.isEmpty()) return list

        val startIndex = if (lines[0].contains("Grammar", ignoreCase = true) || lines[0].contains("Level", ignoreCase = true)) 1 else 0

        for (i in startIndex until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val cols = parseCsvLine(line)
            if (cols.size < 5) continue

            val level = cols.getOrNull(0)?.trim() ?: ""
            val grammarIdStr = cols.getOrNull(1)?.trim() ?: ""
            val grammarId = grammarIdStr.toIntOrNull() ?: (i + 1)
            val grammar = cols.getOrNull(2)?.trim() ?: ""
            val reading = cols.getOrNull(3)?.trim() ?: ""
            val meaning = cols.getOrNull(4)?.trim() ?: ""

            if (grammar.isBlank() && meaning.isBlank()) continue

            val notesList = mutableListOf<String>()
            for (nIdx in 5..9) {
                val note = cols.getOrNull(nIdx)?.trim()
                if (!note.isNullOrBlank()) {
                    notesList.add(note)
                }
            }
            val notesStr = if (notesList.isNotEmpty()) notesList.joinToString("\n") else null
            val source = cols.getOrNull(10)?.trim().takeIf { !it.isNullOrBlank() }

            val examplesArray = JSONArray()
            var eIdx = 11
            while (eIdx < cols.size - 1) {
                val jp = cols.getOrNull(eIdx)?.trim() ?: ""
                val en = cols.getOrNull(eIdx + 1)?.trim() ?: ""
                if (jp.isNotBlank()) {
                    val exObj = JSONObject()
                    exObj.put("japanese", jp)
                    exObj.put("english", en)
                    examplesArray.put(exObj)
                }
                eIdx += 2
            }

            list.add(
                GrammarEntity(
                    grammarId = grammarId,
                    jlptLevel = level.ifBlank { "N5" },
                    grammar = grammar,
                    reading = reading,
                    meaning = meaning,
                    notes = notesStr,
                    source = source,
                    examplesJson = examplesArray.toString()
                )
            )
        }

        return list
    }

    fun importCsvFromStream(inputStream: InputStream, jlptLevel: String): List<VocabularyEntity> {
        val list = mutableListOf<VocabularyEntity>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = readCsvLines(reader)
        reader.close()

        if (lines.isEmpty()) return list

        val firstLine = lines[0]
        val isBreakdownFormat = firstLine.contains("sub kanji", ignoreCase = true)
        val startIndex = if (firstLine.contains("Kanji", ignoreCase = true) || isBreakdownFormat || firstLine.contains("Japanese", ignoreCase = true)) 1 else 0

        for (i in startIndex until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val cols = parseCsvLine(line)
            if (cols.isEmpty()) continue

            var kanji = ""
            var kana = ""
            var romaji = ""
            var type = ""
            var meaning = ""
            var breakdownData: String? = null

            if (isBreakdownFormat) {
                kanji = cols.getOrNull(2)?.trim() ?: ""
                kana = cols.getOrNull(3)?.trim() ?: ""
                type = cols.getOrNull(4)?.trim() ?: ""
                meaning = cols.getOrNull(5)?.trim() ?: ""
                
                val breakdownObj = JSONObject()
                val subKanjis = JSONArray()
                
                var j = 6
                while (j < cols.size - 1) {
                    val subKanji = cols.getOrNull(j)?.trim()
                    val subMeaning = cols.getOrNull(j + 1)?.trim()
                    if (!subKanji.isNullOrBlank() && subKanji != "N/A" && !subMeaning.isNullOrBlank()) {
                        val subObj = JSONObject()
                        subObj.put("kanji", subKanji)
                        subObj.put("meaning", subMeaning)
                        subKanjis.put(subObj)
                    }
                    j += 2
                }
                
                breakdownObj.put("subKanjis", subKanjis)
                val collective = cols.lastOrNull()?.trim()
                if (!collective.isNullOrBlank() && collective != "N/A") {
                    breakdownObj.put("collectiveMeaning", collective)
                }
                
                if (subKanjis.length() > 0 || breakdownObj.has("collectiveMeaning")) {
                    breakdownData = breakdownObj.toString()
                }
            } else {
                kanji = cols.getOrNull(0)?.trim() ?: ""
                kana = cols.getOrNull(1)?.trim() ?: ""
                romaji = cols.getOrNull(2)?.trim() ?: ""
                type = cols.getOrNull(3)?.trim() ?: ""
                meaning = cols.getOrNull(4)?.trim() ?: ""
            }

            if (kanji.isBlank() && kana.isBlank()) continue

            list.add(
                VocabularyEntity(
                    japanese = if (kanji.isNotBlank()) kanji else kana,
                    reading = if (kana.isNotBlank()) kana else kanji,
                    romaji = romaji,
                    type = if (type.isNotBlank()) type else "Noun",
                    meaning = meaning,
                    jlptLevel = jlptLevel,
                    breakdownData = breakdownData
                )
            )
        }

        return list
    }

    fun importRelatedWordsMapFromStream(inputStream: InputStream): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = readCsvLines(reader)
        reader.close()

        if (lines.isEmpty()) return map

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val cols = parseCsvLine(line)
            if (cols.size < 7) continue

            val kanji = cols.getOrNull(2)?.trim() ?: ""
            val kana = cols.getOrNull(3)?.trim() ?: ""
            val key = if (kanji.isNotBlank()) kanji else kana
            if (key.isBlank()) continue

            val array = JSONArray()
            var j = 6
            while (j < cols.size) {
                val relWord = cols.getOrNull(j)?.trim() ?: ""
                val relReading = cols.getOrNull(j + 1)?.trim() ?: ""
                val relMeaning = cols.getOrNull(j + 2)?.trim() ?: ""
                val relBreakdown = cols.getOrNull(j + 3)?.trim() ?: ""

                if (relWord.isNotBlank() && relWord != "N/A") {
                    val obj = JSONObject()
                    obj.put("word", relWord)
                    obj.put("reading", relReading)
                    obj.put("meaning", relMeaning)
                    if (relBreakdown.isNotBlank() && relBreakdown != "N/A") {
                        obj.put("breakdown", relBreakdown)
                    }
                    array.put(obj)
                }
                j += 4
            }

            if (array.length() > 0) {
                map[key] = array.toString()
            }
        }

        return map
    }

    private fun readCsvLines(reader: BufferedReader): List<String> {
        val content = reader.readText()
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()
        var inQuotes = false

        for (i in content.indices) {
            val c = content[i]
            when (c) {
                '"' -> {
                    inQuotes = !inQuotes
                    currentLine.append(c)
                }
                '\n' -> {
                    if (inQuotes) {
                        currentLine.append(' ')
                    } else {
                        lines.add(currentLine.toString())
                        currentLine.clear()
                    }
                }
                '\r' -> {
                    if (inQuotes) {
                        currentLine.append(' ')
                    }
                }
                else -> currentLine.append(c)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        result.add(sb.toString())
        return result
    }
}
