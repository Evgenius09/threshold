# Slice 3: Organs, Slots, Skills, Energy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the pure core types `BodySlot`, `Organ`, `Skill`, `Body`, `SkillState` to `core/`, covered by JUnit tests.

**Architecture:** Three tasks, each with its own red/green CI cycle: the enums (`BodySlot`, `Organ`, `Skill`), then the `Body` record that consumes them, then the `SkillState` record. All types are immutable and import nothing from the game. The version goes to `0.1.3` in Task 1's implementation commit.

**Tech Stack:** Java 21, JUnit 5, Gradle in GitHub Actions only.

**Spec:** `tech.md` v2.1, sections 5 (core rules), 5.1 (`Element`, done in slice 1), 5.2–5.5, 5.7 (contracts), 9 (tests), 12 (commits), 15 "Слайс 3" (criteria).

## Global Constraints

- `./gradlew` is never run locally. Verification = `git push`, then `gh run watch` on the `build` run for HEAD. A run that fails in 2–5 s without starting the job is a GitHub-side block (`gh run view <id>`), not a red step.
- `core/` imports nothing from `net.minecraft.*`, `com.mojang.*`, `net.fabricmc.*` (`CorePurityTest` enforces it).
- Core records are immutable: constructors copy maps with `Map.copyOf`, methods return a new record.
- Indentation: tabs. Code, comments, commits in English. Comments explain why, not what. No Javadoc.
- Commits: `type(scope): summary`, imperative, lowercase, no period, ≤50 chars. **No `Co-Authored-By` or any attribution lines.**
- No new files beyond the five core files and three test classes listed below. No locale keys, no game code, no family/capacity/rarity fields.
- Version in `gradle.properties`: `version=0.1.3`.
- Tests assert literal numbers from tech.md (65, 400, 0.5, …), not the constants, and compare doubles with delta `1e-9`.
- Shared test convention: each test class has a one-line class comment saying the numbers come from tech.md 5.x, so a failure means the code drifted from the spec.

## Review Focus

1. **Regen drift blocks a skill one tick late.** 0.2 is not exact in binary: 100 ticks from energy 0 give `19.99999999999996`, so a literal `energy >= 20` refuses `SHADOW_BOLT` after the 5 s the player waited. A reasonable player expects the skill ready. Ruling in this plan: `canUse` compares with the spec's own tolerance, `energy + 1e-9 >= energyCost`, and `afterUse` clamps energy at 0. Test `regenFromZeroAffordsBoltAfterFiveSeconds` (Task 3).
2. **Shared constant mutated.** `SkillState.FULL` and `Body` with an empty map are shared values; an implementation that writes into the record's map corrupts every player. Test `afterUseLeavesFullUntouched` (Task 3) and `bodyIgnoresLaterChangesToSourceMap` (Task 2).
3. **`CORE` gives no skill for any organ**, not only the one criterion 4 checks. Covered by `ofCoreIsEmptyForEveryOrgan` (Task 1).
4. **Energy cap** holds when regen starts just below 100 (99.9 + 0.2 → exactly 100.0). Test `regenStopsAtMax` (Task 3).
5. **Several cooldowns tick independently**: a short cooldown expiring removes only its own key. Test `cooldownsTickIndependently` (Task 3).

---

### Task 1: `BodySlot`, `Organ`, `Skill`

**Files:**
- Modify: `gradle.properties` (`version=0.1.2` → `version=0.1.3`)
- Create: `src/main/java/io/github/evgenius09/threshold/core/BodySlot.java`
- Create: `src/main/java/io/github/evgenius09/threshold/core/Organ.java`
- Create: `src/main/java/io/github/evgenius09/threshold/core/Skill.java`
- Test: `src/test/java/io/github/evgenius09/threshold/SkillTest.java`

**Interfaces:**
- Consumes: `Element` (slice 1).
- Produces:
  ```java
  public enum BodySlot { ARMS, LEGS, BACK, CORE }
  public enum Organ {
      SHADOW_GLAND(Element.SHADOW), THUNDER_FANG(Element.STORM);
      public Element element();
  }
  public enum Skill {
      SHADOW_BOLT(Organ.SHADOW_GLAND, BodySlot.ARMS, 20, 60),
      SHADOW_STEP(Organ.SHADOW_GLAND, BodySlot.LEGS, 20, 120),
      SHADOW_VEIL(Organ.SHADOW_GLAND, BodySlot.BACK, 35, 400),
      STORM_DISCHARGE(Organ.THUNDER_FANG, BodySlot.ARMS, 25, 100),
      STORM_DASH(Organ.THUNDER_FANG, BodySlot.LEGS, 20, 240),
      STORM_HALO(Organ.THUNDER_FANG, BodySlot.BACK, 35, 300);
      public Organ organ(); public BodySlot slot();
      public double energyCost(); public int cooldownTicks();
      public static Optional<Skill> of(Organ organ, BodySlot slot);
  }
  ```

- [ ] **Step 1: Write the failing test `SkillTest`**

