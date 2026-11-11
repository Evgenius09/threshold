# Slice 5: Player State and Anatomy Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Store the player's body in the `threshold_gates:body` attachment and let the player graft organs through the «Анатомия» screen opened with `K`.

**Architecture:** `BodyData` is the persistent, synced player record; its codecs live inside it and treat the save file as untrusted input. `AnatomyMenu` shows 4 body cells over the player inventory; on the server the cells read and write the attachment directly through a private `Container`, so a placed organ is grafted at once. The client sends only `OpenAnatomyPayload`; the server opens the menu. Behavior is checked by the owner on the phone; CI gates compilation and `LangTest`.

**Tech Stack:** Java 21, Minecraft 1.21.1 (Mojang mappings), Fabric API 0.116.17 (`fabric-data-attachment-api-v1` 1.4.7: `AttachmentRegistry.create`, `syncWith`, `AttachmentSyncPredicate.targetOnly()`, `copyOnDeath()` verified present), JUnit 5, Gradle in GitHub Actions only.

**Spec:** `tech.md` v2.1, sections 3, 4, 6.1, 6.2, 6.3 (K key and `OpenAnatomyPayload` only), 6.4 (menu and screen, no HUD), 6.8, 8, 11, 12, 15 "Слайс 5", 16.

## Global Constraints

- `./gradlew` is never run locally. Verification = `git push`, then `gh run watch` on the `build` run for HEAD. A run that fails in 2–5 s without starting the job is a GitHub-side block (`gh run view <id>`), not a red step.
- The literal `"threshold_gates"` appears in Java only in `Threshold.MOD_ID`. Every key and ID is built from `Threshold.MOD_ID` / `Threshold.id(...)`.
- Client code (screen, keys) only in `src/client/`. `src/main` never imports from `client`.
- Player-facing text only in `ru_ru.json`.
- No own S2C payloads; the attachment syncs itself (`syncWith`).
- `adb` only with `-s "$THRESHOLD_ADB"`. Do not touch the SaluteTV, worlds, accounts or launcher settings. Phone criteria are checked by the owner; the executor runs `scripts/deploy.sh`, reads the log, may take a screenshot.
- Tabs in Java, 2 spaces in JSON. English code, comments, commits; comments explain why. No Javadoc.
- Commits: `type(scope): summary`, imperative, lowercase, no period, ≤50 chars. **No `Co-Authored-By` or any attribution lines.**
- Files: only those listed in the tasks. No element switching, HUD, skills, `BodyTicker`, `BodyActions`, swap penalties.
- Version in `gradle.properties`: `version=0.1.5`.

## Rulings for the owner (decisions tech.md leaves open)

1. The death reset (`ServerPlayerEvents.AFTER_RESPAWN`) is registered in `ModAttachments.init()`: tech.md names no file for it in slice 5, and it is part of the attachment's lifecycle.
2. `AnatomyMenu` has two constructors: `(int, Inventory)` for the client, required by `new MenuType<>(AnatomyMenu::new, …)`, and `(int, Inventory, ServerPlayer)` for the server, backed by the attachment.
3. The codecs are lenient: an unknown element reads as `STEEL`, unknown skill/element map keys are dropped, numbers are clamped to their core ranges, and the organ list is padded or cut to 4. A hand-edited or future save must not wipe the player's organs (the whole record falling back to `DEFAULT` would).
4. Screen layout (texture and slot coordinates) is fixed in Task 3, since tech.md describes it only in words.

## Review Focus

1. **Vanilla mutates the stack it gets from `Container.getItem`** (shift-click shrinks it to 0 before calling `setItem(EMPTY)`). If the container skips writes when "nothing changed", the attachment keeps a dead stack and the client never resyncs. `setItem`, `removeItem*` and `setChanged` always store a fresh `BodyData`. Owner checks shift-click both ways (Task 3 Step 6, item 7).
2. **Shift-click of a stack of 16 organs** must put exactly 1 into the body and leave 15. `OrganSlot.getMaxStackSize()` returns 1. Owner check, item 2.
3. **Edited or corrupt save** (`/data modify` on the attachment, renamed enum later) must not drop organs. Lenient codecs per Ruling 3; `body()` skips stacks that are not `OrganItem`.
4. **Respawn without death** (leaving the End, `alive == true`) must not reset load and energy. The reset runs only when `!alive`.
5. **Swapping organ for organ** in a cell (cursor holds the fang, cell holds the gland) grafts the fang with a `graft` message and returns the gland to the cursor; nothing is duplicated or lost. Owner check, item 2.

