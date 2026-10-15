# Exported JLPT App CSV Data

This folder contains all the CSV data files used within the JLPT Learning App, organized by usage.

## 📂 Folder Structure

```
exported_csv_data/
├── active_app_data/                  # CSV files actively loaded by the app at runtime
│   ├── grammar/
│   │   └── jlpt_grammar.csv          # Master grammar points and examples (N5, N4, N3)
│   ├── vocabulary_breakdown/
│   │   ├── jlpt_n5_breakdown.csv     # N5 vocabulary with kanji, reading, meaning & radical breakdown
│   │   ├── jlpt_n4_breakdown.csv     # N4 vocabulary breakdown
│   │   └── jlpt_n3_breakdown.csv     # N3 vocabulary breakdown
│   └── related_vocabulary/
│       ├── jlpt_n5_related_vocab.csv # N5 related vocabulary JSON mappings
│       ├── jlpt_n4_related_vocab.csv # N4 related vocabulary JSON mappings
│       └── jlpt_n3_related_vocab.csv # N3 related vocabulary JSON mappings
│
└── source_data/                      # Original source datasets and legacy asset copies
    ├── JLPT_Grammar_N5_N4_N3_natural_usage_examples.csv
    ├── jlpt_n5_breakdown_final.csv
    ├── jlpt_n4_breakdown_final.csv
    ├── jlpt_n3_breakdown_final.csv
    ├── dictionary/
    │   ├── jlpt_n5_related_vocab.csv
    │   ├── jlpt_n4_related_vocab.csv
    │   └── jlpt_n3_related_vocab.csv
    └── legacy_assets/
        ├── N5Vocab.csv
        ├── N4Vocab.csv
        ├── N3Vocab.csv
        └── jlpt_n4_breakdown_updated.csv
```

---

## 📑 Data Descriptions & App Usage

### 1. Grammar Points (`active_app_data/grammar/jlpt_grammar.csv`)
- **App Usage:** Loaded by [`GrammarViewModel.kt`](file:///C:/Users/dushy/StudioProjects/jlpt_learning_app/app/src/main/java/com/nihongo/app/ui/grammar/GrammarViewModel.kt) on initial database sync.
- **Columns:** `Level`, `Grammar ID`, `Grammar`, `Reading`, `Meaning`, `Notes 1..5`, `Source`, `Example 1..10 (Japanese & English)`.

### 2. Vocabulary Breakdown (`active_app_data/vocabulary_breakdown/`)
- **App Usage:** Loaded by [`VocabularyViewModel.kt`](file:///C:/Users/dushy/StudioProjects/jlpt_learning_app/app/src/main/java/com/nihongo/app/ui/vocabulary/VocabularyViewModel.kt) for JLPT levels N5, N4, and N3.
- **Columns:** `#`, `lvl`, `kanji`, `furigana/vocab`, `type`, `meaning`, `sub kanji/radical 1..12`, `meaning of sub kanji/radical 1..12`, `collective meaning of the radicals`.

### 3. Related Vocabulary (`active_app_data/related_vocabulary/`)
- **App Usage:** Loaded alongside breakdown CSVs to populate compound words and related expressions.
- **Columns:** `Word`, `Related Words (JSON)`.

---

## 🛠 How Data Importers Work
- **CSV Importer Class:** [`CsvImporter.kt`](file:///C:/Users/dushy/StudioProjects/jlpt_learning_app/app/src/main/java/com/nihongo/app/data/importer/CsvImporter.kt) handles parsing quote-escaped CSV lines and mapping them to Room database entities (`VocabularyEntity` and `GrammarEntity`).
