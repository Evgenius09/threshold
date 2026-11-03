# Slice 0 (v0.0 Foundation) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fabric 1.21.1 mod `threshold_gates` builds in GitHub Actions, deploys to the test phone with one script, and shows the test item `debug_shard` in game.

**Architecture:** Project from the `1.21.1` branch of `FabricMC/fabric-example-mod`, trimmed (no mixins, no publish). All builds and tests run in CI only. `scripts/deploy.sh` pulls the CI jar and drives the phone over `adb`.

**Tech Stack:** Minecraft 1.21.1, Fabric Loader 0.20.1, Fabric API 0.116.17+1.21.1, Loom 1.19.3 (`net.fabricmc.fabric-loom-remap`), Mojang mappings, Gradle 9.8.2, Java 21, JUnit 5.11.4, GitHub Actions, bash + `gh` + `adb`.

**Spec:** `tech.md` (v1), section 15 "Слайс 0". Game design: `docs/threshold-plan.md`.

## Global Constraints

- Never run `./gradlew` on this machine. "Run tests" = `git push` then `gh run watch --exit-status`.
- Mod ID `threshold_gates`; package `io.github.evgenius09.threshold`; literal `"threshold_gates"` only in `Threshold.MOD_ID`.
- Versions exactly as `tech.md` section 2; `group=io.github.evgenius09`, `version=0.0.1`, `rootProject.name = 'threshold_gates'`, `base.archivesName = "threshold_gates"`.
- CI artifact name `threshold-jar`, path `build/libs/threshold_gates-*.jar` excluding `*-sources.jar`, `retention-days: 7`.
- `core/` imports nothing from `net.minecraft.*`, `com.mojang.*`, `net.fabricmc.*`.
- Player text only in `assets/threshold_gates/lang/ru_ru.json`.
- Commits: `type(scope): summary`, English, no `Co-Authored-By` / `Signed-off-by` / "Generated with" lines. Author comes from local git config (already set). Commit straight to `main`.
- `adb` always with `-s "$THRESHOLD_ADB"` (default `100.98.166.61:5555`). Never touch the SaluteTV device, worlds, accounts or launcher settings.

## Review Focus

- `deploy.sh` run right after push, before the CI run exists → must wait for the run to appear (poll up to 60 s), not fail with "no runs".
- `deploy.sh` run with local commits not pushed → must stop with a clear message (`HEAD not on origin`), not deploy an older jar.
- Phone offline / adb not connected → must try `adb connect` once, then exit 1 with a readable message.
- Game crashes during load (no mod list line ever appears) → must report the crash and the log tail after the 180 s timeout, exit 1, not print OK.
- Old `threshold_gates-*.jar` already in `mods/` (including a different version number) → removed before push, so exactly one Threshold jar remains.

Each of these is covered by a verification step in Task 4.

---

### Task 1: Project skeleton, CI, GitHub repo

**Files:**
- Create from template branch `1.21.1`: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `.gitignore`, `.gitattributes`
- Create: `settings.gradle`, `gradle.properties`, `build.gradle`, `.github/workflows/build.yml`
- Create: `src/main/java/io/github/evgenius09/threshold/Threshold.java`
- Create: `src/main/java/io/github/evgenius09/threshold/core/package-info.java`
- Create: `src/client/java/io/github/evgenius09/threshold/client/ThresholdClient.java`
- Create: `src/main/resources/fabric.mod.json`, `src/main/resources/assets/threshold_gates/icon.png`

**Interfaces:**
- Produces: `Threshold.MOD_ID: String`, `Threshold.LOGGER: org.slf4j.Logger`, `static ResourceLocation Threshold.id(String path)`; CI artifact `threshold-jar`.