---

### Task 1: Locale keys for the slice

**Files:**
- Modify: `gradle.properties` (`version=0.1.4` → `version=0.1.5`)
- Modify: `src/test/java/io/github/evgenius09/threshold/LangTest.java`
- Modify: `src/main/resources/assets/threshold_gates/lang/ru_ru.json`

**Interfaces:**
- Produces keys used by Tasks 2–3: `slot.threshold_gates.{arms,legs,back,core}`, `container.threshold_gates.anatomy`, `key.categories.threshold_gates`, `key.threshold_gates.anatomy`, `message.threshold_gates.system.graft`.

- [ ] **Step 1: Add the 8 keys to `LangTest.KEYS` (failing test)**

Add exactly: `slot.threshold_gates.arms`, `slot.threshold_gates.legs`, `slot.threshold_gates.back`, `slot.threshold_gates.core`, `container.threshold_gates.anatomy`, `key.categories.threshold_gates`, `key.threshold_gates.anatomy`, `message.threshold_gates.system.graft`.

- [ ] **Step 2: Push and confirm red**

```bash
git add src/test/java/io/github/evgenius09/threshold/LangTest.java
git commit -m "test(lang): require anatomy keys"
git push
gh run watch "$(gh run list --commit "$(git rev-parse HEAD)" --workflow build --limit 1 --json databaseId --jq '.[0].databaseId')" --exit-status
```

Expected: FAILS at `:test`, `LangTest > ruRuHasExactlyTheExpectedKeys() FAILED`.

- [ ] **Step 3: Add the texts to `ru_ru.json` and bump the version**

Texts from tech.md 8: Руки / Ноги / Спина / Ядро / Анатомия / Threshold / Анатомия / `[Система] Орган прижился: %s, слот «%s».` Set `version=0.1.5`.

- [ ] **Step 4: Push and confirm green**

```bash
git add gradle.properties src/main/resources/assets/threshold_gates/lang/ru_ru.json
git commit -m "feat(lang): add anatomy screen texts"
git push
```
Then the same `gh run watch` line. Expected: PASS, `> Task :test` executed.

---

### Task 2: `BodyData`, attachment, death reset, system messages

**Files:**
- Create: `src/main/java/io/github/evgenius09/threshold/body/BodyData.java`
- Create: `src/main/java/io/github/evgenius09/threshold/registry/ModAttachments.java`
- Create: `src/main/java/io/github/evgenius09/threshold/system/SystemMessages.java`
- Modify: `src/main/java/io/github/evgenius09/threshold/Threshold.java` (call `ModAttachments.init()`)

**Interfaces:**
- Consumes: core `Element`, `LoadState`, `SkillState`, `Skill`, `Body`, `BodySlot`; `OrganItem.organ()`.
- Produces (tech.md 6.2, 6.8):
  ```java
  public record BodyData(List<ItemStack> organs, Element selected, LoadState load, SkillState skills,
          int haloTicks, boolean seenOrgan, Map<Element, Double> damageDealt) {
      public static final BodyData DEFAULT;
      public static final Codec<BodyData> CODEC;
      public static final StreamCodec<RegistryFriendlyByteBuf, BodyData> STREAM_CODEC;
      public Body body();
  }
  public final class ModAttachments { public static final AttachmentType<BodyData> BODY; public static void init(); }
  public final class SystemMessages { public static void send(ServerPlayer player, String id, Object... args); }
  ```

- [ ] **Step 1: `BodyData`**

