# Plugin-Kompatibilität (Spigot / Paper)

Diese Datei definiert das **Ziel (~80 % praktische Kompatibilität)** mit gängigen Spigot- und Paper-Plugins und sammelt **Testergebnisse**. Sie ersetzt keine automatisierten Tests, gibt aber Team und Nutzern eine gemeinsame Checkliste.

**Hinweis:** „80 %“ bedeutet hier: **ein großer Anteil üblicher Server-Plugins** soll ohne Patch funktionieren — **nicht**, dass jede einzelne API-Methode papieridentisch ist. Interna (NMS, `Craft*`-Reflection, Mischungen aus Mod + Plugin) sind oft **außerhalb** dieses Ziels.

Siehe auch: Fortschritt API-Upstreams im [README](README.md) (Spigot/Paper/PurPur).

---

## Loslegen — gemeinsamer Ablauf (Session 0)

So startet ihr **ohne Rätselraten**; derselbe Ablauf gilt für die ersten Einträge in den Tabellen unten.

### 0) Voraussetzungen

- **JDK 25**, **`JAVA_HOME`** gesetzt (siehe [list.md](list.md) Abschnitt 2, falls nötig).
- Repo geklont, Terminal im **Repository-Root** (`Youer/`).

### 1) Server bauen

```powershell
.\gradlew.bat setup youerJar
```

(Wenn der NeoDev-Fix fehlt: zuerst `setup` zuverlässig vor Kompilierung — siehe [list.md](list.md) Abschnitt 7.)

### 2) Entwicklungsserver starten (empfohlen für Plugin-Tests)

NeoForge-Run **„server“** — Arbeitsverzeichnis ist `projects/youer/run/server/` (siehe `neoDev { runs { … gameDirectory … } }` in `projects/youer/build.gradle`).

```powershell
.\gradlew.bat :neoforge:runServer
```

**Plugins ablegen:** Ordner **`projects/youer/run/server/plugins/`** anlegen (falls nicht vorhanden) und dort **nur ein** `.jar` pro Testrunde ablegen — Standard entspricht der Option `-P` / `--plugins` in `org.bukkit.craftbukkit.Main` (Default: `./plugins` relativ zum Server-Startverzeichnis).

**EULA:** Beim ersten Start ggf. `projects/youer/run/server/eula.txt` auf `eula=true` setzen (wie bei Vanilla).

**Stoppen:** Konsole mit `stop` beenden, nicht nur Terminal killen (Welt sauber speichern).

### 3) Erster gemeinsamer Test (Mini-Plan)

