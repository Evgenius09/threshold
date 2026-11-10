# tech.md — Threshold (мод Minecraft)

**Версия: v2.1** (2026-11-10)

Changelog:
- v2.1 — репозиторий стал публичным: GitHub Actions на приватном репозитории заблокирован из-за оплаты, а на публичном работает бесплатно. Решение владельца.
- v2 — контракты стадии v0.1. Заморожены: типы ядра (`Element`, `BodySlot`, `Organ`, `Skill`, `LoadState`, `SkillState`, `Body`) и их числа; ID предметов, сущностей, типов урона, меню, attachment, сетевых сообщений, клавиш; ключи локализации v0.1; первый миксин (`LivingEntityMixin`); раскладка игрового слоя; 13 слайсов v0.1 с критериями приёмки. Решения владельца: Fabric GameTest не подключается, игровой слой проверяется руками на телефоне; управление — 5 отдельных клавиш; `debug_shard` удаляется в слайсе 4. Версия мода в стадии v0.1 — `0.1.N`, N — номер слайса.
- v1 — первая версия. Заморожены: ID мода, Java-пакет, раскладка папок, правило чистого ядра, контракт сборки и CI, контракт `scripts/deploy.sh`, тестовый предмет `debug_shard`.

---

## 0. Как читать этот файл

Это источник истины по технике. Читай его перед каждой задачей и подчиняйся дословно.

- Геймдизайн (что игра делает и зачем) — `docs/threshold-plan.md`. Техника (как это устроено в коде) — этот файл. При расхождении по технике прав этот файл, по геймдизайну — план.
- Имена классов, пакетов, ID, ключей локализации и файлов берутся отсюда. Свои варианты не придумывай.
- Нужного контракта здесь нет → СТОП, выдай блок `CONTRACT GAP` (раздел 13). Код с выдуманным именем или типом не пиши.
- Файл меняет только владелец проекта. Каждое изменение контракта поднимает версию сверху файла.
- Работаешь один слайс за заход. Стадии и слайсы — раздел 15, иди по нему сверху вниз.
- Классы, которых нет в разделе 3, не создавай. Приватные вложенные классы и записи внутри перечисленного файла можно.

---

## 1. Проект

Threshold — мод для Minecraft Java Edition: тело из органов новых мобов, стихии, перехват чужой стихии («Подчинение»). Полный геймдизайн и план версий v0.0–v1.0 — `docs/threshold-plan.md`.

Разработчик один, код пишется с нейросетью. Мод делается для себя, публикация возможна после v1.0.

Стадия v0.0 «Фундамент» закрыта 2026-11-03.

Цель текущей стадии: v0.1 «Играбельное ядро». Готово, когда: убил ползуна, получил железу, привил, применил навык, победил Стража, переключая стихии, и не умирал постоянно.

---

## 2. Стек и окружение

Версии заморожены. Меняются только через новую версию этого файла.

| что | версия |
| --- | --- |
| Minecraft | 1.21.1 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.116.17+1.21.1 |
| Fabric Loom | 1.19.3, плагин `net.fabricmc.fabric-loom-remap` |
| Маппинги | официальные Mojang (`loom.officialMojangMappings()`) |
| Gradle | 9.8.2 (wrapper) |
| Java | исходники и байткод 21 (`options.release = 21`); Gradle в CI запускается на JDK 25 |
| Тесты | JUnit 5 (`org.junit.jupiter:junit-jupiter`) |

Основа проекта — ветка `1.21.1` репозитория `FabricMC/fabric-example-mod`.

Маппинги Mojang выбраны потому, что с Minecraft 26.1 игра не обфусцируется и использует те же имена. Перенос на новые версии не потребует переименований.

Модули Fabric API, на которые опирается v0.1 (все входят в зависимость `fabric-api`): Data Attachment (с `syncWith`, есть в 0.116.17), Networking, Key Binding, Rendering (`HudRenderCallback`, `EntityRendererRegistry`), Object Builder (атрибуты сущностей), Biome Modification, Item Group, Lifecycle Events, Entity Events.

Окружение разработки:

- **Машина разработки** (`vps`, Linux, 2 vCPU, 3.8 ГБ RAM, без GPU). Здесь только код, git, `gh` и `adb`. JDK, Gradle и IDE здесь не ставятся, `./gradlew` здесь не запускается: на сборку не хватает памяти.
- **Сборка** — только GitHub Actions (раздел 10).
- **Тестовый клиент** — телефон Samsung Galaxy A22 (Android 12, 4 ГБ RAM), лаунчер Zalith Launcher 2, профиль `1.21.1 Fabric 0.20.1`, офлайн-аккаунт `ThresholdDev`. Связь через Tailscale и `adb` (раздел 11).

Без Architectury, без мультилоадера, без Kotlin, без GeckoLib до v0.9, без сторонних модов-зависимостей кроме Fabric API.

---

## 3. Структура папок

```
.github/workflows/build.yml       CI: сборка, тесты, артефакт jar
docs/
  threshold-plan.md               геймдизайн и план версий
scripts/
  deploy.sh                       CI-артефакт → телефон → перезапуск → лог
src/
  main/
    java/io/github/evgenius09/threshold/
      Threshold.java              main-энтрипоинт: MOD_ID, LOGGER, вызов init() регистраций
      core/                       чистая логика, без net.minecraft (раздел 5)
        Element.java
        BodySlot.java
        Organ.java
        Skill.java
        Body.java
        LoadState.java
        SkillState.java
      registry/
        ModItems.java             предметы органов и яйца призыва
        ModEntities.java          типы сущностей, атрибуты, появление в биомах
        ModMenus.java             MenuType экрана анатомии
        ModAttachments.java       attachment threshold_gates:body
        ModDamageTypes.java       ResourceKey<DamageType> для shadow и storm
        ModNetworking.java        регистрация payload-ов и серверных обработчиков
      item/
        OrganItem.java            предмет органа: Organ + подсказка
      body/
        BodyData.java             запись состояния игрока, Codec и StreamCodec
        BodyTicker.java           серверный тик: Нагрузка, энергия, Ореол, первый орган
        BodyActions.java          переключение стихии, применение навыка
      combat/
        ElementalMob.java         интерфейс моба со стихией
        ElementalDamage.java      расчёт урона со стихией, вызывается из миксина
        ElementParticles.java     частицы цвета стихии
      skill/
        SkillEffects.java         эффекты 6 навыков
      entity/
        ShadowCrawler.java
        StormFang.java
        GateWarden.java
        TelegraphedAttackGoal.java
      menu/
        AnatomyMenu.java
      network/
        OpenAnatomyPayload.java
        SwitchElementPayload.java
        UseSkillPayload.java
      system/
        SystemMessages.java       сообщения Системы в чат
      mixin/
        LivingEntityMixin.java
    resources/
      fabric.mod.json
      threshold_gates.mixins.json
      assets/threshold_gates/
        icon.png
        lang/ru_ru.json
        models/item/<id>.json
        textures/item/<id>.png
        textures/entity/<entity_id>.png
        textures/gui/anatomy.png
      data/threshold_gates/
        damage_type/shadow.json
        damage_type/storm.json
        loot_table/entities/shadow_crawler.json
        loot_table/entities/storm_fang.json
  client/
    java/io/github/evgenius09/threshold/client/
      ThresholdClient.java        client-энтрипоинт
      ModKeys.java                5 клавиш и отправка payload-ов
      BodyHud.java                стихия, энергия, Нагрузка на экране
      AnatomyScreen.java          экран анатомии
      render/
        ShadowCrawlerRenderer.java
        StormFangRenderer.java
        GateWardenRenderer.java
  test/
    java/io/github/evgenius09/threshold/
      *Test.java
CLAUDE.md
tech.md
```

Файл появляется в том слайсе, где он нужен (раздел 15), а не заранее.

Правила размещения:

- Код, который трогает классы Minecraft, лежит вне `core/`. Код в `core/` их не импортирует (раздел 5).
- Клиентский код (рендер, экраны, клавиши, HUD) лежит только в `src/client/`. Серверная сторона не импортирует из `client`. Это держит `splitEnvironmentSourceSets()` в Loom: нарушение не скомпилируется.
- Тексты для игрока только в `lang/ru_ru.json`. Строк на русском в Java нет.
- Миксин в проекте один: `LivingEntityMixin` (раздел 6.5). Новый миксин — только через `CONTRACT GAP` с обоснованием, почему не хватает событий Fabric API.
- Миксин ничего не считает сам: он вызывает `ElementalDamage` и возвращает результат.