- Compact constructor: `organs` becomes an unmodifiable list of exactly 4 (pad with `ItemStack.EMPTY`, cut extras); `damageDealt = Map.copyOf(damageDealt)`.
- `DEFAULT`: 4× `ItemStack.EMPTY`, `STEEL`, `LoadState.EMPTY`, `SkillState.FULL`, 0, false, `Map.of()`.
- `body()`: for each index whose stack's item is an `OrganItem`, put `BodySlot.values()[i] → organ()`; other stacks are ignored.
- `CODEC` via `RecordCodecBuilder`, field names `organs`, `selected`, `load`, `skills`, `halo_ticks`, `seen_organ`, `damage_dealt`. Private codecs inside the file (Ruling 3):
  - organs: `ItemStack.OPTIONAL_CODEC.listOf()`;
  - element: `Codec.STRING`, lower-case name (`Locale.ROOT`), unknown → `STEEL`;
  - a private generic enum-keyed map codec over `Codec.unboundedMap(Codec.STRING, valueCodec)` that drops unknown keys; used for `Map<Skill, Integer>` and `Map<Element, Double>`;
  - `LoadState`: `load` clamped to `[0, MAX]`, `rejection_ticks` clamped to `[0, REJECTION_TICKS]`;
  - `SkillState`: `energy` clamped to `[0, MAX_ENERGY]`, `cooldowns` keeping only values > 0.
- `STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC)`: one codec to keep in step, and `StreamCodec.composite` stops at 6 fields while `BodyData` has 7.

- [ ] **Step 2: `ModAttachments`**

- `BODY` exactly as tech.md 6.2: `AttachmentRegistry.create(Threshold.id("body"), b -> b.initializer(() -> BodyData.DEFAULT).persistent(BodyData.CODEC).copyOnDeath().syncWith(BodyData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()))`.
- `init()` registers `ServerPlayerEvents.AFTER_RESPAWN`: when `!alive`, read `newPlayer.getAttachedOrCreate(BODY)` and set a copy with `load = LoadState.EMPTY`, `skills = SkillState.FULL`, `haloTicks = 0`, everything else kept. One comment: `alive` is true for leaving the End, which is not a death.

- [ ] **Step 3: `SystemMessages.send`**

`player.sendSystemMessage(Component.translatable("message." + Threshold.MOD_ID + ".system." + id, args).withStyle(s -> s.withColor(0xA8B4C0)))`. Private constructor.

- [ ] **Step 4: Wire init and push for green**

`Threshold.onInitialize`: `ModItems.init(); ModAttachments.init();` then the log line.

