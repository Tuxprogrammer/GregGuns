## What and why

<!-- One or two sentences. Link the roadmap item, e.g. "M1 · Combat core: pistol reload". -->

**Roadmap:**
**Spec sheets:** <!-- e.g. docs/techguns-spec/02-guns/per-gun-behaviour.md#… -->
**Design records:** <!-- docs/design/NNNN-… if any -->

## Deviations from the spec

<!-- What behaves differently from Techguns, and why (tier gate, EU, GT replacement, bug fix). "None" if none. -->

## Inherited bugs avoided

<!-- BUGS.md IDs this change avoids, and the test that proves each one. -->

## Definition of done

- [ ] Implements the spec sheet, with deviations recorded
- [ ] `./gradlew spotlessApply build` passes; unit tests cover the new logic and the relevant BUGS.md regressions
- [ ] `runServer` boots cleanly; the dev self-check passes
- [ ] An in-game scenario exists (world-touching features); multiplayer checked (synced features)
- [ ] Recipes are tier-gated per D1 and visible in NEI; no duplicated GT materials
- [ ] Every string is a lang key; assets are original, with provenance in `docs/assets/CREDITS.md`
- [ ] Javadoc and `// Spec:` comments are in place; no commented-out code
- [ ] The roadmap box is ticked; docs are updated

## Test evidence

<!-- Test output, scenario results, and screenshots for visual work. -->