---

## 4. Идентификаторы (заморожены)

| что | значение |
| --- | --- |
| ID мода | `threshold_gates` |
| Название мода | `Threshold` |
| Java-пакет | `io.github.evgenius09.threshold` |
| Константа ID в коде | `Threshold.MOD_ID` |
| Имя jar | `threshold_gates-<version>.jar` (`base.archivesName = "threshold_gates"`) |
| Версия мода | `version` в `gradle.properties`. Стадия v0.0 — `0.0.x`. Стадия v0.1 — `0.1.N`, где N — номер слайса; версия поднимается первым коммитом слайса |

Правила:

- ID мода вшивается в сохранения и предметы, менять его нельзя. До публикации (v1.0) проверить занятость на Modrinth и CurseForge.
- `ResourceLocation` создаётся только через `Threshold.id(String path)`, литерал `"threshold_gates"` в Java встречается один раз, в `MOD_ID`.
- ID предметов, блоков, сущностей — `snake_case`, английские.
- Ключи локализации — стандартные ключи Minecraft: `item.threshold_gates.<id>`, `entity.threshold_gates.<id>` и т. д.

`Threshold.java` экспортирует ровно это:

```java
public static final String MOD_ID = "threshold_gates";
public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
public static ResourceLocation id(String path)   // ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
```

ID v0.1 (все в пространстве `threshold_gates:`):

| что | ID |
| --- | --- |
| предметы органов | `shadow_gland`, `thunder_fang` |
| яйца призыва | `shadow_crawler_spawn_egg`, `storm_fang_spawn_egg`, `gate_warden_spawn_egg` |
| сущности | `shadow_crawler`, `storm_fang`, `gate_warden` |
| типы урона | `shadow`, `storm` |
| меню | `anatomy` |
| attachment | `body` |
| payload C2S | `open_anatomy`, `switch_element`, `use_skill` |

Моб `storm_fang` (Грозовой клык) и орган `thunder_fang` (Громовой клык) — разные ID нарочно: у них разные названия в плане.

---

## 5. Чистое ядро `core/`

`io.github.evgenius09.threshold.core` — правила игры: стихии, Нагрузка, энергия и перезарядки, органы и навыки. Это то, что тестируется в CI без запуска игры.

Правила:

- В `core/` нет импортов `net.minecraft.*`, `com.mojang.*`, `net.fabricmc.*`. Это проверяет `CorePurityTest` (раздел 9), нарушение роняет сборку.
- Классы `core/` не знают про тики сервера, сущности и мир. Время задаётся в тиках как `int` (20 тиков = 1 с), урон и ресурсы как `double`.
- Записи ядра неизменяемые: методы возвращают новую запись, а не меняют текущую.
- Связка с игрой (события Fabric API, сущности, Codec, NBT) лежит в соседних пакетах и вызывает `core/`.
- Состояние игрока хранится в игровом слое (attachment, раздел 6.2). `core/` получает и возвращает значения, сам ничего не хранит.
- Числа ниже — стартовый баланс. Они меняются только новой версией этого файла, тесты сверяются с ними.

### 5.1 `Element`

```java
public enum Element {
    STEEL(0xC0C8D0),   // Сталь, серебро
    STORM(0xFFE04A),   // Гроза, жёлтый
    SHADOW(0x8A3FD1);  // Тень, фиолетовый

    public int color()                                            // RGB без альфы
    public static double multiplier(Element attack, Element defender)
    public static Element next(Element current, Set<Element> available)
}
```

`multiplier`:

| атака \ защита | STEEL | STORM | SHADOW |
| --- | --- | --- | --- |
| STEEL | 1.0 | 1.0 | 1.0 |
| STORM | 1.0 | 0.5 | 1.5 |
| SHADOW | 1.0 | 1.5 | 0.5 |

Попадание в слабость — `multiplier > 1.0`.

`next`: следующая стихия из `available` в порядке `STEEL → STORM → SHADOW → STEEL`, начиная после `current`. Если в `available` только `STEEL`, возвращает `STEEL`. Если `current` нет в `available`, поиск всё равно идёт по кругу после `current`.

### 5.2 `BodySlot`

```java
public enum BodySlot { ARMS, LEGS, BACK, CORE }
```

Порядок перечисления — порядок ячеек в `BodyData.organs` и в меню анатомии.

### 5.3 `Organ`

```java
public enum Organ {
    SHADOW_GLAND(Element.SHADOW),   // Теневая железа
    THUNDER_FANG(Element.STORM);    // Громовой клык

    public Element element()
}
```

Семейство, ёмкость и редкость органа из плана в v0.1 не входят и в `Organ` не добавляются (раздел 17). Строка лора — ключ локализации (раздел 8).

### 5.4 `Skill`

```java
public enum Skill {
    SHADOW_BOLT    (Organ.SHADOW_GLAND, BodySlot.ARMS, 20,  60),
    SHADOW_STEP    (Organ.SHADOW_GLAND, BodySlot.LEGS, 20, 120),
    SHADOW_VEIL    (Organ.SHADOW_GLAND, BodySlot.BACK, 35, 400),
    STORM_DISCHARGE(Organ.THUNDER_FANG, BodySlot.ARMS, 25, 100),
    STORM_DASH     (Organ.THUNDER_FANG, BodySlot.LEGS, 20, 240),
    STORM_HALO     (Organ.THUNDER_FANG, BodySlot.BACK, 35, 300);

    public Organ organ()
    public BodySlot slot()
    public double energyCost()
    public int cooldownTicks()
    public static Optional<Skill> of(Organ organ, BodySlot slot)  // CORE → всегда empty
}
```

Навык = орган × слот. Слот `CORE` навыков не даёт.

### 5.5 `Body`

```java
public record Body(Map<BodySlot, Organ> organs) {   // пустой слот — ключа нет
    public Set<Element> elements()                    // STEEL + стихии привитых органов
    public Optional<Skill> skill(BodySlot slot)       // Skill.of(органа в слоте, slot)
    public double resistance(Element attack)          // 0.5, если орган в CORE той же стихии; иначе 1.0
    public Element keepOrSteel(Element selected)      // selected, если он в elements(); иначе STEEL
}
```

Конструктор копирует карту в неизменяемую (`Map.copyOf`).

### 5.6 `LoadState` — Нагрузка

```java
public record LoadState(double load, int rejectionTicks) {
    public static final double MAX = 100.0;
    public static final double SWITCH_COST = 25.0;
    public static final double DECAY_PER_TICK = 0.25;   // 5 в секунду
    public static final int REJECTION_TICKS = 160;      // 8 с
    public static final LoadState EMPTY = new LoadState(0.0, 0);

    public boolean rejecting()        // rejectionTicks > 0
    public LoadState afterSwitch()
    public LoadState tick()
}
```

- `afterSwitch`: при `rejecting()` возвращает `this`. Иначе `load = min(MAX, load + SWITCH_COST)`; если стало `MAX`, `rejectionTicks = REJECTION_TICKS` (начало отторжения).
- `tick`: при отторжении `rejectionTicks - 1`; когда он стал 0, `load = 0`. Без отторжения `load = max(0, load - DECAY_PER_TICK)`.
- Во время отторжения `load` остаётся `MAX`, полоска на экране полная.

### 5.7 `SkillState` — энергия и перезарядки

```java
public record SkillState(double energy, Map<Skill, Integer> cooldowns) {  // нет ключа = нет перезарядки
    public static final double MAX_ENERGY = 100.0;
    public static final double REGEN_PER_TICK = 0.2;    // 4 в секунду
    public static final SkillState FULL = new SkillState(MAX_ENERGY, Map.of());

    public int cooldown(Skill skill)       // 0, если ключа нет
    public boolean canUse(Skill skill)     // energy >= energyCost и cooldown == 0
    public SkillState afterUse(Skill skill) // без canUse возвращает this
    public SkillState tick()
}
```

- `afterUse`: `energy - energyCost`, `cooldowns[skill] = cooldownTicks`.
- `tick`: `energy = min(MAX_ENERGY, energy + REGEN_PER_TICK)`, каждая перезарядка минус 1, нули удаляются из карты.
- Конструктор копирует карту в неизменяемую.

---

## 6. Игровой слой v0.1

Тестовый предмет `debug_shard` из v1 удаляется в слайсе 4 вместе с моделью, текстурой и ключом локализации.

### 6.1 Предметы