```bash
git add src/main/java/io/github/evgenius09/threshold
git commit -m "feat(body): add player body attachment"
git push
```
Then `gh run watch`. Expected: PASS (compilation is this task's gate; no JUnit can load Minecraft registries, tech.md 9).

---

### Task 3: Anatomy menu, payload, key, screen

**Files:**
- Create: `src/main/java/io/github/evgenius09/threshold/menu/AnatomyMenu.java`
- Create: `src/main/java/io/github/evgenius09/threshold/registry/ModMenus.java`
- Create: `src/main/java/io/github/evgenius09/threshold/network/OpenAnatomyPayload.java`
- Create: `src/main/java/io/github/evgenius09/threshold/registry/ModNetworking.java`
- Create: `src/client/java/io/github/evgenius09/threshold/client/ModKeys.java`
- Create: `src/client/java/io/github/evgenius09/threshold/client/AnatomyScreen.java`
- Create: `src/main/resources/assets/threshold_gates/textures/gui/anatomy.png`
- Modify: `Threshold.java` (`ModMenus.init(); ModNetworking.init();`), `ThresholdClient.java`

**Interfaces:**
- Consumes: `ModAttachments.BODY`, `BodyData`, `SystemMessages.send`, `OrganItem`.
- Produces:
  ```java
  public class AnatomyMenu extends AbstractContainerMenu {
      public AnatomyMenu(int containerId, Inventory inventory);                     // client
      public AnatomyMenu(int containerId, Inventory inventory, ServerPlayer owner); // server
  }
  public final class ModMenus { public static final MenuType<AnatomyMenu> ANATOMY; public static void init(); }
  public record OpenAnatomyPayload() implements CustomPacketPayload {
      public static final OpenAnatomyPayload INSTANCE;
      public static final CustomPacketPayload.Type<OpenAnatomyPayload> TYPE;   // Threshold.id("open_anatomy")
      public static final StreamCodec<RegistryFriendlyByteBuf, OpenAnatomyPayload> CODEC; // StreamCodec.unit(INSTANCE)
  }
  public final class ModNetworking { public static void init(); }
  // client: ModKeys.init(), AnatomyScreen extends AbstractContainerScreen<AnatomyMenu>
  ```

**Layout (Ruling 4).** Screen 176×176, texture 256×256 with the work area at (0, 0). Slot coordinates are item positions (the 18×18 cell starts 1 px up-left):

| what | position |
| --- | --- |
| `ARMS` cell / label | (8, 24) / left-aligned at (28, 29) |
| `LEGS` cell / label | (8, 60) / left-aligned at (28, 65) |
| `BACK` cell / label | (152, 24) / right-aligned ending at x 148, y 29 |
| `CORE` cell / label | (152, 60) / right-aligned ending at x 148, y 65 |
| player inventory | rows at y 94, 112, 130, x = 8 + 18·col |
| hotbar | y 152 |
| title / inventory label | (8, 6) / (8, 82) (`inventoryLabelY = imageHeight - 94`) |

- [ ] **Step 1: `anatomy.png`**

Throwaway stdlib Python PNG writer in the session scratchpad (not in the repo), 256×256 RGBA, outside 176×176 transparent. Background `#14101A`, 1 px border `#3A3346`. Gray body silhouette `#4A4A55` centered on x 88: head (circle r 7 at (88, 24)), torso x 76–100 y 32–62, arms x 68–75 and 101–108 y 33–60, legs x 78–87 and 89–98 y 63–88. Light cracks `#CFCFE0`: 2–3 one-pixel jagged lines across the torso and one per limb. Cells for the 4 body slots and the 36 inventory slots: 18×18, border `#2A2433`, inside `#0C0A10`. Check: `file` prints `256 x 256, 8-bit/color RGBA`.

- [ ] **Step 2: `AnatomyMenu` and `ModMenus`**

- Private nested `OrganSlot extends Slot`: `mayPlace` only for `OrganItem`; `getMaxStackSize()` 1.
- Client constructor: body cells over a `SimpleContainer(4)`.
- Server constructor: body cells over a private nested `BodyContainer implements Container` bound to the `ServerPlayer`:
  - `getItem(i)` returns the stored stack from `BodyData.organs` (vanilla expects the live stack);
  - `setItem(i, stack)`: new organs list with `stack` at `i`; `selected = body.keepOrSteel(selected)` of the new body; `setAttached` a new `BodyData`. If `stack` is a non-empty `OrganItem` and the old stack was empty or a different item → `SystemMessages.send(player, "graft", stack.getHoverName(), Component.translatable("slot." + MOD_ID + "." + slot lower-case))`;
  - `removeItem(i, n)` / `removeItemNoUpdate(i)`: take the stack, store `EMPTY` the same way;
  - `setChanged()`: store a fresh copy of the current `BodyData`. One comment: vanilla shrinks the stack from `getItem` in place, so every change event writes (Review Focus 1);
  - `stillValid`: player alive. `getContainerSize` 4, `isEmpty`, `clearContent` as expected.
- Player inventory and hotbar slots at the Layout coordinates.
- `quickMoveStack`: body cell → player inventory (indices 4–39); inventory → body cells 0–3 when the stack is an `OrganItem` (the slot limit keeps it to 1); otherwise `ItemStack.EMPTY`. Standard vanilla shape: copy, `moveItemStackTo`, `setByPlayer(EMPTY)` or `setChanged()`.
- `stillValid(Player)` true while the player is alive.
- `ModMenus.ANATOMY = Registry.register(BuiltInRegistries.MENU, Threshold.id("anatomy"), new MenuType<>(AnatomyMenu::new, FeatureFlags.VANILLA_SET))`; `init()` loads the class.

- [ ] **Step 3: `OpenAnatomyPayload` and `ModNetworking`**

- Payload per Interfaces; `type()` returns `TYPE`.
- `ModNetworking.init()`: `PayloadTypeRegistry.playC2S().register(TYPE, CODEC)`; `ServerPlayNetworking.registerGlobalReceiver(TYPE, (p, ctx) -> ctx.player().openMenu(new SimpleMenuProvider((id, inv, pl) -> new AnatomyMenu(id, inv, ctx.player()), Component.translatable("container." + MOD_ID + ".anatomy"))))`.
- `Threshold.onInitialize`: `ModItems.init(); ModAttachments.init(); ModMenus.init(); ModNetworking.init();`.

- [ ] **Step 4: Client `ModKeys`, `AnatomyScreen`, `ThresholdClient`**

- `ModKeys`: one `KeyMapping("key." + MOD_ID + ".anatomy", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories." + MOD_ID)` via `KeyBindingHelper.registerKeyBinding`; `ClientTickEvents.END_CLIENT_TICK`: `while (key.consumeClick())` and `client.player != null` → `ClientPlayNetworking.send(OpenAnatomyPayload.INSTANCE)`.
- `AnatomyScreen`: `imageWidth = imageHeight = 176`, `inventoryLabelY = imageHeight - 94`. `renderBg`: blit `Threshold.id("textures/gui/anatomy.png")` 176×176; for each body slot with an `OrganItem`, `fill` its 18×18 cell area with `0x80000000 | element.color()` (the glow under the organ, criterion 3). `renderLabels`: `super`, then the 4 `slot.threshold_gates.*` labels at the Layout positions, color `0xC0C0C8`. `render`: `renderBackground`, `super.render`, `renderTooltip`.
- `ThresholdClient.onInitializeClient`: `MenuScreens.register(ModMenus.ANATOMY, AnatomyScreen::new); ModKeys.init();`.

- [ ] **Step 5: Checks, push, green, deploy**

```bash
git grep -n '"threshold_gates' -- src/main/java src/client/java   # Expected: only Threshold.java
git grep -n 'threshold.client' -- src/main/java                   # Expected: no output
git add src
git commit -m "feat(menu): add anatomy screen"
git push
gh run watch "$(gh run list --commit "$(git rev-parse HEAD)" --workflow build --limit 1 --json databaseId --jq '.[0].databaseId')" --exit-status
scripts/deploy.sh
grep -iE 'error|exception' build/deploy/latest.log | grep -i threshold   # Expected: no output
```

Expected: CI PASS with `threshold_gates-0.1.5.jar`; `deploy.sh` prints OK. If compilation fails on a Mojang-mapping name, fix it against 1.21.1; that is not a contract change.

- [ ] **Step 6: Hand phone criteria to the owner**

First the owner adds a Zalith button for `K` (tech.md 11). Then in «Threshold Dev», with a gland and a fang from `/give`:
1. `K` opens «Анатомия»: silhouette, 4 labeled cells, inventory below.
2. The gland goes into any of the 4 cells; only 1 fits even from a stack (shift-click a stack of 16: body 1, inventory 15); dirt does not go in; swapping fang for gland in a cell works without losing or duplicating items.
3. An occupied cell glows in the organ's element color.
4. Chat after grafting: «[Система] Орган прижился: Теневая железа, слот «Руки».»
5. Leave and rejoin: the organ is still there; `/data get entity @s "fabric:attachments"."threshold_gates:body"` shows it.
6. After `/kill` the organs stay in the body; nothing drops.
7. The organ comes back out to the inventory (drag and shift-click) and leaves the body (`/data get` shows the slot empty).

- [ ] **Step 7: Definition of Done check (tech.md 15)**

CI green; only this plan's files changed since the plan commit; no attribution lines in `git log --format=%B <plan-commit>..HEAD`; `tech.md` untouched; owner confirmed criteria 1–7.