| Test | Assertions |
| --- | --- |
| `organElements` (crit. 1) | `SHADOW_GLAND.element()` = `SHADOW`; `THUNDER_FANG.element()` = `STORM` |
| `ofMatchesTable` (crit. 2) | for each of the 6 rows of the table in Interfaces: `Skill.of(organ, slot)` = `Optional.of(skill)`, and that skill's `energyCost()` and `cooldownTicks()` equal the row's literals. Write the table as a literal `Object[][]` in the test, not derived from `Skill.values()` |
| `ofCoreIsEmptyForEveryOrgan` (crit. 2) | `Skill.of(o, CORE)` is empty for both organs |
| `slotOrder` | `BodySlot.values()` = `{ARMS, LEGS, BACK, CORE}` (order is the menu and storage order, tech.md 5.2) |

- [ ] **Step 2: Push and confirm red**

```bash
git add src/test/java/io/github/evgenius09/threshold/SkillTest.java
git commit -m "test(core): cover organs and skill table"
git push
gh run watch "$(gh run list --commit "$(git rev-parse HEAD)" --workflow build --limit 1 --json databaseId --jq '.[0].databaseId')" --exit-status
```

Expected: FAILS at `compileTestJava` with `cannot find symbol` for `Organ`/`Skill`/`BodySlot`.

- [ ] **Step 3: Implement the three enums and bump the version**

Values exactly as in Interfaces. `Skill(Organ, BodySlot, double energyCost, int cooldownTicks)`. `Skill.of`: stream or loop over `values()` and return the one whose organ and slot match; `CORE` matches nothing because no row has it. Set `version=0.1.3`.

- [ ] **Step 4: Push and confirm green**

```bash
git add gradle.properties src/main/java/io/github/evgenius09/threshold/core/BodySlot.java src/main/java/io/github/evgenius09/threshold/core/Organ.java src/main/java/io/github/evgenius09/threshold/core/Skill.java
git commit -m "feat(core): add organs, body slots and skills"
git push
```
Then the same `gh run watch` line. Expected: PASS, `> Task :test` executed, `BUILD SUCCESSFUL`.

---

### Task 2: `Body`

**Files:**
- Create: `src/main/java/io/github/evgenius09/threshold/core/Body.java`
- Test: `src/test/java/io/github/evgenius09/threshold/BodyTest.java`

**Interfaces:**
- Consumes: `Element`, `BodySlot`, `Organ`, `Skill.of` (Task 1).
- Produces:
  ```java
  public record Body(Map<BodySlot, Organ> organs) {
      public Set<Element> elements();
      public Optional<Skill> skill(BodySlot slot);
      public double resistance(Element attack);
      public Element keepOrSteel(Element selected);
  }
  ```

- [ ] **Step 1: Write the failing test `BodyTest`**

Fixtures: `empty = new Body(Map.of())`; `glandArmsFangCore = new Body(Map.of(ARMS, SHADOW_GLAND, CORE, THUNDER_FANG))`; `fangOnly = new Body(Map.of(ARMS, THUNDER_FANG))`.

| Test | Assertions |
| --- | --- |
| `emptyBody` (crit. 3) | `elements()` = `Set.of(STEEL)`; `skill(ARMS)` empty; `resistance(SHADOW)` = 1.0 |
| `glandInArmsFangInCore` (crit. 4) | `elements()` = `Set.of(STEEL, SHADOW, STORM)`; `skill(ARMS)` = `Optional.of(SHADOW_BOLT)`; `skill(CORE)` empty; `resistance(STORM)` 0.5; `resistance(SHADOW)` 1.0; `resistance(STEEL)` 1.0 |
| `keepOrSteel` (crit. 5) | `fangOnly.keepOrSteel(SHADOW)` = `STEEL`; `glandArmsFangCore.keepOrSteel(SHADOW)` = `SHADOW` |
| `bodyIgnoresLaterChangesToSourceMap` | build a `HashMap` with `ARMS → SHADOW_GLAND`, create `Body`, then `put(CORE, THUNDER_FANG)` into the source map; `body.organs()` has no `CORE` key and `elements()` = `Set.of(STEEL, SHADOW)` |

- [ ] **Step 2: Push and confirm red**

```bash
git add src/test/java/io/github/evgenius09/threshold/BodyTest.java
git commit -m "test(core): cover body elements and resistance"
git push
```
Then `gh run watch` as above. Expected: FAILS at `compileTestJava`, `cannot find symbol ... Body`.

- [ ] **Step 3: Implement `core/Body.java`**