- `OrganItem extends Item`, поле `Organ organ()`. Свойства: `new Item.Properties().stacksTo(16)`.
- Подсказка (`appendHoverText`): строка `item.threshold_gates.organ.element` с названием стихии цветом `Element.color()`, затем строка лора `item.threshold_gates.<id>.lore` серым курсивом.
- `ModItems.SHADOW_GLAND` (`Organ.SHADOW_GLAND`), `ModItems.THUNDER_FANG` (`Organ.THUNDER_FANG`). Вкладка творческого режима: ванильная `CreativeModeTabs.INGREDIENTS`.
- Яйца призыва: ванильный `SpawnEggItem`, `ModItems.SHADOW_CRAWLER_SPAWN_EGG`, `STORM_FANG_SPAWN_EGG`, `GATE_WARDEN_SPAWN_EGG`. Вкладка: `CreativeModeTabs.SPAWN_EGGS`. Модель: родитель `minecraft:item/template_spawn_egg`. Цвета (фон, пятна): ползун `0x1E1424`, `0x8A3FD1`; клык `0x2A3440`, `0xFFE04A`; Страж `0x4A4A55`, `0x7FD8FF`.
- Текстуры органов 16×16 в стиле «Трещины света»: железа — тёмный сгусток с фиолетовой трещиной, клык — светлый клык с жёлтой трещиной.
- Своей вкладки творческого режима нет.

### 6.2 Состояние игрока: attachment `threshold_gates:body`

```java
public record BodyData(
    List<ItemStack> organs,          // ровно 4, индекс = BodySlot.ordinal(), пусто = ItemStack.EMPTY
    Element selected,
    LoadState load,
    SkillState skills,
    int haloTicks,                   // сколько тиков ещё работает Ореол
    boolean seenOrgan,               // сообщение о первом органе уже было
    Map<Element, Double> damageDealt // урон, нанесённый игроком, по стихиям
) {
    public static final BodyData DEFAULT;          // пустые слоты, STEEL, EMPTY, FULL, 0, false, пустая карта
    public static final Codec<BodyData> CODEC;
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyData> STREAM_CODEC;
    public Body body()                             // органы из OrganItem в слотах
}
```

- `ModAttachments.BODY` создаётся через `AttachmentRegistry.create(Threshold.id("body"), b -> b.initializer(() -> BodyData.DEFAULT).persistent(BodyData.CODEC).copyOnDeath().syncWith(BodyData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()))`.
- Codec и StreamCodec записей ядра пишутся внутри `BodyData`, в ядре их нет.
- После смерти (`ServerPlayerEvents.AFTER_RESPAWN`): органы, `selected`, `seenOrgan`, `damageDealt` остаются; `load = LoadState.EMPTY`, `skills = SkillState.FULL`, `haloTicks = 0`.
- Изменения пишутся через `setAttached` только когда значение изменилось. Синхронизация на клиент идёт сама (`syncWith`), своих S2C payload-ов нет.
- Проверка на телефоне: `/data get entity @s "fabric:attachments"."threshold_gates:body"`.

`BodyTicker` (регистрируется на `ServerTickEvents.END_SERVER_TICK`, обходит игроков сервера), каждый тик:

1. `load = load.tick()`; `skills = skills.tick()`.
2. `haloTicks > 0` → импульс Ореола по разделу 6.7, `haloTicks - 1`.
3. Раз в 20 тиков: если `!seenOrgan` и в инвентаре есть `OrganItem` → сообщение `first_organ`, `seenOrgan = true`.

### 6.3 Клавиши и сеть

Клавиши (`client/ModKeys`, через `KeyBindingHelper.registerKeyBinding`, категория `key.categories.threshold_gates`):

| ключ | клавиша | действие |
| --- | --- | --- |
| `key.threshold_gates.skill_arms` | R | `UseSkillPayload(ARMS)` |
| `key.threshold_gates.skill_legs` | V | `UseSkillPayload(LEGS)` |
| `key.threshold_gates.skill_back` | G | `UseSkillPayload(BACK)` |
| `key.threshold_gates.switch_element` | Z | `SwitchElementPayload` |
| `key.threshold_gates.anatomy` | K | `OpenAnatomyPayload` |

На телефоне владелец сам добавляет кнопки под эти клавиши в редакторе раскладки Zalith.

Payload-ы (все C2S, `record ... implements CustomPacketPayload`, регистрация в `ModNetworking` через `PayloadTypeRegistry.playC2S()` и `ServerPlayNetworking.registerGlobalReceiver`):

- `OpenAnatomyPayload()` → сервер открывает `AnatomyMenu`.
- `SwitchElementPayload()` → `BodyActions.switchElement(ServerPlayer)`.
- `UseSkillPayload(BodySlot slot)` → `BodyActions.useSkill(ServerPlayer, BodySlot)`.

Клиент только сообщает о нажатии. Все проверки (энергия, перезарядка, отторжение) делает сервер.

`BodyActions.switchElement`:

1. `next = Element.next(selected, body.elements())`. Если `next == selected` → ничего, Нагрузка не растёт.
2. Если `load.rejecting()` → ничего.
3. `selected = next`, `load = load.afterSwitch()`, всплеск частиц цвета `next` вокруг игрока (раздел 6.9).
4. Если после шага 3 `load.rejecting()` → Слабость I и Медлительность I на `REJECTION_TICKS`, сообщение `rejection`.

`BodyActions.useSkill`:

1. `skill = body.skill(slot)`; пусто → action bar `no_skill`.
2. `!skills.canUse(skill)`: перезарядка → action bar `cooldown` (секунды, округление вверх); иначе → `no_energy`.
3. `SkillEffects.cast(skill, player)` вернул `false` → action bar `no_target`, энергия не тратится.
4. Иначе `skills = skills.afterUse(skill)`.

### 6.4 Экран анатомии и HUD

- `AnatomyMenu extends AbstractContainerMenu`, `ModMenus.ANATOMY = new MenuType<>(AnatomyMenu::new, FeatureFlags.VANILLA_SET)`.
- Верх: 4 ячейки тела по `BodySlot`, в ячейку кладётся только `OrganItem`, максимум 1 штука. Низ: инвентарь игрока и хотбар, как у сундука. Shift-клик переносит орган в первую подходящую пустую ячейку.
- На сервере ячейки тела читают и пишут `BodyData.organs` напрямую. Предмет, положенный в ячейку, сразу прижит, вынутый сразу снят. Штрафа за перестановку нет.
- Орган положен в ячейку → сообщение `graft`. После любого изменения ячеек `selected = body.keepOrSteel(selected)`, без Нагрузки.
- `client/AnatomyScreen extends AbstractContainerScreen<AnatomyMenu>`, фон `textures/gui/anatomy.png` (рабочая область 176×176): тёмный фон, серый силуэт тела с трещинами, ячейки на руках, ногах, спине и в груди. Под занятой ячейкой рисуется свечение цвета стихии органа. Подписи ячеек — ключи `slot.threshold_gates.*`. Заголовок — `container.threshold_gates.anatomy`.
- `client/BodyHud` (`HudRenderCallback`), левый верхний угол, отступ 4 px: название выбранной стихии её цветом; полоска энергии 80×4 цветом `0x4FA3FF`; полоска Нагрузки 80×4 цветом `0xD03030`. При отторжении полоска Нагрузки мигает (2 раза в секунду). Данные берутся из attachment на клиенте.

### 6.5 Урон со стихией

`LivingEntityMixin` — `@ModifyVariable` на аргумент `amount` в начале `LivingEntity#hurt(DamageSource, float)`. Тело: `return ElementalDamage.modify((LivingEntity)(Object)this, source, amount);`. Конфиг `threshold_gates.mixins.json`: пакет `io.github.evgenius09.threshold.mixin`, `compatibilityLevel` `JAVA_21`, `mixins: ["LivingEntityMixin"]`, `injectors.defaultRequire = 1`.

Обоснование миксина: `ServerLivingEntityEvents.ALLOW_DAMAGE` в Fabric API умеет только отменить урон, а изменить его величину не умеет.

`ElementalDamage.modify(victim, source, amount)`, только на сервере (на клиенте возвращает `amount`):

1. **Стихия атаки**, первое подходящее правило:
   - тип урона `threshold_gates:shadow` → `SHADOW`, `threshold_gates:storm` → `STORM`;
   - `source.getEntity()` — игрок → его `selected` (так же для стрел из его лука);
   - `source.getEntity()` — `ElementalMob` → его `element()`;
   - иначе `STEEL`.
