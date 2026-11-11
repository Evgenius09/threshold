# Slice 4: Organ Items Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Register the two organ items (`shadow_gland`, `thunder_fang`) with element tooltip, lore, models, textures and Russian names, and remove `debug_shard`.

**Architecture:** One game-layer class `item/OrganItem` wraps a core `Organ` and draws the tooltip. `ModItems` swaps `DEBUG_SHARD` for the two organ fields and puts them in the vanilla Ingredients tab. `LangTest` switches from "these item names exist" to "ru_ru.json holds exactly this key list", so a missing or stale key fails CI. Phone checks are done by the owner after `scripts/deploy.sh`.

**Tech Stack:** Java 21, Minecraft 1.21.1 (Mojang mappings), Fabric API 0.116.17, JUnit 5, Gradle in GitHub Actions only.

**Spec:** `tech.md` v2.1, sections 3 (layout), 4 (IDs, `Threshold.id`, `MOD_ID` rule), 6 + 6.1 (items), 8 (locale keys and texts), 9 (tests), 11 (phone), 12 (commits), 15 "Слайс 4" (criteria), 16 (review).

## Global Constraints

- `./gradlew` is never run locally. Verification = `git push`, then `gh run watch` on the `build` run for HEAD. A run that fails in 2–5 s without starting the job is a GitHub-side block (`gh run view <id>`), not a red step.
- The literal `"threshold_gates"` appears in Java only in `Threshold.MOD_ID`. Build keys and IDs from `Threshold.MOD_ID` / `Threshold.id(...)`.
- Player-facing text lives only in `ru_ru.json`; no Russian strings in Java.
- `adb` only with `-s "$THRESHOLD_ADB"` (default `100.98.166.61:5555`). Do not touch the SaluteTV, worlds, accounts or launcher settings. Phone checks of tooltip, tab and stack size are the owner's; the executor runs `scripts/deploy.sh`, reads `build/deploy/latest.log` and may take a screenshot.
- Indentation: tabs in Java, 2 spaces in JSON (matches existing files). Code, comments, commits in English. Comments explain why. No Javadoc.
- Commits: `type(scope): summary`, imperative, lowercase, no period, ≤50 chars. **No `Co-Authored-By` or any attribution lines.**
- No new files beyond those listed in Task 1. No anatomy screen, player state or spawn eggs.
- Version in `gradle.properties`: `version=0.1.4`.

## Review Focus

1. **Stale `debug_shard` leftovers** (model, texture, key, field, test list) make the jar ship a dead asset or a raw-key item. After the change `git grep -n debug_shard -- src` prints nothing. Pinned by `ruRuHasExactlyTheExpectedKeys` (extra key fails) and the grep in Task 1 Step 5.
2. **Hardcoded mod ID in the element key**: building `"element.threshold_gates." + …` breaks the `MOD_ID` rule (tech.md 4, checklist item 3). The key is built from `Threshold.MOD_ID`. Checked by `git grep -n '"threshold_gates' -- src/main/java` in Step 5: only `Threshold.java` matches.
3. **Whole tooltip line in element color**: criterion 2 wants «Стихия: Тень» purple, not only the word «Тень». Color the outer translatable component. Owner checks on the phone (criteria 2, 3).
4. **Wrong-case element key**: `Element.STEEL.name()` is upper case; keys are lower case (`element.threshold_gates.steel`). Use `name().toLowerCase(Locale.ROOT)` so a Turkish-locale JVM does not produce a dotless i. Pinned by the exact key list in `LangTest`.
5. **Missing model or texture shows the purple-black checker** in game. Model files point at `threshold_gates:item/<id>`, PNGs are 16×16 RGBA. Checked with `file` in Step 4 and by the owner on the phone.

---

### Task 1: Organ items replace `debug_shard`

