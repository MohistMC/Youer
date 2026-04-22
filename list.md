# Youer / NeoForge build — what you need and what was fixed

This is a checklist for building the project on **Windows** without help from the repo owner.

---

## 1. What you need installed

| Requirement | Notes |
|---------------|--------|
| **Git** | Clone the repository. |
| **JDK 25** | The project pins `java_version=25` in `gradle.properties`. Example: [Eclipse Temurin](https://adoptium.net/) JDK 25. |
| **Internet** | Gradle downloads Minecraft jars, NeoForm data, libraries, and the Gradle distribution on first run. |

Optional but useful: enough RAM and disk space (decompilation and caches are large).

---

## 2. Set `JAVA_HOME` (required on Windows)

If you run `gradlew.bat` and see:

```text
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

then Java is not configured for Gradle.

**Do this once (system or user environment variables):**

1. Install JDK 25 (e.g. to `C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot` — your path may differ).
2. Set **`JAVA_HOME`** to that folder (not to `bin`).
3. Add **`%JAVA_HOME%\bin`** to **`PATH`**.

**Or temporarily in PowerShell before building:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
```

Adjust the path if your JDK is elsewhere.

Verify:

```powershell
java -version
echo $env:JAVA_HOME
```

---

## 3. Build command (Spigot-style “Youer” jar)

From the repository root:

```powershell
.\gradlew.bat setup youerJar
```

Gradle may accept a **short task name** (e.g. `youerJa` if it uniquely matches `youerJar`). Prefer the full name: **`youerJar`**.

First run can take **many minutes** (downloads + decompile + compile).

---

## 4. What was wrong before (and what was changed in the repo)

### Problem A — no Java for Gradle

Without `JAVA_HOME` / `java` on `PATH`, `gradlew.bat` fails immediately (Windows error **9009**).

**Fix:** configure JDK as in section 2.

### Problem B — “package `net.minecraft` … does not exist” (100+ errors)

Cause: **`org.gradle.parallel=true`** in `gradle.properties` allowed **`:neoforge:compileJava`** to run **in parallel** with **`setup`**, before **`setupCommon` / `setupClient`** finished copying decompiled Minecraft sources into:

- `projects/youer/src/main/java`
- `projects/youer/src/client/java`

The compiler then saw no `net.minecraft.*` sources and failed.

**Fix applied in the codebase** (already in the repo if you pulled latest):

- **File:** `buildSrc/src/main/java/net/neoforged/neodev/NeoDevPlugin.java`
- **Change:** After the `setup` task is registered, every **`JavaCompile`** task **`dependsOn`** **`setup`**, so sources are always synced before compilation.

Relevant snippet:

```java
var setup = tasks.register("setup", task -> {
    task.dependsOn(setupCommon, setupClient);
});

tasks.withType(JavaCompile.class).configureEach(task -> task.dependsOn(setup));
```

Plus an import for `org.gradle.api.tasks.compile.JavaCompile`.

If your clone **does not** contain this change, either pull the latest commit or add the same logic manually.

---

## 5. If the build still fails

1. Confirm **JDK 25** matches `gradle.properties` (`java_version=25`).
2. Run with a clean daemon once:  
   `.\gradlew.bat --stop`  
   then retry `.\gradlew.bat setup youerJar`.
3. If you suspect a bad cache, try `--no-build-cache` (slower).
4. Read the **last** error block in the console; search for `FAILED` / `What went wrong`.

---

## 6. Output location (typical)

The custom **`youerJar`** task writes under the **`:neoforge`** project (physical dir `projects/youer/`). Check:

`projects/youer/build/libs/`

for a jar named like **`youer-<minecraft_version>-<git abbreviated id>-spigot.jar`** (exact pattern is defined in `projects/youer/build.gradle`).

---

## 7. Complete changelog — every file touched in the repo

Only the items below were changed or added for the build fix and this guide. **No** edits were made to `gradle.properties`, `projects/youer/build.gradle`, wrapper scripts, etc.

### 7.1 `buildSrc/src/main/java/net/neoforged/neodev/NeoDevPlugin.java`

**Purpose:** Force all Java compilation to run **after** `setup`, so decompiled Minecraft sources exist under `projects/youer/src/...` before `compileJava` / `compileClientJava` (fixes parallel-build race with `org.gradle.parallel=true`).

**1) New import** (with the other `org.gradle.api.tasks.*` imports, e.g. after `Zip`):

```java
import org.gradle.api.tasks.compile.JavaCompile;
```

**2) `setup` task registration** — **before:**

```java
        tasks.register("setup", task -> {
            task.dependsOn(setupCommon, setupClient);
        });
```