2. **Стихия защиты**: жертва `ElementalMob` → её `element()`; иначе `STEEL` (игрок и ванильные мобы).
3. `result = amount × Element.multiplier(атака, защита)`; если жертва — игрок, ещё `× body.resistance(атака)`.
4. `multiplier > 1.0` → вспышка частиц цвета атаки на жертве (раздел 6.9).
5. `source.getEntity()` — игрок и `result > 0` → `damageDealt[атака] += result`.
6. Возвращает `result`.

`ElementalMob` (интерфейс в `combat/`): `Element element();`.

Типы урона (`data/threshold_gates/damage_type/<id>.json`): `{"message_id": "threshold_gates.<id>", "exhaustion": 0.1, "scaling": "never"}`. Ключи в `ModDamageTypes.SHADOW`, `ModDamageTypes.STORM`. Урон навыков создаётся с игроком как источником (`new DamageSource(holder, player)`), чтобы мобы злились на игрока, а счётчик видел удар.

### 6.6 Мобы

Модели ванильные, текстуры — перекраска ванильных в цвет стихии (замена своими моделями в v0.9). Все три класса реализуют `ElementalMob`. Регистрация в `ModEntities`, атрибуты через `FabricDefaultAttributeRegistry`, рендереры в `ThresholdClient` через `EntityRendererRegistry`.

| | `ShadowCrawler` | `StormFang` | `GateWarden` |
| --- | --- | --- | --- |
| основа | `extends Spider` | `extends Wolf` | `extends IronGolem` |
| `MobCategory` | `MONSTER` | `MONSTER` | `MONSTER` |
| размер | как у паука | как у волка | как у голема |
| `max_health` | 16 | 30 | 150 |
| `attack_damage` | 3 | 6 | 8 |
| `movement_speed` | 0.3 | 0.3 | 0.25 |
| `knockback_resistance` | — | — | 1.0 |
| стихия | `SHADOW` | `STORM` | меняется |
| предупреждение | нет | `TelegraphedAttackGoal` | `TelegraphedAttackGoal` |
| дроп | `shadow_gland`, 50% | `thunder_fang`, 35% | ничего |
| появление | ночь, поверхность Верхнего мира | ночь, поверхность Верхнего мира | только яйцо |
| вес / группа | 40 / 1–2 | 15 / 1 | — |
| текстура | `textures/entity/shadow_crawler.png` | `textures/entity/storm_fang.png` | `textures/entity/gate_warden.png` |

- Таблицы дропа: `data/threshold_gates/loot_table/entities/<id>.json`, один пул, условия `minecraft:killed_by_player` и `minecraft:random_chance`. Ванильный дроп основы (нить, глаз паука) не наследуется.
- Появление: `BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, ...)`, правила как у монстров: темнота, твёрдый блок снизу. Исчезают вдали от игрока и на мирной сложности, как монстры.
- `StormFang`: не приручается, не садится, всегда нападает на ближайшего игрока в 16 блоках.
- `GateWarden`: не нападает на монстров, нападает на ближайшего игрока в 24 блоках. Своего естественного появления и Врат в v0.1 нет.
- `TelegraphedAttackGoal(mob)`: цель в досягаемости → 10 тиков замаха, каждый тик частицы цвета `element()` моба (раздел 6.9), затем удар, если цель ещё в досягаемости. Пауза между ударами 20 тиков. Заменяет ванильную цель ближней атаки у клыка и Стража.

**Страж врат.** Стихия на старте `SHADOW`, каждые 400 тиков меняется `SHADOW ↔ STORM`. Слабость — вторая стихия (по таблице 5.1). Полоса босса (`ServerBossEvent`, видна игрокам в 32 блоках): название `entity.threshold_gates.gate_warden.bar` с названием слабости, цвет полосы `YELLOW` при слабости `STORM` и `PURPLE` при слабости `SHADOW`. Раз в 10 тиков вокруг Стража частицы цвета слабости. Сообщения: `warden_awake` при появлении, `warden_weakness` при смене, `warden_defeated` при смерти, игрокам в 48 блоках.

### 6.7 Навыки (`SkillEffects`)

`static boolean cast(Skill skill, ServerPlayer player)` — `true`, если навык сработал. Урон навыков — тип урона стихии органа, источник — игрок; множители применяет раздел 6.5.

| навык | что делает | `false`, если |
| --- | --- | --- |
| `SHADOW_BOLT` | мгновенный луч по взгляду до 16 блоков, блоки его останавливают; первое живое существо на пути получает 6 урона `shadow`; след частиц Тени каждые 0,5 блока | никогда (промах тоже тратит энергию) |
| `SHADOW_STEP` | телепорт до 6 блоков по горизонтали по взгляду, в последнюю точку, где помещается игрок; частицы Тени в начале и в конце | свободного места меньше 1 блока |
| `SHADOW_VEIL` | Невидимость на 80 тиков без частиц эффекта; мобы в 16 блоках, у которых цель — этот игрок, теряют цель | никогда |
| `STORM_DISCHARGE` | живое существо под прицелом до 10 блоков в прямой видимости получает 7 урона `storm`; в цель бьёт молния `setVisualOnly(true)` | нет цели |
| `STORM_DASH` | Скорость II на 100 тиков | никогда |
| `STORM_HALO` | `haloTicks = 80`; каждые 10 тиков (8 импульсов) все живые существа в 3 блоках, кроме самого игрока, получают 2 урона `storm`; кольцо частиц Грозы | никогда |

### 6.8 Сообщения Системы

`SystemMessages.send(ServerPlayer, String id, Object... args)` шлёт в чат `Component.translatable("message.threshold_gates.system." + id, args)` цветом `0xA8B4C0`. Текст сообщения начинается с `[Система]` прямо в переводе. Сообщения v0.1: `first_organ`, `graft`, `rejection`, `warden_awake`, `warden_weakness`, `warden_defeated`. На каждый удар сообщений нет.

Подсказки в action bar (`player.displayClientMessage(..., true)`): `message.threshold_gates.actionbar.no_skill`, `cooldown`, `no_energy`, `no_target`.

### 6.9 Частицы

Все частицы мода — `DustParticleOptions` цвета `Element.color()`, рассылаются с сервера через `ServerLevel.sendParticles`. Хелперы в `ElementParticles`:

- `burst(level, pos, element)` — 16 частиц разлёта (переключение стихии, вспышка слабости, концы `SHADOW_STEP`);
- `trail(level, from, to, element)` — линия через каждые 0,5 блока (`SHADOW_BOLT`);
- `ring(level, center, radius, element)` — кольцо из 24 частиц (`STORM_HALO`);
- `aura(level, entity, element)` — 4 частицы у тела (замах, аура Стража).

---

## 7. `fabric.mod.json`

```json
{
  "schemaVersion": 1,
  "id": "threshold_gates",
  "version": "${version}",
  "name": "Threshold",
  "description": "Body built from organs, elements and the law of Subjugation.",
  "authors": ["Evgenius09"],
  "license": "All-Rights-Reserved",
  "icon": "assets/threshold_gates/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": ["io.github.evgenius09.threshold.Threshold"],
    "client": ["io.github.evgenius09.threshold.client.ThresholdClient"]
  },
  "mixins": ["threshold_gates.mixins.json"],
  "depends": {
    "fabricloader": ">=0.20.1",
    "minecraft": "~1.21.1",
    "java": ">=21",
    "fabric-api": "*"
  }
}
```

Строка `mixins` появляется в слайсе 8 вместе с миксином. Лицензия закрытая до решения в v1.0.

---

## 8. Локализация

- Основной и единственный в v0.1 язык — `ru_ru.json`. Английский `en_us.json` появляется в v0.9.
- Каждый зарегистрированный предмет, сущность, тип урона имеет ключ в `ru_ru.json`. Это проверяет `LangTest`.
- Сообщения Системы — ключи локализации, через `Component.translatable`, не литералы.
- Ключ добавляется в том слайсе, где его начинает использовать код.

Ключи v0.1 (тексты стартовые, правит владелец):