**Files:**
- Modify: `gradle.properties` (`version=0.1.3` → `version=0.1.4`)
- Modify: `src/test/java/io/github/evgenius09/threshold/LangTest.java`
- Modify: `src/main/resources/assets/threshold_gates/lang/ru_ru.json`
- Modify: `src/main/java/io/github/evgenius09/threshold/registry/ModItems.java`
- Create: `src/main/java/io/github/evgenius09/threshold/item/OrganItem.java`
- Create: `src/main/resources/assets/threshold_gates/models/item/shadow_gland.json`, `.../thunder_fang.json`
- Create: `src/main/resources/assets/threshold_gates/textures/item/shadow_gland.png`, `.../thunder_fang.png`
- Delete: `models/item/debug_shard.json`, `textures/item/debug_shard.png`

**Interfaces:**
- Consumes: `Organ` with `element()`, `Element` with `color()` (slices 1, 3); `Threshold.MOD_ID`, `Threshold.id(String)`.
- Produces (later slices rely on these exact names):
  ```java
  package io.github.evgenius09.threshold.item;
  public class OrganItem extends Item {
      public OrganItem(Organ organ);   // properties: new Item.Properties().stacksTo(16)
      public Organ organ();
  }
  // registry/ModItems
  public static final Item SHADOW_GLAND;   // new OrganItem(Organ.SHADOW_GLAND), id "shadow_gland"
  public static final Item THUNDER_FANG;   // new OrganItem(Organ.THUNDER_FANG), id "thunder_fang"
  ```

- [ ] **Step 1: Rewrite `LangTest` to an exact key list (failing test)**

Replace `ITEM_IDS` with an explicit `Set<String> KEYS` (comment stays: explicit so the test runs without Minecraft registries):

```
item.threshold_gates.shadow_gland
item.threshold_gates.shadow_gland.lore
item.threshold_gates.thunder_fang
item.threshold_gates.thunder_fang.lore
item.threshold_gates.organ.element
element.threshold_gates.steel
element.threshold_gates.storm
element.threshold_gates.shadow
```

Tests:
- `ruRuHasExactlyTheExpectedKeys`: `assertEquals(KEYS, lang.keySet())`. One-line comment: exact match also catches stale keys, which tech.md 16 item 7 forbids.
- `noBlankValues`: every value in `lang` is non-blank; use `assertFalse(value.isBlank(), "Blank value in ru_ru.json: " + key)`.

Class comment stays: every registered item needs a Russian name (tech.md 8). Later slices add their keys to `KEYS`.

- [ ] **Step 2: Push and confirm red**

```bash
git add src/test/java/io/github/evgenius09/threshold/LangTest.java
git commit -m "test(lang): require organ and element keys"
git push
gh run watch "$(gh run list --commit "$(git rev-parse HEAD)" --workflow build --limit 1 --json databaseId --jq '.[0].databaseId')" --exit-status
```

Expected: FAILS at `:test`, `LangTest > ruRuHasExactlyTheExpectedKeys() FAILED` (expected set has organ keys, actual has `debug_shard`). This is criterion 1's "fails without their keys".

- [ ] **Step 3: Locale, version, removal of `debug_shard`**

- `ru_ru.json`: exactly the 8 keys of Step 1 with the texts from tech.md 8 (Теневая железа / Сочится тьмой даже на свету. / Громовой клык / Гудит перед грозой. / Стихия: %s / Сталь / Гроза / Тень). `debug_shard` key removed.
- `git rm` the `debug_shard` model and texture.
- `version=0.1.4`.

- [ ] **Step 4: Models and textures**

