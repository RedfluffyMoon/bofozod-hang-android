# 👋 Slap Sound – Android App

Egy vicces Android alkalmazás, amely érzékeli, ha megütöd a telefont, és különböző hangokat játszik le!

---

## Mi ez az alkalmazás?

A **Slap Sound** egy szórakoztató Android app, amely a telefon beépített **gyorsulásmérő szenzorát** (accelerometer) használja arra, hogy észlelje, ha megütöd vagy erősen megemeled a telefont. Minden érzékelt ütésnél az app egy véletlenszerűen kiválasztott hangeffektet játszik le.

Az alkalmazás a [taigrr/spank](https://github.com/taigrr/spank) macOS-es project ötletéből merített ihletet, és azt valósítja meg Android platformon.

---

## Hogyan működik?

1. **Szenzor figyelés**: Az app folyamatosan olvassa a gyorsulásmérő adatait.
2. **Ütés detektálás**: Kiszámolja a gyorsulás nagyságát (`√(x² + y² + z²)`), majd levonja a gravitáció értékét (~9,81 m/s²). Ha az eredmény meghaladja a beállított küszöbértéket, az app ütést érzékel.
3. **Hanglejátszás**: Ütés esetén az app véletlenszerűen kiválaszt egy hangot az aktív mód hangkészletéből, és lejátssza azt.
4. **Cooldown**: Két egymást követő érzékelés között legalább 750 ms-nak kell eltelnie (alapértelmezetten).

### Módok

| Mód | Leírás |
|-----|--------|
| 😣 **Pain mode** | Fájdalomra utaló, vicces hangok |
| 🤣 **Funny mode** | Humoros, rajzfilmes hangeffektek |

---

## Képernyők és funkciók

- **Állapot jelző**: „👂 Listening..." vagy „💥 SLAP DETECTED!" (vörös villanással)
- **Ütésszámláló**: Az aktuális munkamenet ütéseinek száma (visszaállítható)
- **Érzékenység csúszka**: Az érzékelési küszöbérték 5–40 m/s² között állítható
- **Start/Stop gomb**: Az érzékelés be- és kikapcsolása
- **Mód választó**: Pain / Funny mód váltása
- **Háttérszolgáltatás**: Az app a háttérben is figyeli az ütéseket (értesítéssel)
- **Sötét mód**: Teljes dark mode támogatás (Material You / Material Design 3)

---

## Hogyan kell buildelni?

### Szükséges eszközök

- [Android Studio](https://developer.android.com/studio) (Hedgehog 2023.1.1 vagy újabb)
- JDK 17 (Android Studio-val együtt települ)
- Android SDK API 34 (Android 14)

### Lépések

1. **Klónozd a repót:**
   ```bash
   git clone https://github.com/RedfluffyMoon/slap-sound-android.git
   cd slap-sound-android
   ```

2. **Nyisd meg Android Studio-ban:**
   - Indítsd el az Android Studio-t
   - Kattints: **File → Open** → válaszd ki a klónozott mappát
   - Várd meg, amíg a Gradle sync befejeződik

3. **Buildeld az appot:**
   - Kattints a **▶ Run** gombra, vagy
   - Terminálban: `./gradlew assembleDebug`
   - Az APK a `app/build/outputs/apk/debug/` mappában lesz

4. **Futtasd a telefonon:**
   - Engedélyezd a fejlesztői módot és az USB debuggolást a telefonon
   - Csatlakoztasd USB-vel és kattints **▶ Run**

### Minimum követelmények

| | Érték |
|---|---|
| Min. Android verzió | Android 7.0 (API 24) |
| Target Android verzió | Android 14 (API 34) |
| Szükséges hardver | Gyorsulásmérő szenzor |

---

## Beállítások

### Érzékenység (Threshold)

Az érzékenységet a főképernyőn lévő csúszkával lehet állítani:

| Érték | Leírás |
|-------|--------|
| 5–12 m/s² | Nagyon érzékeny – könnyű érintés is elindítja |
| 12–20 m/s² | Normál – határozott ütésre reagál (alapértelmezett: 15 m/s²) |
| 20–30 m/s² | Kevésbé érzékeny – erős ütés kell |
| 30–40 m/s² | Csak erős csapásra reagál |

### Módok

- **Pain mode** (alapértelmezett): „Jaj!" típusú, fájdalmat imitáló hangok
- **Funny mode**: Vicces, rajzfilmes hangeffektek

### Háttér detektálás

Az app a háttérben is fut (előtér szolgáltatásként), és egy értesítésben mutatja az ütések számát. Az értesítés a Start gomb megnyomásakor jelenik meg, és a Stop gombra eltűnik.

> **Megjegyzés:** Android 13-tól (API 33) az értesítésekhez engedélyt kell adni. Az app ezt automatikusan kéri az első indításkor.

---

## Projektstruktúra

```
app/src/main/
├── kotlin/com/redfluffymoon/slapsound/
│   ├── MainActivity.kt          # Fő képernyő (Jetpack Compose UI)
│   ├── SlapViewModel.kt         # ViewModel – szenzor + hang logika
│   ├── SensorHelper.kt          # Gyorsulásmérő kezelés
│   ├── SoundManager.kt          # SoundPool hanglejátszás
│   ├── SlapDetectionService.kt  # Háttér előtér-szolgáltatás
│   └── ui/theme/                # Material You téma
├── res/
│   └── raw/                     # Hangfájlok (WAV)
│       ├── pain1–5.wav
│       └── funny1–5.wav
└── AndroidManifest.xml
```

---

## Inspiráció

Ez az alkalmazás a [taigrr/spank](https://github.com/taigrr/spank) nyílt forráskódú projekt Android adaptációja.
Az eredeti projekt macOS-re készült (Apple Silicon), és az ütés érzékelésére a Mac gyorsulásmérőjét használja.

---

## Licenc

MIT License – szabadon felhasználható, módosítható és terjeszthető.
