# Changelog

Alle nennenswerten Änderungen an diesem Projekt werden hier dokumentiert (Format angelehnt an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/)).

## [Unreleased]

### Behoben

- **Gradle / Parallel-Build:** `compileJava` hängt jetzt von `setup` ab, damit decompilierte Minecraft-Quellen in `projects/youer/src/...` synchronisiert sind, bevor die Kompilierung startet (`buildSrc/.../NeoDevPlugin.java`). Behebt fehlende `net.minecraft.*`-Pakete bei `org.gradle.parallel=true`.
- **NeoForge-Daten:** Ungültige `data/neoforge/loot_modifiers/global_loot_modifiers.json` entfernt (altes `replace`/`entries`-Format). Der aktuelle `LootModifierManager` erwartet pro Datei einen Modifier inkl. `type`-Feld — die Datei verursachte Parse-Fehler.
- **EssentialsX / ReflServerStateProvider:** Alias-Methode `boolean z()` auf `MinecraftServer` ergänzt (Mixin), entspricht dem von EssentialsX per Reflection erwarteten Spigot-Obfuskator für `isRunning()` ab 1.21.7 (`com.mohistmc.youer.mixins.minecraft.server.MixinMinecraftServer`).
- **Vault + Essentials:** Warnung „Loaded class … Economy … not a depend or softdepend“ unterdrückt für den üblichen Vault→Essentials-Economy-Bridge-Fall (`PluginClassLoader.isKnownOptionalPluginBridge`).
- **ProtocolLib / Commons Lang 2:** `commons-lang:commons-lang:2.6` zur NeoForge-`libraries`-Liste hinzugefügt, damit `org.apache.commons.lang.Validate` u. a. zur Laufzeit verfügbar sind (vorher `NoClassDefFoundError` beim `onLoad`).
- **Citizens / AnvilMenu:** NeoForge hatte `AnvilMenu#createResult()` als `final` gepatcht — Citizens überschreibt die Methode im NMS-Bridge (`CitizensAnvilMenu`). **`final` entfernt** (Patch unverändert bis auf fehlendes `final`), damit `IncompatibleClassChangeError` nicht mehr auftritt. Unterklassen sollten weiterhin **`createResultInternal()`** überschreiben, wenn das Neo-Anvil-Hook-Verhalten erhalten bleiben soll.
- **WorldGuard / Folia scheduler types:** Stub **`io.papermc.paper.threadedregions.scheduler.ScheduledTask`** als eigenes Subprojekt **`paper-thread-stub`** (JAR über NeoForge-`libraries`), nicht mehr unter `src/main/java` — dort verursachte dieselbe Package-Export JPMS-Kollision (**`ResolutionException`: minecraft und neoforge exportieren dasselbe Package**).
- **Vulcan / Paper Scheduler API:** Folia/Paper-Scheduler-Fläche erweitert: `RegionScheduler`, `GlobalRegionScheduler`, `AsyncScheduler` (im `paper-thread-stub`) sowie `Server`/`CraftServer`-Methoden `getRegionScheduler()`, `getGlobalRegionScheduler()`, `getAsyncScheduler()`. Nicht-Folia-Mapping über Bukkit-Scheduler ergänzt, damit Plugins wie Vulcan nicht mehr mit `NoSuchMethodError` auf `Server.getRegionScheduler()` abbrechen.
- **World-Config / Dimension-Mapping:** In `ConfigByWorlds.loadWorlds()` waren die Spezialordner vertauscht geprüft (`DIM-1`/`DIM1`). Korrigiert auf **`DIM-1` = Nether** (`allow-nether`) und **`DIM1` = End** (`allow-end`), damit Welt-Load/Create nicht fälschlich in der falschen Dimension landet.

### Dokumentation

- `list.md` — Windows-Build (JAVA_HOME, Gradle-Befehl, NeoDev-Fix, Changelog-Verweis optional).
- `plugins.md` — Kompatibilitätsmatrix und Testablauf (Spigot/Paper-Ziel ~80 %).

### Bekannt / ohne Code-Fix im Repo

- **JLine / `System::load`**, **JOML / `Unsafe`:** JVM-Warnungen unter Java 25; optional `--enable-native-access=ALL-UNNAMED` in der Startzeile.
- **Essentials „limited API“ / userdata:** Erwartbar bei Hybrid/Paper-API; `userdata` entsteht mit Spielbetrieb — kein Repo-Patch nötig, sofern keine weiteren Fehler auftreten.

---

## Hinweise zur Pflege

- Bei weiteren Fixes: Eintrag unter `[Unreleased]` ergänzen; bei Release `[Unreleased]` in eine Versionszeile mit Datum umbenennen und neuen `[Unreleased]`-Block anlegen.