| ключ | текст |
| --- | --- |
| `item.threshold_gates.shadow_gland` | Теневая железа |
| `item.threshold_gates.shadow_gland.lore` | Сочится тьмой даже на свету. |
| `item.threshold_gates.thunder_fang` | Громовой клык |
| `item.threshold_gates.thunder_fang.lore` | Гудит перед грозой. |
| `item.threshold_gates.organ.element` | Стихия: %s |
| `item.threshold_gates.shadow_crawler_spawn_egg` | Яйцо призыва теневого ползуна |
| `item.threshold_gates.storm_fang_spawn_egg` | Яйцо призыва грозового клыка |
| `item.threshold_gates.gate_warden_spawn_egg` | Яйцо призыва стража врат |
| `entity.threshold_gates.shadow_crawler` | Теневой ползун |
| `entity.threshold_gates.storm_fang` | Грозовой клык |
| `entity.threshold_gates.gate_warden` | Страж врат |
| `entity.threshold_gates.gate_warden.bar` | Страж врат · слабость: %s |
| `element.threshold_gates.steel` | Сталь |
| `element.threshold_gates.storm` | Гроза |
| `element.threshold_gates.shadow` | Тень |
| `slot.threshold_gates.arms` | Руки |
| `slot.threshold_gates.legs` | Ноги |
| `slot.threshold_gates.back` | Спина |
| `slot.threshold_gates.core` | Ядро |
| `container.threshold_gates.anatomy` | Анатомия |
| `key.categories.threshold_gates` | Threshold |
| `key.threshold_gates.skill_arms` | Навык рук |
| `key.threshold_gates.skill_legs` | Навык ног |
| `key.threshold_gates.skill_back` | Навык спины |
| `key.threshold_gates.switch_element` | Сменить стихию |
| `key.threshold_gates.anatomy` | Анатомия |
| `death.attack.threshold_gates.shadow` | %1$s растворился во тьме |
| `death.attack.threshold_gates.shadow.player` | %1$s растворился во тьме от руки %2$s |
| `death.attack.threshold_gates.storm` | %1$s сгорел в грозе |
| `death.attack.threshold_gates.storm.player` | %1$s сгорел в грозе от руки %2$s |
| `message.threshold_gates.actionbar.no_skill` | В этом слоте нет навыка |
| `message.threshold_gates.actionbar.cooldown` | Перезарядка: %s с |
| `message.threshold_gates.actionbar.no_energy` | Не хватает энергии |
| `message.threshold_gates.actionbar.no_target` | Нет цели |
| `message.threshold_gates.system.first_organ` | [Система] Получен орган: %s. Тело способно его принять. |
| `message.threshold_gates.system.graft` | [Система] Орган прижился: %s, слот «%s». |
| `message.threshold_gates.system.rejection` | [Система] Тело отторгает стихии. Переключение недоступно. |
| `message.threshold_gates.system.warden_awake` | [Система] Страж врат пробудился. Его слабость меняется. |
| `message.threshold_gates.system.warden_weakness` | [Система] Слабость Стража: %s. |
| `message.threshold_gates.system.warden_defeated` | [Система] Страж врат повержен. Порог пройден. |

На клиенте с английским языком без `en_us.json` игрок увидит сырые ключи, это ожидаемо до v0.9.

---

## 9. Тесты

Тесты выводятся из критериев приёмки слайса, а не из готового кода. Критерии написаны в разделе 15 до того, как написан код. Тест кодирует критерий, а не повторяет реализацию.

Запрещённый вид теста: «посчитали урон, проверили, что посчитался». Такой тест зелёный и на сломанной формуле. Тест сверяет результат с числом из раздела 5 или из критерия (например, Гроза по Тени ×1.5).

Слои тестов:

- **JUnit на `core/`** — основной слой. Чистые функции, без Minecraft на classpath во время теста. Граничные значения и инварианты (Нагрузка не уходит ниже нуля и не выше `MAX`, энергия не выше `MAX_ENERGY`, перезарядка не уходит ниже нуля). Дробные числа сравниваются с допуском `1e-9`.
- **Структурные тесты** — `CorePurityTest`: читает исходники `src/main/java/.../core/**` и падает, если находит импорт из раздела 5. `LangTest`: читает `ru_ru.json` и сверяет с явным списком ключей в тесте (ID предметов, сущностей, типов урона). Список пополняется в слайсе, где появляется объект.
- **Проверка в игре** — руками на телефоне через `scripts/deploy.sh`, по пунктам «на телефоне: …» в критериях слайса. Числа видны ванильными командами в мире «Threshold Dev»: здоровье — `/data get entity @n[type=<id>] Health`, состояние игрока — `/data get entity @s "fabric:attachments"."threshold_gates:body"`, урон стихией — `/damage <цель> <число> threshold_gates:<shadow|storm>`.

Fabric GameTest не подключается (решение владельца в v2): игровой слой проверяется на телефоне.

Тесты лежат в `src/test/java/io/github/evgenius09/threshold/` и запускаются задачей `test`, которая входит в `build`. Имена тестов ядра: `<Класс>Test` (`ElementTest`, `LoadStateTest`, `SkillTest`, `SkillStateTest`, `BodyTest`).

---

## 10. Сборка, CI, гейт

Гейт — `./gradlew build` в GitHub Actions: компиляция, `test`, сборка jar. Красный CI → слайс не закрыт.

Локально `./gradlew` не запускается (раздел 2). Проверка кода до push — только чтение и тесты в CI.

`.github/workflows/build.yml`, по шаблону Fabric:

- триггеры: `push` в любую ветку, `pull_request`;
- `runs-on: ubuntu-24.04`;
- шаги: `actions/checkout@v6` → `gradle/actions/wrapper-validation@v6` → `actions/setup-java@v5` (JDK 25, `microsoft`) → `gradle/actions/setup-gradle@v6` (кеш) → `./gradlew build`;
- артефакт: `actions/upload-artifact@v7`, имя **`threshold-jar`**, путь `build/libs/threshold_gates-*.jar` без `-sources.jar`, `retention-days: 7`.

Имя артефакта `threshold-jar` — контракт для `deploy.sh`, менять вместе.

Репозиторий: публичный `Evgenius09/threshold` на GitHub (открыт 2026-11-10, чтобы Actions работал бесплатно). Ветка `main`. Разработчик один, работа идёт прямо в `main` маленькими коммитами, без PR. Каждый push гоняет CI.

---

## 11. Телефон и `scripts/deploy.sh`

Цикл «правка → игра»: commit → push → CI → `scripts/deploy.sh` → проверка в игре.

Параметры (переменные окружения, со значениями по умолчанию):

| переменная | по умолчанию |
| --- | --- |
| `THRESHOLD_ADB` | `100.98.166.61:5555` |
| `THRESHOLD_PKG` | `com.movtery.zalithlauncher.v2` |
| `THRESHOLD_INSTANCE` | `/sdcard/Android/data/com.movtery.zalithlauncher.v2/files/.minecraft/versions/1.21.1 Fabric 0.20.1` |

Что делает `deploy.sh`, по шагам:

1. Находит последний run workflow `build` для текущего `HEAD` (`gh run list --commit`), ждёт окончания (`gh run watch --exit-status`). CI красный → выход с кодом 1.
2. Скачивает артефакт `threshold-jar` в `build/deploy/` (`gh run download`).
3. Удаляет на телефоне `"$THRESHOLD_INSTANCE/mods/threshold_gates-*.jar"` и кладёт новый jar через `adb push`.
4. Останавливает игру (`am force-stop`), запускает лаунчер и нажимает «Запустить» (поиск кнопки по тексту через `uiautomator dump`).
5. Ждёт в `logs/latest.log` строку загрузки модов, до 180 с. Проверяет, что в списке есть `threshold_gates`, и что нет `Crash report` и `ERROR`, где упоминается `threshold`.
6. Копирует лог в `build/deploy/latest.log` и печатает итог: ОК или первые строки ошибки.

Правила:

- `deploy.sh` не трогает другие моды, миры, `options.txt` и аккаунты на телефоне.
- `adb` второго устройства (SaluteTV) не используется. Скрипт всегда передаёт `-s "$THRESHOLD_ADB"`.
- Особенности управления: касание в игре работает как тачпад, клик срабатывает в точке курсора, свайп двигает курсор 1:1. Удержание джойстика через `adb input swipe` персонажа не двигает. Ходить и сражаться в игре — руками на телефоне.
- Кнопки под клавиши мода (раздел 6.3) владелец добавляет в Zalith сам. Нейросеть раскладку лаунчера не меняет.
- Проверки «на телефоне» делает владелец. Нейросеть может запустить `deploy.sh`, проверить лог и сделать скриншот, но бой и интерфейс проверяет владелец.

Тестовый мир: «Threshold Dev», творческий режим, команды включены. Для боя владелец переключает режим командой `/gamemode survival`.

---

## 12. Конвенция текста и коммитов

Код, комментарии, коммиты, лог — по-английски. Тексты для игрока — по-русски, только в `ru_ru.json`.

Коммиты, Conventional Commits, фиксированный формат:

```
type(scope): summary
```