- Models: same shape as the old `debug_shard.json` (`parent: minecraft:item/generated`, `layer0: threshold_gates:item/<id>`).
- Textures 16×16 RGBA, style «Трещины света» (tech.md 6.1): `shadow_gland` is a dark blob (near-black purple `#1E1424`, edge `#2E1F38`) with a bright purple crack (`#8A3FD1`, core `#C89BFF`); `thunder_fang` is a light curved fang (bone `#E8E4D8`, shade `#B8B2A0`) with a yellow crack (`#FFE04A`, core `#FFF6B0`). Transparent background.
- No PIL on this machine: draw them with a throwaway stdlib Python script (`zlib` + `struct` PNG writer, pixel map as 16 strings) kept in the session scratchpad, not in the repo.
- Check: `file src/main/resources/assets/threshold_gates/textures/item/*.png` prints `16 x 16, 8-bit/color RGBA` for both.

- [ ] **Step 5: `OrganItem` and `ModItems`**

`OrganItem`:
- Constructor `OrganItem(Organ organ)` calls `super(new Item.Properties().stacksTo(16))`.
- Override `appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag)`:
  1. `Component.translatable("item." + Threshold.MOD_ID + ".organ.element", Component.translatable("element." + Threshold.MOD_ID + "." + element.name().toLowerCase(Locale.ROOT)))` with style color `organ.element().color()` on the outer component, so the whole line is colored.
  2. `Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)`.
- One comment on the color: the whole line carries the element color (criterion 2).

`ModItems`:
- Remove `DEBUG_SHARD` and its comment.
- `SHADOW_GLAND`, `THUNDER_FANG` registered with `Registry.register(BuiltInRegistries.ITEM, Threshold.id("<id>"), new OrganItem(Organ.<X>))`; a private static helper `register(String id, Item item)` is fine if it removes duplication.
- `init()`: both items into `CreativeModeTabs.INGREDIENTS` via the existing `ItemGroupEvents.modifyEntriesEvent` call.

Then:

```bash
git grep -n debug_shard -- src                       # Expected: no output
git grep -n '"threshold_gates' -- src/main/java      # Expected: only Threshold.java (MOD_ID)
```

- [ ] **Step 6: Push and confirm green**

```bash
git add -A src gradle.properties
git commit -m "feat(item): add organ items, drop debug shard"
git push
gh run watch "$(gh run list --commit "$(git rev-parse HEAD)" --workflow build --limit 1 --json databaseId --jq '.[0].databaseId')" --exit-status
```

Expected: PASS, `> Task :test` executed, `BUILD SUCCESSFUL`. Artifact `threshold-jar` holds `threshold_gates-0.1.4.jar` containing `item/OrganItem.class`, both models and both PNGs, and no `debug_shard` entry (`unzip -l … | grep -E 'organ|gland|fang|debug'`).

If compilation fails on a Mojang-mapping name (`appendHoverText` signature, `Item.TooltipContext`, `Style.withColor(int)`), fix it against the 1.21.1 mapping; that is not a contract change.

- [ ] **Step 7: Deploy and log check**

```bash
scripts/deploy.sh
grep -iE 'error|exception' build/deploy/latest.log | grep -i threshold   # Expected: no output
```

Expected: `deploy.sh` prints OK with `threshold_gates 0.1.4`. Take one screenshot to `build/deploy/screen.png` to confirm the game reached the menu or world.

- [ ] **Step 8: Hand phone criteria to the owner**

Ask the owner to check in «Threshold Dev» and report back:
1. `/give @s threshold_gates:shadow_gland`: name «Теневая железа», own texture, tooltip «Стихия: Тень» purple, gray italic lore below.
2. `/give @s threshold_gates:thunder_fang`: «Громовой клык», «Стихия: Гроза» yellow, lore.
3. Creative «Ингредиенты» tab has both; a stack holds 16.
4. `/give @s threshold_gates:debug_shard` says the item does not exist.

Slice closes when the owner confirms all four (DoD 1).

- [ ] **Step 9: Definition of Done check (tech.md 15)**

Verify: CI green; only the files listed in this task changed since the plan commit (`git diff --stat <plan-commit>..HEAD`); no attribution lines in `git log -3 --format=%B`; `tech.md` untouched; owner confirmed phone criteria 2–5.