**after** (capture the `TaskProvider` in a variable named `setup`):

```java
        var setup = tasks.register("setup", task -> {
            task.dependsOn(setupCommon, setupClient);
        });
```

**3) New block** (insert **immediately after** the `setup` registration shown above, **before** the `/*` `* RUNS SETUP` `*/` comment block):

```java
        // Decompiled Minecraft sources live under src/ after setup* Sync tasks. With parallel execution,
        // compileJava could otherwise start before those directories are populated and fail on missing
        // net.minecraft.* packages.
        tasks.withType(JavaCompile.class).configureEach(task -> task.dependsOn(setup));
```

**Summary:** one import, three lines changed around `setup`, plus four comment lines + one `configureEach` line.

---

### 7.2 `list.md` (this file)

**Purpose:** English hand-off for someone else building on Windows.

**Action:** **Created** at repository root as `list.md`. Later updated to add **section 7** so every repo file change is documented in one place.

**Not a code change** — documentation only.

---

### 7.3 `src/main/java/com/mohistmc/youer/mixins/minecraft/server/MixinMinecraftServer.java`

**Purpose:** EssentialsX `ReflServerStateProvider` reflects `boolean MinecraftServer#z()` (Spigot obfuscation for `isRunning()` on 1.21.7+). Added `public boolean z()` delegating to `@Shadow isRunning()`.

---

### 7.4 Removed: `src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json`

**Reason:** Wrong JSON shape for current `LootModifierManager` (expects per-modifier `type`); caused `No key type` parse errors.

---

### 7.5 `src/main/java/net/neoforged/neoforge/common/loot/IGlobalLootModifier.java`

**Change:** Javadoc updated (loot modifiers are one JSON per file with `type`).

---

### 7.6 `CHANGELOG.md`, `plugins.md`, section 8 in `list.md`

**Purpose:** Human-readable release notes (`CHANGELOG.md`); plugin compatibility matrix (`plugins.md`); reference plugin stack in **section 8** of this file.

---

### 7.7 `src/main/java/org/bukkit/plugin/java/PluginClassLoader.java`

**Purpose:** Suppress the benign Vault→Essentials economy bridge classloader warning (`isKnownOptionalPluginBridge`).

---

### 7.8 `projects/youer/build.gradle`

**Purpose:** `libraries 'commons-lang:commons-lang:2.6'` — supplies `org.apache.commons.lang.Validate` for ProtocolLib and other plugins still on Commons Lang 2.

---

### 7.9 `patches/net/minecraft/world/inventory/AnvilMenu.java.patch`

**Purpose:** `createResult()` is **not** `final` so Citizens (`CitizensAnvilMenu`) and similar NMS bridges can override it (`IncompatibleClassChangeError` fix).

---

### 7.10 `paper-thread-stub/` (Gradle subproject)

**Purpose:** JAR with **`io.papermc.paper.threadedregions.scheduler.ScheduledTask`** for WorldGuard etc. Declared as **`libraries(project(':paper-thread-stub'))`** in `projects/youer/build.gradle` so it is **not** merged into both minecraft and neoforge modules (avoids JPMS split-package export).

---

## Summary checklist for your friend

- [ ] Clone repo  
- [ ] Install **JDK 25**  
- [ ] Set **`JAVA_HOME`** and **`PATH`**  
- [ ] Repo includes the **`NeoDevPlugin.java`** `JavaCompile` → `dependsOn(setup)` fix (or apply it)  
- [ ] From repo root: **`.\gradlew.bat setup youerJar`**  
- [ ] Wait for first-time downloads/decompile  
- [ ] Pick up the jar from **`projects/youer/build/libs/`**

---

## 8. Reference plugin stack (all green, 2026-04-22)

One production-style **`plugins/`** folder was run successfully together (no `onEnable` failures for this set). File names as listed in that folder (extensions depend on how the host shows them):

| Plugin file |
|-------------|
| Citizens |
| EssentialsX-2.22.0-dev+104-87bde72 |
| EssentialsXChat-2.22.0-dev+104-87bde72 |
| EssentialsXSpawn-2.22.0-dev+104-87bde72 |
| LuckPerms-Bukkit-5.5.42 |
| multiverse-core-5.6.2-pre |
| packetevents-spigot-2.12.1 |
| PlaceholderAPI-2.12.2 |
| ProtocolLib |
| Vault |
| Vulcan-2.9.7.17 |
| worldedit-bukkit-7.4.3-beta-01 |
| worldguard-bukkit-7.0.16 |

Details and compatibility notes: [plugins.md](plugins.md) (German).