- `type` из набора `feat|fix|test|refactor|chore|docs`;
- `scope` — пакет или область: `core`, `registry`, `item`, `body`, `combat`, `skill`, `entity`, `menu`, `network`, `system`, `mixin`, `client`, `ci`, `deploy`, `lang`, `docs`;
- `summary` в императиве, со строчной буквы, без точки, до 50 символов;
- тело только чтобы объяснить *почему*, не *что*;
- коммитить по ходу работы маленькими шагами, не одним коммитом в конце.

**Авторство.** Автор коммитов — владелец проекта (`Evgenius09 <79006129+Evgenius09@users.noreply.github.com>`, задано в локальном `git config` репозитория). В коммиты не добавлять `Co-Authored-By`, `Signed-off-by` и любые другие строки об авторстве или «сгенерировано». В репозитории не добавлять коллабораторов.

Примеры: `feat(core): add element multipliers`, `test(core): cover weakness multiplier`.

Правила письма для всей прозы (коммиты, комментарии, документы): активный залог, конкретика вместо общих фраз, без вводных оборотов, без длинного тире, без наречий-усилителей. Комментарий объясняет причину решения, а не пересказывает строку. Закомментированный код не оставлять.

---

## 13. CONTRACT GAP

Не хватает имени, типа, ID, файла или правила — работа останавливается. Выдай блок и жди ответа:

```
CONTRACT GAP
Что нужно: <класс/метод/ID/файл/правило>
Зачем: <какой критерий приёмки без него не выполняется>
Предлагаемая форма: <точная сигнатура, ID или путь>
Что делаю пока: <заглушка локально в своём слайсе / жду>
```

Код с выдуманным контрактом не пиши. ID, пакеты, раскладку папок, версии стека, имя артефакта сам не меняй.

Не считается пробелом: приватные методы, поля и вложенные классы внутри файла из раздела 3; выбор ванильного API для того, что здесь описано поведением (например, как именно найти цель под прицелом).

---

## 14. Правила поведения в сессии

- Думай до кода: назови допущения, спроси при неоднозначности, покажи варианты вместо молчаливого выбора.
- Простота: никаких фич сверх слайса, никаких абстракций под одноразовый код, никакой обработки ошибок, которых не бывает.
- Хирургические правки: соседний рабочий код не улучшать и не рефакторить. Каждая изменённая строка следует из текущей задачи.
- Один слайс за заход.
- Механики сверяются с `docs/threshold-plan.md`. Что в плане помечено «не входит» в текущую версию, не делается.
- Ревью идёт вторым заходом, после того как слайс готов и CI зелёный.

---

## 15. Стадии и слайсы

Порядок жёсткий, сверху вниз. Один слайс за один заход. Слайс закрыт, когда выполнен Definition of Done.

**Definition of Done одного слайса:**

1. Критерии приёмки слайса выполнены. Критерии «на телефоне» проверены после `scripts/deploy.sh`.
2. Тесты написаны из критериев, CI зелёный.
3. Новых файлов, классов и абстракций сверх описанных в этом файле нет.
4. Мёртвого кода нет.
5. Коммиты по конвенции раздела 12, без строк соавторства.
6. `tech.md` не изменён (изменение контракта идёт через раздел 13).
7. Версия мода поднята до `0.1.N` (стадия v0.1).

Стадии соответствуют версиям плана:

- **Стадия v0.0 — фундамент.** Слайс 0. Закрыта.
- **Стадия v0.1 — играбельное ядро.** Слайсы 1–13 ниже.
- **Стадии v0.2–v1.0** — по `docs/threshold-plan.md`, детализируются по мере подхода.

---

### Слайс 0 — фундамент (закрыт 2026-11-03)

**Что собрать:**

- Репозиторий `Evgenius09/threshold` (приватный), локальный `git config user.name/user.email` из раздела 12.
- Проект из ветки `1.21.1` шаблона `fabric-example-mod`: Gradle wrapper, `build.gradle`, `settings.gradle` (`rootProject.name = 'threshold_gates'`), `gradle.properties` с версиями раздела 2 (`loom_version=1.19.3`, `group=io.github.evgenius09`, `version=0.0.1`), `.gitignore`, `.gitattributes`.
- Из шаблона убрать: примеры миксинов и их json, `maven-publish`, `LICENSE` CC0 (лицензия закрытая), `README.md` шаблона.
- `build.gradle`: `base.archivesName = "threshold_gates"`, зависимость `testImplementation "org.junit.jupiter:junit-jupiter:5.11.4"` и `testRuntimeOnly "org.junit.platform:junit-platform-launcher"`, `test { useJUnitPlatform() }`.
- `Threshold.java`, `ThresholdClient.java` (пустой `onInitializeClient`), `core/package-info.java`, `registry/ModItems.java`.
- `fabric.mod.json` по разделу 7 (без строки `mixins`), `icon.png` 128×128.
- Тестовый предмет `debug_shard` (описан в v1 этого файла, раздел 6).
- `.github/workflows/build.yml` по разделу 10.
- `scripts/deploy.sh` по разделу 11, `bash`, `set -euo pipefail`.

**Критерии приёмки** (все выполнены):

1. CI на push в `main` зелёный, артефакт `threshold-jar` содержит ровно один jar `threshold_gates-0.0.1.jar`.
2. `CorePurityTest` падает, если в `core/` добавить `import net.minecraft.world.item.Item;`.
3. `LangTest` падает, если удалить ключ `item.threshold_gates.debug_shard` из `ru_ru.json`.
4. `scripts/deploy.sh` с нуля доводит свежий коммит до запущенной игры на телефоне и печатает ОК.
5. На телефоне: в списке модов есть `threshold_gates 0.0.1`.
6. На телефоне: `/give @s threshold_gates:debug_shard` выдаёт предмет с названием и текстурой.
7. В `latest.log` после загрузки мира нет `ERROR` и исключений, где упоминается `threshold`.

---

### Слайс 1 — стихии

**Что собрать:** `core/Element.java` по разделу 5.1. `core/package-info.java` остаётся.

**Критерии приёмки:**

1. `multiplier` совпадает со всеми 9 клетками таблицы 5.1.
2. `color()`: `STEEL` = `0xC0C8D0`, `STORM` = `0xFFE04A`, `SHADOW` = `0x8A3FD1`.
3. `next(STEEL, {STEEL, STORM, SHADOW})` = `STORM`, `next(STORM, …)` = `SHADOW`, `next(SHADOW, …)` = `STEEL`.
4. `next(STEEL, {STEEL, SHADOW})` = `SHADOW`; `next(SHADOW, {STEEL, SHADOW})` = `STEEL`.
5. `next(STEEL, {STEEL})` = `STEEL`.
6. `next(STORM, {STEEL, SHADOW})` = `SHADOW` (текущей стихии нет в наборе).

**Тесты:** `ElementTest` по критериям 1–6.

**Не делать:** игровой код, ключи локализации стихий (появятся в слайсе 4).

---

### Слайс 2 — Нагрузка

**Что собрать:** `core/LoadState.java` по разделу 5.6.

**Критерии приёмки:**

1. С `EMPTY` одно `afterSwitch` → `load` 25, не отторжение.
2. С `EMPTY` четыре `afterSwitch` подряд → `load` 100, `rejectionTicks` 160.
3. С `load` 90 `afterSwitch` → `load` 100 и отторжение (не 115).
4. При отторжении `afterSwitch` возвращает то же состояние.
5. С `load` 25 без отторжения 20 `tick` → `load` 20; 100 `tick` → 0, дальше не уходит ниже 0.
6. При отторжении 159 `tick` → `load` 100, отторжение идёт; 160-й `tick` → `load` 0, `rejecting()` = false.

**Тесты:** `LoadStateTest` по критериям 1–6.

**Не делать:** игровой код, HUD.

---

### Слайс 3 — органы, слоты, навыки, энергия

**Что собрать:** `core/BodySlot.java`, `core/Organ.java`, `core/Skill.java`, `core/Body.java`, `core/SkillState.java` по разделу 5.

**Критерии приёмки:**

