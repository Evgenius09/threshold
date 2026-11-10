# Slice 1: Elements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the pure `Element` enum (colors, damage multiplier table, element cycling) to `core/`, covered by JUnit tests.

**Architecture:** One enum in the pure core package, no Minecraft imports. One JUnit test class encodes acceptance criteria 1–6 of slice 1 with the exact numbers from tech.md 5.1. The version goes to `0.1.1` in the first commit of the slice.

**Tech Stack:** Java 21, JUnit 5, Gradle in GitHub Actions only.

**Spec:** `tech.md` v2, sections 5.1 (contract), 9 (tests), 12 (commits), 15 "Слайс 1" (criteria).

## Global Constraints

- `./gradlew` is never run locally. Verification = `git push`, then `gh run watch` on the `build` run for HEAD.
- `core/` imports nothing from `net.minecraft.*`, `com.mojang.*`, `net.fabricmc.*` (`CorePurityTest` enforces it).
- Indentation: tabs (matches existing sources).
- Commits: `type(scope): summary`, imperative, lowercase, no period, ≤50 chars. **No `Co-Authored-By` or any attribution lines.**
- Code, comments, commits in English. Comments explain why, not what.
- No new files beyond: `core/Element.java`, `ElementTest.java`. No locale keys in this slice.
- Version in `gradle.properties`: `version=0.1.1`.

## Review Focus

1. `next` must not depend on `Set` iteration order: walk the enum in ordinal order, not the set. Tests use `Set.of(...)`, whose iteration order is unspecified, so an order-dependent implementation fails randomly. Covered by criteria tests using `Set.of`.
2. `next` when `current` is not in `available` (criterion 6): search starts after `current`'s ordinal anyway. Covered by test `nextSkipsCurrentMissingFromSet`.
3. `next` with only `STEEL` available returns `STEEL` without looping forever. Covered by `nextWithSteelOnlyStaysSteel`.
4. `multiplier` with `STEEL` on either side is exactly 1.0 (no accidental 0.5 for STEEL vs STEEL). Covered by the full 9-cell table test.
5. Colors are RGB without alpha (`0xC0C8D0`, not `0xFFC0C8D0`). Covered by `colorsMatchSpec`.

---

### Task 1: `Element` enum with tests

**Files:**
- Modify: `gradle.properties` (`version=0.0.1` → `version=0.1.1`)
- Create: `src/main/java/io/github/evgenius09/threshold/core/Element.java`
- Test: `src/test/java/io/github/evgenius09/threshold/ElementTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces (later slices rely on these exact names):
  ```java
  package io.github.evgenius09.threshold.core;
  public enum Element {
      STEEL(0xC0C8D0), STORM(0xFFE04A), SHADOW(0x8A3FD1);
      public int color();
      public static double multiplier(Element attack, Element defender);
      public static Element next(Element current, Set<Element> available);
  }
  ```

- [ ] **Step 1: Bump version and commit**

Set `version=0.1.1` in `gradle.properties`.

```bash
git add gradle.properties
git commit -m "chore(ci): bump version to 0.1.1"
```

- [ ] **Step 2: Write the failing test**

`ElementTest` in package `io.github.evgenius09.threshold`, imports `io.github.evgenius09.threshold.core.Element` and static `Element.*` constants as convenient. Compare doubles with delta `1e-9`.

```java
@Test void multiplierMatchesTable() {
	// rows: attack STEEL, STORM, SHADOW; columns: defender STEEL, STORM, SHADOW (tech.md 5.1)
	double[][] expected = {
		{1.0, 1.0, 1.0},
		{1.0, 0.5, 1.5},
		{1.0, 1.5, 0.5},
	};
	Element[] order = {STEEL, STORM, SHADOW};
	for (int a = 0; a < 3; a++)
		for (int d = 0; d < 3; d++)
			assertEquals(expected[a][d], Element.multiplier(order[a], order[d]), 1e-9,
				order[a] + " -> " + order[d]);
}

@Test void colorsMatchSpec() {
	assertEquals(0xC0C8D0, STEEL.color());
	assertEquals(0xFFE04A, STORM.color());
	assertEquals(0x8A3FD1, SHADOW.color());
}

@Test void nextCyclesThroughAll() {
	Set<Element> all = Set.of(STEEL, STORM, SHADOW);
	assertEquals(STORM, Element.next(STEEL, all));
	assertEquals(SHADOW, Element.next(STORM, all));
	assertEquals(STEEL, Element.next(SHADOW, all));
}

@Test void nextSkipsMissingElement() {
	Set<Element> steelShadow = Set.of(STEEL, SHADOW);
	assertEquals(SHADOW, Element.next(STEEL, steelShadow));
	assertEquals(STEEL, Element.next(SHADOW, steelShadow));
}

@Test void nextWithSteelOnlyStaysSteel() {
	assertEquals(STEEL, Element.next(STEEL, Set.of(STEEL)));
}

@Test void nextSkipsCurrentMissingFromSet() {
	assertEquals(SHADOW, Element.next(STORM, Set.of(STEEL, SHADOW)));
}
```

A one-line class comment: the numbers come from tech.md 5.1, so a test failure means the code drifted from the spec.

- [ ] **Step 3: Push and confirm the test fails in CI**

```bash
git add src/test/java/io/github/evgenius09/threshold/ElementTest.java
git commit -m "test(core): cover element multipliers and cycling"
git push
gh run watch "$(gh run list --commit "$(git rev-parse HEAD)" --workflow build --limit 1 --json databaseId --jq '.[0].databaseId')" --exit-status
```

Expected: run FAILS at `compileTestJava` with `cannot find symbol ... Element`. (Compile failure is the expected red here: the type does not exist yet.)

- [ ] **Step 4: Implement `core/Element.java`**

Enum with constructor `Element(int color)` and the three methods from the Interfaces block.

- `multiplier`: `1.0` if either side is `STEEL`; `0.5` if `attack == defender`; otherwise `1.5`.
- `next`: for `i = 1..values().length`, candidate = `values()[(current.ordinal() + i) % length]`; return the first candidate contained in `available` (the last step, `i = length`, is `current` itself). Per spec `available` always contains `STEEL`, so the loop always returns; after it, `throw new IllegalArgumentException("available must contain STEEL")` only to satisfy the compiler.

Javadoc-free; one comment on `multiplier` stating why STEEL is neutral (plan: Сталь нейтральная ×1).

- [ ] **Step 5: Push and confirm CI is green**

```bash
git add src/main/java/io/github/evgenius09/threshold/core/Element.java
git commit -m "feat(core): add element multipliers"
git push
gh run watch "$(gh run list --commit "$(git rev-parse HEAD)" --workflow build --limit 1 --json databaseId --jq '.[0].databaseId')" --exit-status
```

Expected: run PASSES; test report shows 6 tests in `ElementTest` passed, `CorePurityTest` and `LangTest` still pass. Artifact `threshold-jar` contains `threshold_gates-0.1.1.jar`.

- [ ] **Step 6: Definition of Done check (tech.md 15)**

Verify: CI green; only the three files above changed since the plan commit (`git diff --stat HEAD~3..HEAD`); no attribution lines in `git log -4 --format=%B`; `tech.md` untouched. No phone check in this slice (pure core).