| Schritt | Was |
|--------|-----|
| 3.1 | Server einmal **ohne** Plugins starten → Welt lädt, kein Crash. |
| 3.2 | **Nur [Vault](https://github.com/MilkBowl/Vault)** (passende MC-Version) nach `…/run/server/plugins/` legen, neu starten. |
| 3.3 | Log prüfen: `Loading Vault` / kein roter Stacktrace beim `onEnable`. |
| 3.4 | Client joinen, `/plugins` — Vault **grün**. |
| 3.5 | Ergebnis in **Stufe A** bei Vault eintragen (Datum, Git-**Short-SHA** von `git rev-parse --short HEAD`, Plugin-JAR-Version, ✅/⚠️/❌, eine Kurznotiz). |

**Warum zuerst Vault?** Sehr klein, wenig Annahmen — guter **Rauchtest**, ob die Plugin-Pipeline überhaupt stimmt. Danach **LuckPerms** (häufig, etwas mehr API).

### 4) Optional: Distribution-Jar statt `runServer`

Das Task **`youerJar`** legt ein gebündeltes Jar unter **`projects/youer/build/libs/`** ab (Namen siehe `projects/youer/build.gradle`). Start und `plugins/` sind dann relativ zu dem Ordner, von dem aus ihr `java -jar …` aufruft — für Tests reicht meist **`runServer`**, bis ihr gezielt das Release-Jar prüfen wollt.

### Aktuelle Test-Session (zuletzt dokumentiert)

| Feld | Wert |
|------|------|
| Datum | 2026-04-22 (Ordner-Screenshot) · davor/u. a. 2026-04-23 (Einzeltests) |
| Youer / Git | siehe jeweilige Tabellenzeilen (`9ea8f2a4` …); nach Scheduler-Fix: neu bauen (`youerJar`) |
| Minecraft (laut `gradle.properties`) | 26.1.2 |
| **Referenz-Stack (alles grün, gemeinsam im `plugins/`)** | Citizens · EssentialsX + Chat + Spawn **2.22.0-dev+104** · LuckPerms **5.5.42** · Multiverse-Core **5.6.2-pre** · PacketEvents **2.12.1** · PlaceholderAPI **2.12.2** · ProtocolLib · Vault · Vulcan **2.9.7.17** · WorldEdit **7.4.3-beta-01** · WorldGuard **7.0.16** |
| Ergebnis | ✅ Gesamtstart mit obiger Liste ohne roten `onEnable`-Abbruch |
| Log / Issue | Vulcan: `Server.getRegionScheduler()` u. a. — Stubs in `paper-thread-stub` + `CraftServer`; siehe `CHANGELOG.md` |

---

## Status-Legende

| Symbol | Bedeutung |
|--------|-----------|
| ✅ | Funktioniert in euren Tests für die genannte Kombination |
| ⚠️ | Teilweise (Start ok, einzelne Features kaputt) |
| ❌ | Startet nicht oder Kernfunktionen gebrochen |
| ⬜ | Noch nicht getestet |

Eintrag bei Tests immer: **Youer-Build / Commit**, **Minecraft-Version**, **Plugin-Version**.

---

## Stufe A — Kern (soll ins „80 %-Ziel“ passen)

Plugins, die auf vielen Survival-/Mod-Servern Standard sind. Priorität für Ports und Bugfixes.

| Plugin | Typische API | Getestet (Datum / Build) | Status | Kurznotiz |
|--------|----------------|---------------------------|--------|-----------|
| [LuckPerms](https://luckperms.net/) | Spigot + Paper | 2026-04-23 / `9ea8f2a4`, MC 26.1.2, LP 5.5.42 (+ Vault 1.7.3-b131) | ✅ | Enable ok; H2 storage; „Running on Bukkit - Youer“; `/plugins` → 2 Plugins |
| [Vault](https://github.com/MilkBowl/Vault) | Spigot | 2026-04-23 / `9ea8f2a4`, MC 26.1.2, Vault 1.7.3-b131 | ✅ | Log + ingame `/plugins` → `Plugins (1): Vault` |
| [EssentialsX](https://github.com/EssentialsX/Essentials) | Spigot (+ ggf. Paper) | 2026-04-23 / `9ea8f2a4`, MC 26.1.2, EssX **2.22.0-dev+104** + Vault + LP | ✅ | Mixin `MixinMinecraftServer#z()`; Vault↔Essentials-Classloader-Warnung im Server unterdrückt (optionaler Economy-Bridge). |
| [WorldEdit](https://enginehub.org/worldedit) | Spigot / Paper | 2026-04-22 / MC 26.1.2, **7.4.3-beta-01** (im Referenz-Stack) | ✅ | Zusammen mit WorldGuard + übrigem Stack |
| [WorldGuard](https://enginehub.org/worldguard) | Spigot (+ Paper/Folia stubs) | 2026-04-24 / MC 26.1.2 | ✅ | `ScheduledTask`-Stub im Server-Jar — „does not exist“ beim Registrieren der Listener behoben |
| [CoreProtect](https://www.coreprotect.net/) | Spigot | ⬜ | ⬜ | |
| [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) | Spigot | ⬜ | ⬜ | |
| [dynmap](https://github.com/webbukkit/dynmap) | Spigot | ⬜ | ⬜ | Mod-Worlds ggf. extra |

---

## Stufe B — sehr verbreitet (wünschenswert in den 80 %)

| Plugin | Typische API | Getestet (Datum / Build) | Status | Kurznotiz |
|--------|----------------|---------------------------|--------|-----------|
| [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) | Spigot | 2026-04-22 / MC 26.1.2, **2.12.2** (Referenz-Stack) | ✅ | Enable ok im Gesamt-`plugins/`-Test |
| [Citizens](https://github.com/CitizensDev/Citizens2) | Spigot / NMS-lastig | 2026-04-24 / MC 26.1.2, Youer (AnvilMenu nicht `final`) | ✅ | War `IncompatibleClassChangeError` bei `final createResult`; behoben im Server-Patch |
| [Shopkeepers](https://github.com/Shopkeepers/Shopkeepers) | Spigot | ⬜ | ⬜ | |
| [GriefPrevention](https://github.com/TechFortress/GriefPrevention) | Spigot | ⬜ | ⬜ | |
| [Multiverse-Core](https://github.com/Multiverse/Multiverse-Core) | Spigot | 2026-04-22 / MC 26.1.2, **5.6.2-pre** (Referenz-Stack) | ✅ | Enable ok im Gesamt-`plugins/`-Test |
| [spark](https://spark.lucko.me/) | Spigot / Paper | ⬜ | ⬜ | |
| [Plan](https://github.com/plan-player-analytics/Plan) | Spigot / Paper | ⬜ | ⬜ | |

---

## Stufe C — oft problematisch (erwartbar oft ⚠️/❌)

Explizit **nicht** Voraussetzung für „80 %“, aber dokumentieren, wenn getestet.

| Plugin | Warum heikel | Getestet (Datum / Build) | Status | Kurznotiz |
|--------|----------------|---------------------------|--------|-----------|
| [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) | Paket-/NMS-Zugriff | 2026-04-23 / Youer + `commons-lang` 2.6 | ✅ | MC 26.1.2: Version-Warnung möglich; `Validate`-Fehler durch Server-Lib behoben — **neu bauen** (`youerJar`) |
| [PacketEvents](https://github.com/retrooper/packetevents) | Paket-API (netzwerk) | 2026-04-22/24 / MC 26.1.2, **2.12.1** (Referenz-Stack) | ✅ | Start/Enable ok |
| [Vulcan](https://www.spigotmc.org/resources/vulcan-advanced-cheat-detection.83663/) | Paper-Scheduler (`getRegionScheduler` …) | 2026-04-22 / MC 26.1.2, **2.9.7.17** (Referenz-Stack) | ✅ | Paper-Scheduler-Stubs auf BukkitScheduler gemappt |
| [ViaVersion](https://github.com/ViaVersion/ViaVersion) + Backwards | Protokoll | ⬜ | ⬜ | |
| [LibsDisguises](https://www.spigotmc.org/resources/libsdisguises.81/) | NMS | ⬜ | ⬜ | |
| Plugins mit **eigenem Paper-Patch** / Fork | Paper-Internals | ⬜ | ⬜ | |

---

## API-Schwerpunkte (für Entwicklung statt nur Plugin-roulette)

Wenn diese Bereiche stabil sind, steigen viele Plugins automatisch mit:

1. **Permissions / Vault-Hooks** — `PermissionDefault`, LuckPerms-Bridge  
2. **Scheduler (sync/async)** — korrekte Thread-Zuordnung zu Welt/Chunks  
3. **Events** — häufige Block/Entity/Player-Events, abgebrochene Events  
4. **Inventar / GUIs / Adventure (Komponenten)** — Paper nutzt Komponenten stark  
5. **Welten laden/erzeugen** — Multiverse, Void-Gen, Mod-Dimensionen  
6. **Commands / Brigadier** — Tab-Completion, Paper-Command-API wo genutzt  

Priorisiert Fixes nach **Stufe A** + obenstehender Liste, nicht nach „alle Paper-Commits“.

---

## So tragt ihr Ergebnisse ein

1. Server mit festem Youer-Build starten, **nur ein neues Plugin** pro Testrunde (sonst unklare Fehlerursache).  
2. Minimaltest: **Lädt**, **keine Fehler beim Join**, **1–2 Kernkommandos** des Plugins.  
3. In der Tabelle **Status** und **Kurznotiz** (ein Satz + Logzeile / Exception-Klasse) setzen.  
4. Bei ❌ Issue im Repo verlinken oder Ticket-Nummer in der Notiz.

---

## Ziel-Merkmal „~80 %“ (messbar machen)

Optional: Wenn **≥ 80 % der Einträge in Stufe A** ✅ sind (bei definierter MC-Version), gilt das interne Ziel für diese Stufe als erreicht. Stufe B zählt erst danach oder mit geringerem Gewicht.

Viele Stufe-A/B/C-Einträge sind jetzt mit konkreten Testdaten belegt; verbleibende ⬜ bitte nach euren Tests ergänzen.