1. `Organ.SHADOW_GLAND.element()` = `SHADOW`, `THUNDER_FANG.element()` = `STORM`.
2. `Skill.of` для каждой пары орган × {`ARMS`, `LEGS`, `BACK`} даёт навык из таблицы 5.4 с его энергией и перезарядкой; для `CORE` пусто.
3. `Body` без органов: `elements()` = `{STEEL}`, `skill(ARMS)` пусто, `resistance(SHADOW)` = 1.0.
4. `Body` с железой в `ARMS` и клыком в `CORE`: `elements()` = `{STEEL, SHADOW, STORM}`, `skill(ARMS)` = `SHADOW_BOLT`, `skill(CORE)` пусто, `resistance(STORM)` = 0.5, `resistance(SHADOW)` = 1.0, `resistance(STEEL)` = 1.0.
5. `keepOrSteel(SHADOW)` у тела без железы = `STEEL`; у тела с железой = `SHADOW`.
6. `SkillState.FULL.afterUse(SHADOW_VEIL)` → энергия 65, `cooldown(SHADOW_VEIL)` = 400, `canUse(SHADOW_VEIL)` = false.
7. С энергией 10 `canUse(SHADOW_BOLT)` = false, `afterUse` возвращает то же состояние.
8. 20 `tick` после траты 20 энергии → энергия +4; энергия не выше 100.
9. 60 `tick` после `SHADOW_BOLT` → `cooldown` 0, ключа в карте нет, `canUse` снова true.

**Тесты:** `SkillTest`, `BodyTest`, `SkillStateTest` по критериям 1–9.

**Не делать:** игровой код, семейства, ёмкость, редкость.

---

### Слайс 4 — предметы органов

**Что собрать:** `item/OrganItem.java`; в `ModItems` поля `SHADOW_GLAND`, `THUNDER_FANG` по разделу 6.1; модели и текстуры органов; ключи `item.threshold_gates.shadow_gland`, `.lore`, `thunder_fang`, `.lore`, `item.threshold_gates.organ.element`, `element.threshold_gates.*`. Удалить `debug_shard`: поле, модель, текстуру, ключ, запись в `LangTest`.

**Критерии приёмки:**

1. `LangTest` знает `shadow_gland` и `thunder_fang` и падает без их ключей; `debug_shard` в нём нет.
2. На телефоне: `/give @s threshold_gates:shadow_gland` выдаёт «Теневая железа» со своей текстурой; подсказка: «Стихия: Тень» фиолетовым, ниже серым курсивом строка лора.
3. На телефоне: то же для `thunder_fang`, «Стихия: Гроза» жёлтым.
4. На телефоне: оба органа во вкладке «Ингредиенты» творческого режима, стопка до 16.
5. На телефоне: `/give @s threshold_gates:debug_shard` отвечает, что предмета нет.

**Тесты:** `LangTest`.

**Не делать:** экран анатомии, состояние игрока, яйца призыва.

---

### Слайс 5 — состояние игрока и экран анатомии

**Что собрать:** `body/BodyData.java`, `registry/ModAttachments.java`, `registry/ModMenus.java`, `menu/AnatomyMenu.java`, `network/OpenAnatomyPayload.java`, `registry/ModNetworking.java`, `client/ModKeys.java` (только клавиша `K`), `client/AnatomyScreen.java`, `textures/gui/anatomy.png`, ключи `slot.*`, `container.threshold_gates.anatomy`, `key.categories.threshold_gates`, `key.threshold_gates.anatomy`, `message.threshold_gates.system.graft`; `system/SystemMessages.java` для сообщения `graft`; сброс после смерти по разделу 6.2.

**Критерии приёмки:**

1. На телефоне: кнопка `K` открывает экран «Анатомия»: силуэт, 4 подписанные ячейки, инвентарь снизу.
2. На телефоне: железа перетаскивается в любую из 4 ячеек; в ячейку кладётся только 1 штука; обычный предмет (земля) в ячейку не кладётся.
3. На телефоне: под занятой ячейкой свечение цвета стихии органа.
4. На телефоне: после приживления в чате «[Система] Орган прижился: Теневая железа, слот «Руки».».
5. На телефоне: выход из мира и вход обратно — орган на месте; `/data get entity @s "fabric:attachments"."threshold_gates:body"` показывает его в слоте.
6. На телефоне: после `/kill` органы в теле остаются, из тела ничего не выпадает.
7. На телефоне: орган вынимается обратно в инвентарь и пропадает из тела.

**Тесты:** `LangTest` для новых ключей не нужен (ключи не предметов); тестов ядра нет. Проверка на телефоне.

**Не делать:** переключение стихии, HUD, навыки, штрафы за перестановку.

---

### Слайс 6 — переключение стихии, Нагрузка в игре, HUD

**Что собрать:** `body/BodyTicker.java` (шаг 1 раздела 6.2), `body/BodyActions.switchElement`, `network/SwitchElementPayload.java`, клавиша `Z` в `ModKeys`, `client/BodyHud.java`, `combat/ElementParticles.burst`, ключи `key.threshold_gates.switch_element`, `message.threshold_gates.system.rejection`. `keepOrSteel` при изменении ячеек анатомии.

**Критерии приёмки:**

1. На телефоне: в левом верхнем углу «Сталь» серебром, полная синяя полоска энергии, пустая красная полоска Нагрузки.
2. На телефоне: без органов `Z` ничего не меняет, Нагрузка 0.
3. На телефоне: с железой и клыком `Z` переключает Сталь → Гроза → Тень → Сталь; цвет надписи меняется; вокруг игрока всплеск частиц цвета новой стихии; полоска Нагрузки растёт на четверть.
4. На телефоне: без нажатий Нагрузка с четверти уходит в 0 примерно за 5 секунд.
5. На телефоне: 4 быстрых нажатия `Z` → значки Слабости и Медлительности, сообщение «[Система] Тело отторгает стихии…», полоска мигает, `Z` не работает; примерно через 8 секунд эффекты и мигание пропадают, Нагрузка 0, `Z` снова работает.
6. На телефоне: выбрана Тень, железу вынули из тела → надпись «Сталь», Нагрузка не выросла.
7. На телефоне: выбранная стихия сохраняется после выхода из мира; после `/kill` Нагрузка 0, стихия та же.

**Тесты:** тестов ядра нет (ядро покрыто в слайсах 1–3). Проверка на телефоне.

**Не делать:** навыки, урон со стихией.

---

### Слайс 7 — Теневой ползун

**Что собрать:** `combat/ElementalMob.java`, `entity/ShadowCrawler.java`, `registry/ModEntities.java` (ползун), `client/render/ShadowCrawlerRenderer.java`, `textures/entity/shadow_crawler.png`, яйцо `shadow_crawler_spawn_egg`, таблица дропа, появление в биомах, ключи `entity.threshold_gates.shadow_crawler`, `item.threshold_gates.shadow_crawler_spawn_egg`.

**Критерии приёмки:**

1. `LangTest` знает `shadow_crawler` и `shadow_crawler_spawn_egg`.
2. На телефоне: яйцо во вкладке «Яйца призыва», из него появляется паук с тёмно-фиолетовой текстурой и названием «Теневой ползун».
3. На телефоне: `/data get entity @n[type=threshold_gates:shadow_crawler] Health` = 16.
4. На телефоне, в режиме выживания: ползун нападает; из 10 убитых ползунов железа выпала хотя бы 2 раза; нить и паучий глаз не выпадают.
5. На телефоне: ползун, убитый командой `/kill`, железу не роняет.
6. На телефоне: ночью в обычном мире ползуны появляются сами (за 5 минут игры ночью хотя бы один).

**Тесты:** `LangTest`.

**Не делать:** урон со стихией (ползун пока бьёт обычным уроном), предупреждения об атаке.

---

### Слайс 8 — урон со стихией

**Что собрать:** `mixin/LivingEntityMixin.java`, `threshold_gates.mixins.json`, строка `mixins` в `fabric.mod.json`, `combat/ElementalDamage.java`, `registry/ModDamageTypes.java`, `data/.../damage_type/shadow.json`, `storm.json`, ключи `death.attack.threshold_gates.*`, вспышка слабости (`ElementParticles.burst`), счётчик `damageDealt`.

**Критерии приёмки** (каждый замер на свежем ползуне, 16 HP):

1. На телефоне: `/damage @n[type=threshold_gates:shadow_crawler] 4 threshold_gates:storm` → Health 10 и вспышка жёлтых частиц.
2. На телефоне: `/damage … 4 threshold_gates:shadow` → Health 14, вспышки нет.
3. На телефоне: `/damage … 4 minecraft:generic` → Health 12.
4. На телефоне: зомби без брони (`/summon minecraft:zombie`), `/damage @n[type=zombie] 4 threshold_gates:storm` → Health 16 (20 − 4).
5. На телефоне, в режиме выживания с полным здоровьем и без брони: игрок с клыком в ядре, `/damage @s 4 threshold_gates:storm` → Health игрока 18; с клыком в руках → 16.
6. На телефоне: выбрана Гроза, удар по ползуну пустой рукой с полностью заряженной атакой, не в прыжке (1 урона) → Health 14.5; в `damageDealt` у `STORM` прибавилось 1.5.
7. На телефоне: смерть от `/damage @s 40 threshold_gates:shadow` пишет в чат русское сообщение о смерти, а не ключ.
8. В `latest.log` нет ошибок применения миксина.

