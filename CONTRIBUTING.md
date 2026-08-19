# Contributing to WaitLingo

WaitLingo welcomes code and vocabulary contributions. The plugin stays fully offline: do not add telemetry, accounts, or network-only learning features.

## Vocabulary packs

Built-in packs live in `src/main/resources/vocabulary`. Each JSON entry uses:

```json
{
  "word": "refactor",
  "translation": "yeniden düzenlemek",
  "translations": { "tr": "yeniden düzenlemek" },
  "examples": ["We refactored the service."],
  "category": "developer",
  "level": "B1",
  "tags": ["developer", "code-quality"]
}
```

Keep examples original, concise, natural, and free of private information. Use a CEFR level from A1 to C2 and lowercase tags. Validate JSON and run `gradlew.bat test buildPlugin verifyPlugin` before opening a pull request.

Users can also load a personal `.json` file with this schema or a UTF-8 CSV containing `word,translation,example,category,level,tags`; separate multiple tags with `|`.
