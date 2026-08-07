Звукові асети (спек debt-tracker-kmp-spec.md, §1.1 п.4):
  add.wav          — 130 мс, висхідний двотон (позитивна дія)
  delete.wav       — 160 мс, низхідний двотон (негативна дія)
  dialog_open.wav  — 90 мс, нейтральний тон

Згенеровані програмно (синус + fade in/out), без сторонніх ліцензій —
WAV, а не MP3, свідомо: javax.sound.sampled (Desktop) з коробки декодує
лише WAV, MP3 вимагає додаткового плагіна. Завантажуються й відтворюються
в core/sound/SoundPlayer.*.kt на кожній платформі через
compose.resources (Res.readBytes/Res.getUri) — саму назву файлу для
SoundEffect визначає SoundEffect.assetFileName (core/sound/SoundPlayer.kt).