**Тесты:** тестов ядра нет (множители покрыты в слайсе 1). Проверка на телефоне.

**Не делать:** навыки, своих мобов кроме ползуна, свой интерфейс для цифр урона.

---

### Слайс 9 — Грозовой клык и предупреждение об атаке

**Что собрать:** `entity/StormFang.java`, `entity/TelegraphedAttackGoal.java`, `client/render/StormFangRenderer.java`, текстура, яйцо `storm_fang_spawn_egg`, таблица дропа, появление в биомах, `ElementParticles.aura`, ключи сущности и яйца.

**Критерии приёмки:**

1. `LangTest` знает `storm_fang` и `storm_fang_spawn_egg`.
2. На телефоне: из яйца появляется волк с серо-жёлтой текстурой, «Грозовой клык», Health 30.
3. На телефоне: кость его не приручает; в выживании он нападает на игрока.
4. На телефоне: перед каждым укусом у клыка видны жёлтые частицы примерно полсекунды; если в это время отойти, укуса нет.
5. На телефоне: `/damage @n[type=threshold_gates:storm_fang] 4 threshold_gates:shadow` → Health 24.
6. На телефоне: из 10 убитых игроком клыков клык-орган выпал хотя бы 1 раз; от `/kill` не выпадает.
7. На телефоне: ночью в обычном мире клыки появляются сами (за 10 минут игры ночью хотя бы один).

**Тесты:** `LangTest`.

**Не делать:** Стража, навыки.

---

### Слайс 10 — навыки Теневой железы

**Что собрать:** `skill/SkillEffects.java` (3 навыка железы), `body/BodyActions.useSkill`, `network/UseSkillPayload.java`, клавиши `R`, `V`, `G`, `ElementParticles.trail`, ключи `key.threshold_gates.skill_*`, `message.threshold_gates.actionbar.*`. В `BodyTicker` расход `skills.tick()` уже есть со слайса 6.

**Критерии приёмки:**

1. На телефоне: пустой слот рук, `R` → «В этом слоте нет навыка».
2. На телефоне: железа в руках, `R` по ползуну в 10 блоках → фиолетовый след, Health ползуна 13 (6 × 0.5); энергия −20; повторный `R` сразу → «Перезарядка: 3 с».
3. На телефоне: тот же `R` по зомби без брони → Health 14.
4. На телефоне: железа в ногах, `V` → перенос примерно на 6 блоков вперёд, частицы в начале и в конце; лицом в стену вплотную → «Нет цели», энергия не тратится.
5. На телефоне: железа в спине, `G` → Невидимость 4 с; ползун, который гнался за игроком, теряет цель.
6. На телефоне: энергия кончается после нескольких навыков → «Не хватает энергии»; полоска энергии восстанавливается примерно на 4 % в секунду.
7. На телефоне: железа в ядре, `R`, `V`, `G` ничего не применяют.

**Тесты:** тестов ядра нет (числа навыков покрыты в слайсе 3). Проверка на телефоне.

**Не делать:** навыки клыка, Стража.

---

### Слайс 11 — навыки Громового клыка

**Что собрать:** 3 навыка клыка в `SkillEffects`, Ореол в `BodyTicker` (шаг 2 раздела 6.2), `ElementParticles.ring`.

**Критерии приёмки:**

1. На телефоне: клык в руках, `R` по ползуну в 8 блоках → молния в цель без огня, Health ползуна 5.5 (16 − 7 × 1.5); `R` в пустое небо → «Нет цели», энергия не тратится.
2. На телефоне: клык в ногах, `V` → значок Скорости II на 5 с.
3. На телефоне: клык в спине, `G` рядом с двумя ползунами → жёлтые кольца 4 с; каждый ползун умирает (8 × 2 × 1.5 = 24 > 16); сам игрок урона не получает.
4. На телефоне: после `/kill` во время Ореола он не продолжается после возрождения.

**Тесты:** тестов ядра нет. Проверка на телефоне.

**Не делать:** Стража, новые навыки.

---

### Слайс 12 — Страж врат

**Что собрать:** `entity/GateWarden.java`, `client/render/GateWardenRenderer.java`, текстура, яйцо `gate_warden_spawn_egg`, полоса босса, смена слабости, частицы, сообщения `warden_awake`, `warden_weakness`, `warden_defeated`, ключи сущности, яйца, `gate_warden.bar`.

**Критерии приёмки:**

1. `LangTest` знает `gate_warden` и `gate_warden_spawn_egg`.
2. На телефоне: из яйца появляется голем с тёмной текстурой, «Страж врат», Health 150; сообщение «[Система] Страж врат пробудился…».
3. На телефоне: полоса босса «Страж врат · слабость: Гроза», жёлтая; вокруг Стража жёлтые частицы.
4. На телефоне: через 20 секунд полоса «слабость: Тень», фиолетовая, частицы фиолетовые, сообщение «[Система] Слабость Стража: Тень.»; дальше смена каждые 20 секунд.
5. На телефоне: при слабости Гроза `/damage @n[type=threshold_gates:gate_warden] 10 threshold_gates:storm` → Health 135; `… shadow` → Health ниже на 5.
6. На телефоне: перед ударом Стража полсекунды частицы цвета его стихии; Страж не трогает зомби и нападает на игрока в выживании.
7. На телефоне: смерть Стража → «[Система] Страж врат повержен. Порог пройден.»; органов не выпадает.

**Тесты:** `LangTest`.

**Не делать:** модуль подчинения стихии, шкалу стойкости, Врата.

---

### Слайс 13 — сообщения Системы и проверка версии

**Что собрать:** сообщение `first_organ` (шаг 3 `BodyTicker`), проверка, что все ключи раздела 8 есть в `ru_ru.json` и используются.

**Критерии приёмки:**

1. На телефоне: в новом мире первый подобранный орган → «[Система] Получен орган: …»; второй орган сообщения не даёт; после выхода и входа сообщение не повторяется.
2. В `ru_ru.json` нет ключей, которых нет в разделе 8, и наоборот.
3. На телефоне, критерий готовности v0.1, в режиме выживания на обычной сложности, без команд: убить ползуна, получить железу, привить, применить навык; добыть клык; победить Стража, переключая стихии; за бой со Стражем не больше 2 смертей.
4. В `latest.log` после этой проверки нет `ERROR` и исключений, где упоминается `threshold`.

**Тесты:** `LangTest` сверяет полный список ключей раздела 8.

**Не делать:** механики v0.2.

---

## 16. Ревью после каждого слайса

Отдельным заходом, после того как слайс готов и CI зелёный. Задача захода — искать проблемы, а не хвалить написанное.

Чек-лист:

1. В `core/` нет импортов Minecraft, Mojang, Fabric. `CorePurityTest` на месте и не ослаблен.
2. Клиентский код только в `src/client/`.
3. ID, пакеты, ключи локализации совпадают с разделами 4 и 8. Литерал `"threshold_gates"` в Java только в `MOD_ID`.
4. Тексты для игрока только в `ru_ru.json`, у каждого зарегистрированного объекта есть ключ.
5. Тесты проверяют критерии приёмки и числа из раздела 5, а не повторяют реализацию.
6. Обработки ошибок, которые не бывают, нет.
7. Мёртвого кода нет: неиспользуемые классы, ресурсы, ключи локализации.
8. Файлов и абстракций сверх раздела 3 не появилось.
9. В коммитах нет строк соавторства.
10. Миксин один, он только вызывает `ElementalDamage`. Решения о сети принимает сервер, клиент только шлёт нажатия.

Находки правятся в том же заходе, потом CI прогоняется заново.

---

## 17. Отложено на после v0.1

Решено владельцем, в v0.1 не делается:

- У ванильных мобов будут свои подходящие стихии. В v0.1 все они Сталь.
- Отторжение получит свои эффекты вместо ванильных Слабости и Медлительности.
- Семейство, ёмкость и редкость органа — в своих версиях плана (v0.4, v0.2, v0.3).
- Перехват, Отзвуки, мастерство — v0.2.
- Цифры урона над целью — когда понадобятся для читаемости боя.
- Перекрашенные ванильные текстуры заменяются своими в v0.9; до публикации проверить чистоту ассетов.