- [ ] **Step 1: Fetch template files** with `gh api repos/FabricMC/fabric-example-mod/contents/<path>?ref=1.21.1` (base64-decode; `gradle-wrapper.jar` binary). Keep `gradlew` executable (`git update-index --chmod=+x gradlew`).
- [ ] **Step 2: Write `settings.gradle`, `gradle.properties`** — template text with `rootProject.name = 'threshold_gates'`, `loom_version=1.19.3`, `group=io.github.evgenius09`, `version=0.0.1`, other versions per Global Constraints.
- [ ] **Step 3: Write `build.gradle`** — template minus `maven-publish`, `publishing {}`, `jar { from("LICENSE") }`; `loom.mods` key `"threshold_gates"`; add `base { archivesName = "threshold_gates" }`, `testImplementation "org.junit.jupiter:junit-jupiter:5.11.4"`, `testRuntimeOnly "org.junit.platform:junit-platform-launcher"`, `test { useJUnitPlatform() }`. Keep `withSourcesJar()`.
- [ ] **Step 4: Write `Threshold.java`** (`implements ModInitializer`, members per Interfaces, `id()` = `ResourceLocation.fromNamespaceAndPath(MOD_ID, path)`, `onInitialize` logs `"Threshold loaded"`), `ThresholdClient.java` (`implements ClientModInitializer`, empty body), `core/package-info.java` (javadoc: pure game rules, no Minecraft imports, see tech.md §5).
- [ ] **Step 5: Write `fabric.mod.json`** — exactly `tech.md` section 7. Generate `icon.png` 128×128 (dark background, grey crack) with a throwaway Python script in the scratchpad using only `zlib`+`struct`; the script is not committed.
- [ ] **Step 6: Write `.github/workflows/build.yml`** — per `tech.md` section 10: triggers `push`, `pull_request`; checkout@v6 → wrapper-validation@v6 → setup-java@v5 (`java-version: '25'`, `distribution: 'microsoft'`) → `gradle/actions/setup-gradle@v6` → `chmod +x ./gradlew` → `./gradlew build` → upload-artifact@v7 with name/path/retention per Global Constraints (`path: |` with `build/libs/threshold_gates-*.jar` and `!build/libs/*-sources.jar`, `if-no-files-found: error`).
- [ ] **Step 7: Commit** `chore(ci): add fabric project skeleton and build workflow`.
- [ ] **Step 8: Create repo and push** — `gh repo create Evgenius09/threshold --private --source . --remote origin --push`. No collaborators.
- [ ] **Step 9: Verify CI** — `gh run watch --exit-status $(gh run list -L1 --json databaseId -q '.[0].databaseId')`. Expected: success. Then `gh run download <id> -n threshold-jar -D build/ci-check && ls build/ci-check`. Expected: exactly `threshold_gates-0.0.1.jar` (criterion 1). If red: read `gh run view --log-failed`, fix, commit `fix(ci): …`, repeat.

---

### Task 2: CorePurityTest

**Files:**
- Test: `src/test/java/io/github/evgenius09/threshold/CorePurityTest.java`

**Interfaces:**
- Consumes: `core/` package from Task 1.

- [ ] **Step 1: Write the test** — `coreHasNoGameImports()`: walk `src/main/java/io/github/evgenius09/threshold/core` (path relative to project dir, Gradle runs tests there) for `*.java`; collect every line matching `^\s*import\s+(static\s+)?(net\.minecraft|com\.mojang|net\.fabricmc)\.`; `assertTrue(violations.isEmpty(), "Game imports in core/: " + violations)` where each violation is `file:line`. Also `coreDirExists()`: `assertTrue(Files.isDirectory(coreDir))` so a moved package cannot make the test pass vacuously.
- [ ] **Step 2: Commit and push, verify green** — commit `test(core): guard core package against game imports`; `gh run watch --exit-status`. Expected: success.
- [ ] **Step 3: Prove it fails (criterion 2)** — create `core/PurityProbe.java` with `import net.minecraft.world.item.Item;` and an empty class; commit `test(core): probe purity guard`, push, watch. Expected: FAIL, `--log-failed` shows `Game imports in core/: …PurityProbe.java:…`.
- [ ] **Step 4: Revert** — `git revert --no-edit HEAD` (message edited to `test(core): revert purity probe`), push, watch. Expected: success.

---

### Task 3: Test item `debug_shard` and LangTest

**Files:**
- Create: `src/main/java/io/github/evgenius09/threshold/registry/ModItems.java`
- Modify: `src/main/java/io/github/evgenius09/threshold/Threshold.java` (call `ModItems.init()` in `onInitialize`)
- Create: `src/main/resources/assets/threshold_gates/lang/ru_ru.json`
- Create: `src/main/resources/assets/threshold_gates/models/item/debug_shard.json`
- Create: `src/main/resources/assets/threshold_gates/textures/item/debug_shard.png`
- Test: `src/test/java/io/github/evgenius09/threshold/LangTest.java`

**Interfaces:**
- Consumes: `Threshold.id(String)`.
- Produces: `ModItems.DEBUG_SHARD: Item`, `static void ModItems.init()`.