Compact constructor: `organs = Map.copyOf(organs)`.
- `elements()`: `EnumSet.of(STEEL)` plus each organ's element; return it wrapped unmodifiable (`Set.copyOf` is fine).
- `skill(slot)`: organ in slot → `Skill.of(organ, slot)`, else empty.
- `resistance(attack)`: `0.5` if the organ in `CORE` exists and its element equals `attack`, else `1.0`. One comment: why only `CORE` grants resistance (the core organ is the body's own element, plan).
- `keepOrSteel(selected)`: `elements().contains(selected) ? selected : STEEL`.

- [ ] **Step 4: Push and confirm green**

```bash
git add src/main/java/io/github/evgenius09/threshold/core/Body.java
git commit -m "feat(core): add body"
git push
```
Then `gh run watch`. Expected: PASS, `> Task :test` executed.

---

### Task 3: `SkillState`

**Files:**
- Create: `src/main/java/io/github/evgenius09/threshold/core/SkillState.java`
- Test: `src/test/java/io/github/evgenius09/threshold/SkillStateTest.java`

**Interfaces:**
- Consumes: `Skill` with `energyCost()`, `cooldownTicks()` (Task 1).
- Produces:
  ```java
  public record SkillState(double energy, Map<Skill, Integer> cooldowns) {
      public static final double MAX_ENERGY = 100.0;
      public static final double REGEN_PER_TICK = 0.2;
      public static final SkillState FULL = new SkillState(MAX_ENERGY, Map.of());
      public int cooldown(Skill skill);
      public boolean canUse(Skill skill);
      public SkillState afterUse(Skill skill);
      public SkillState tick();
  }
  ```

- [ ] **Step 1: Write the failing test `SkillStateTest`**

Helper `ticks(SkillState s, int n)` applies `tick()` n times.

| Test | Assertions |
| --- | --- |
| `veilCostsAndCoolsDown` (crit. 6) | `s = FULL.afterUse(SHADOW_VEIL)`: `energy` 65.0; `cooldown(SHADOW_VEIL)` 400; `canUse(SHADOW_VEIL)` false |
| `lowEnergyBlocksUse` (crit. 7) | `s = new SkillState(10.0, Map.of())`: `canUse(SHADOW_BOLT)` false; `assertSame(s, s.afterUse(SHADOW_BOLT))` |
| `regenFourPerSecond` (crit. 8) | `s = new SkillState(80.0, Map.of())`: after 20 ticks `energy` 84.0; `ticks(FULL, 20).energy()` 100.0 |
| `boltCooldownClears` (crit. 9) | `s = FULL.afterUse(SHADOW_BOLT)`, then 60 ticks: `cooldown(SHADOW_BOLT)` 0; `cooldowns().containsKey(SHADOW_BOLT)` false; `canUse(SHADOW_BOLT)` true. After 59 ticks: `cooldown` 1 |
| `regenFromZeroAffordsBoltAfterFiveSeconds` | `ticks(new SkillState(0.0, Map.of()), 100)`: `canUse(SHADOW_BOLT)` true; `afterUse(SHADOW_BOLT).energy()` ≥ 0.0 |
| `regenStopsAtMax` | `new SkillState(99.9, Map.of()).tick().energy()` = 100.0 |
| `afterUseLeavesFullUntouched` | after `FULL.afterUse(SHADOW_VEIL)`: `FULL.energy()` 100.0; `FULL.cooldowns()` empty |
| `cooldownsTickIndependently` | `s = FULL.afterUse(SHADOW_BOLT).afterUse(STORM_DISCHARGE)`, 60 ticks: no `SHADOW_BOLT` key; `cooldown(STORM_DISCHARGE)` 40 |

- [ ] **Step 2: Push and confirm red**

```bash
git add src/test/java/io/github/evgenius09/threshold/SkillStateTest.java
git commit -m "test(core): cover energy and cooldowns"
git push
```
Then `gh run watch`. Expected: FAILS at `compileTestJava`, `cannot find symbol ... SkillState`.

- [ ] **Step 3: Implement `core/SkillState.java`**

Compact constructor: `cooldowns = Map.copyOf(cooldowns)`.
- `cooldown(skill)`: `cooldowns.getOrDefault(skill, 0)`.
- `canUse(skill)`: `energy + 1e-9 >= skill.energyCost() && cooldown(skill) == 0`. One comment: the tolerance absorbs the drift of summing 0.2 per tick (Review Focus 1).
- `afterUse(skill)`: `!canUse` → `this`. Else copy cooldowns into a new `EnumMap`, put `skill → cooldownTicks()`, return `new SkillState(Math.max(0.0, energy - energyCost), map)`.
- `tick()`: energy `Math.min(MAX_ENERGY, energy + REGEN_PER_TICK)`; new `EnumMap` with each value minus 1, keeping only values > 0.

Note: `new EnumMap<>(Map.of())` throws on an empty non-EnumMap source; build with `new EnumMap<>(Skill.class)` then `putAll`.

- [ ] **Step 4: Push and confirm green**

```bash
git add src/main/java/io/github/evgenius09/threshold/core/SkillState.java
git commit -m "feat(core): add skill energy and cooldowns"
git push
```
Then `gh run watch`. Expected: PASS, `> Task :test` executed, `BUILD SUCCESSFUL`. Artifact `threshold-jar` has `threshold_gates-0.1.3.jar` with the five new core classes.

- [ ] **Step 5: Definition of Done check (tech.md 15)**

Verify: CI green; only the nine files of this plan changed since the plan commit (`git diff --stat <plan-commit>..HEAD`); no attribution lines in `git log -6 --format=%B`; `tech.md` untouched. No phone check in this slice (pure core).