- [ ] **Step 1: Write LangTest** — `everyItemHasRussianName()`: `List<String> ids = List.of("debug_shard")` (explicit list, tech.md §9); read `src/main/resources/assets/threshold_gates/lang/ru_ru.json` with Gson (`JsonParser.parseString`); for each id assert key `item.threshold_gates.<id>` exists and its value is non-blank. Message names the missing key.
- [ ] **Step 2: Push test alone, verify it fails** — commit `test(lang): require russian name for each item`, push, watch. Expected: FAIL (file missing → `NoSuchFileException`). This also proves criterion 3's failure path.
- [ ] **Step 3: Implement item and assets** — `ModItems.DEBUG_SHARD = Registry.register(BuiltInRegistries.ITEM, Threshold.id("debug_shard"), new Item(new Item.Properties()))`; `init()` adds it via `ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(e -> e.accept(DEBUG_SHARD))`. `ru_ru.json`: `{"item.threshold_gates.debug_shard": "Осколок порога (тест)"}`. Model: `{"parent": "minecraft:item/generated", "textures": {"layer0": "threshold_gates:item/debug_shard"}}`. Texture 16×16 RGBA PNG, transparent background, grey shard with a light crack, generated by a throwaway scratchpad script (not committed).
- [ ] **Step 4: Push, verify green** — commit `feat(registry): add debug shard test item`, push, watch. Expected: success.

---

### Task 4: `scripts/deploy.sh` and in-game acceptance

**Files:**
- Create: `scripts/deploy.sh` (executable)
- Modify: `.gitignore` (add `build/` if the template lacks it)

**Interfaces:**
- Consumes: CI artifact `threshold-jar`; env `THRESHOLD_ADB`, `THRESHOLD_PKG`, `THRESHOLD_INSTANCE` with defaults from `tech.md` §11.
- Produces: `build/deploy/latest.log`; exit 0 + line `OK: threshold_gates <version> loaded` or exit 1 + reason.

- [ ] **Step 1: Write `deploy.sh`** — `#!/usr/bin/env bash`, `set -euo pipefail`, steps 1–6 of `tech.md` §11, plus the Review Focus guards:
  - `git fetch -q origin` and `git branch -r --contains HEAD | grep -q origin/` else exit 1 `HEAD not on origin, push first`;
  - poll `gh run list --commit "$(git rev-parse HEAD)" --workflow build -L1` every 5 s up to 60 s for a run id;
  - `adb -s "$THRESHOLD_ADB" get-state` fails → one `adb connect "$THRESHOLD_ADB"` → still fails: exit 1 `phone not reachable`;
  - `adb shell rm -f "$THRESHOLD_INSTANCE/mods/threshold_gates-"*.jar` (quote the path, glob runs on the phone) before `adb push`;
  - tap «Запустить»: `uiautomator dump` → pull → find node with `text="Запустить"` → tap bounds centre; retry dump up to 10× at 2 s while the launcher starts;
  - wait for `Loading [0-9]+ mods` in `latest.log` whose mtime is newer than launch time; then grep the mod list block for `threshold_gates 0.0.1` (version from `gradle.properties`); fail on `Crash report` or an `ERROR`/exception line containing `threshold`; after the mod line, keep watching 30 s more for a crash;
  - timeout 180 s → exit 1, print last 30 log lines.
- [ ] **Step 2: Commit and push** — `feat(deploy): add phone deploy script`; wait CI green.
- [ ] **Step 3: Verify happy path (criteria 4, 5)** — `scripts/deploy.sh`. Expected: `OK: threshold_gates 0.0.1 loaded`. Run again; then `adb -s 100.98.166.61:5555 shell ls "$THRESHOLD_INSTANCE/mods"`. Expected: exactly one `threshold_gates-*.jar`.
- [ ] **Step 4: Verify guards** — (a) make a local empty commit `chore: deploy guard probe` without pushing, run script → expect `HEAD not on origin`, then `git reset --hard HEAD~1`; (b) `THRESHOLD_ADB=100.98.166.61:5599 scripts/deploy.sh` → expect `phone not reachable` (after a green CI wait).
- [ ] **Step 5: Create test world (owner, once)** — ask the owner to create world «Threshold Dev»: creative mode, commands on. In it run `/give @s threshold_gates:debug_shard` (type via the in-game `T` button or ask the owner).
- [ ] **Step 6: Verify in game (criteria 6, 7)** — `adb -s "$THRESHOLD_ADB" exec-out screencap -p > build/deploy/screen.png`, look at it: item named «Осколок порога (тест)», shard texture, not purple-black. Pull `latest.log` → no `ERROR`/exception with `threshold`.
- [ ] **Step 7: Close the slice** — tick the DoD in `tech.md` §15 mentally (do not edit `tech.md`); report to the owner in plain Russian.
