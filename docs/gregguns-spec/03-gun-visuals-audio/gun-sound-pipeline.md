# Gun sounds in the pack's sound pipeline

- **Techguns baseline:** [sounds.md](../../techguns-spec/03-gun-visuals-audio/sounds.md), [rendering-framework.md §14](../../techguns-spec/03-gun-visuals-audio/rendering-framework.md#14-sound-playback-mechanics-summary), [GVA-23](../../techguns-spec/03-gun-visuals-audio/index.md#suspected-bugs) (stereo gun sounds ignore distance)
- **Design records:** [0006 art direction](../../design/0006-art-direction.md) (positional sounds are mono)
- **Pack mods:** Hodgepodge (`hodgepodge`, 2.7.215), Extreme Sound Muffler (`extremesoundmuffler`, 1.1.1, client only)
- **Roadmap:** M1 (sound framework), M2 (ballistic arsenal sounds)

## What changes from Techguns

Techguns has 13 stereo gun files out of 185, and its spec reasons that they play flat (GVA-23). The pack changes three things around that.

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Stereo files play flat, with no distance or direction (reasoned, not observed) | All positional sounds are mono (design 0006). The pack's stereo repair is a safety net, not a plan. | Hodgepodge can fix stereo, but only with a fallback, and it costs memory. |
| Every shot starts a new 1 to 3 s sound | Same sounds, but the longest sound times the fire rate must fit a channel budget | The pack allows 64 normal channels in total. |
| Event names chosen freely | Event names must not match the muffler's blacklist | Matching names can never be muted by the player. |
| No player-side mute | Each weapon has its own fire event, so players can mute one gun | The muffler works per event name. |

## Integration points

### Hodgepodge: stereo sounds are repaired in the pack

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/hodgepodge.cfg`, section `sound`: `downmixStereoSounds` = `true`, `spatializeStereoSounds` = `true`, `downmixExclusions` = `button, click, /gui, menu, typing, page`.
- **Source:** [DownmixingOggCodec.java#L18-L34](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.215/src/main/java/com/mitchej123/hodgepodge/client/sound/DownmixingOggCodec.java#L18-L34) explains that OpenAL only spatialises mono buffers, so a stereo sound plays centred whatever its position, and that the downmix is the fallback for the Java 8 build. `SpatializeSupport` is the primary fix and needs lwjgl3ify and the `AL_SOFT_source_spatialize` extension. The config keys are declared in [SoundConfig.java#L82-L99](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.215/src/main/java/com/mitchej123/hodgepodge/config/SoundConfig.java#L82-L99).
- **What it means:** under the pack, Techguns' 13 stereo files would most likely be positioned correctly. Stereo-spatialised playback uses about twice the buffer memory of mono (config comment). Whether it works depends on the player's audio device and Java build, which GregGuns cannot control.
- **GregGuns does:** ships mono for every positional sound, as design 0006 requires. It does not name any gun sound path that matches `downmixExclusions` (`button`, `click`, `/gui`, `menu`, `typing`, `page`), because those are kept stereo and unpositioned.
- **Pack-side change:** none.

### Hodgepodge: the channel cap

- **Pack config:** `hodgepodge.cfg`, section `sound`, key `numberNormalChannels` = `64`. The same section sets `defaultAttenuationModel` = `ATTENUATION_ROLLOFF`, `defaultRolloffFactor` = `0.03` and `defaultFadeDistance` = `1000.0`, which apply when a sound does not specify its own.
- **Source:** [SoundConfig.java#L22](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.215/src/main/java/com/mitchej123/hodgepodge/config/SoundConfig.java#L22) and [L163-L169](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.215/src/main/java/com/mitchej123/hodgepodge/config/SoundConfig.java#L163-L169), where the values are pushed into `SoundSystemConfig`.
- **What it means:** 64 non-streaming sounds can play at once, for every source in the world. As an illustration, an automatic with a 1.2 s fire sound at 10 shots per second (made-up rate) would hold 12 channels alone. Four such shooters take most of the pool, and further sounds, including other mods' machine hums, are dropped.
- **GregGuns does:** budgets channels per weapon: `ceil(longest fire variant in seconds × shots per second)`. Automatics with a loop delay use the start, loop, stop scheme already in the Techguns spec ([sounds.md, common trigger rules](../../techguns-spec/03-gun-visuals-audio/sounds.md#common-trigger-rules-from-genericgun-area-02)) so the loop file is the only one that overlaps. Explosion sounds stay one-shot.
- **Pack-side change:** none required. A pack maintainer could raise `numberNormalChannels` to 128 if shooters compete with machine sounds. Record it as optional.

### Extreme Sound Muffler: mute by event name, with a substring blacklist

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/extremesoundmuffler.cfg`, key `forbiddenSounds` = `ui.`, `music.`, `ambient.`.
- **Source:** [SoundMixin.java#L24-L33](https://github.com/Lyfts/ExtremeSoundMuffler-Legacy/blob/1.1.1/src/main/java/com/leobeliik/extremesoundmuffler/mixins/minecraft/SoundMixin.java#L24-L33) tests `String.contains` of each forbidden entry against the full `domain:path` of the sound. [L35-L63](https://github.com/Lyfts/ExtremeSoundMuffler-Legacy/blob/1.1.1/src/main/java/com/leobeliik/extremesoundmuffler/mixins/minecraft/SoundMixin.java#L35-L63) scales the volume of every other sound by the player's per-event setting.
- **What it means:** `String.contains` is a substring test, not a prefix test. An event such as `gregguns:gui.turret_click` contains `ui.` and can never be muffled, and `gregguns:ambient.hum` is excluded by design. All other events can be turned down individually by the player. The muffler only ever lowers volume, so it cannot make a warning louder.
- **GregGuns does:** names gun, projectile and explosion events `guns.<weapon>.<action>`, `effects.<name>` and so on. A sound that must stay audible regardless of the player's muffler (a safety alarm) may use a matching name on purpose, and the sound table marks it as such.
- **Pack-side change:** none.

## Requirements

- **GGS-03-07.** Every positional sound file in `assets/gregguns/sounds/` is mono. UI and ambience sounds may be stereo.
- **GGS-03-08.** No gun, projectile, explosion or FX sound event name contains any `forbiddenSounds` entry from the shipped `extremesoundmuffler.cfg` (`ui.`, `music.`, `ambient.`) as a substring.
- **GGS-03-09.** Each weapon's fire sound is its own event, so a player can mute one weapon.
- **GGS-03-10.** For each weapon, `ceil(longest fire variant length × maximum shots per second)` is at most 8 channels. An automatic that would exceed it needs a start, loop, stop scheme.
- **GGS-03-11.** No gun sound path contains an entry of `downmixExclusions`.

## Tests

- GGS-03-07: unit test that reads each `.ogg` header under `assets/gregguns/sounds/` and fails on stereo outside the UI and ambience folders.
- GGS-03-08, GGS-03-09, GGS-03-11: unit test over `sounds.json` keys and file paths against the three lists. The lists are copied into the test with a comment pointing at the pack files.
- GGS-03-10: unit test that joins the sound table with gun stats from the registry.
- Audibility at range with the pack's default attenuation values: manual `runClient` check, see Open questions.

## Open questions

- **Attenuation.** The Techguns spec says a volume 4.0 sound carries 64 blocks under vanilla linear attenuation. The pack sets a default rolloff model (`ATTENUATION_ROLLOFF`, 0.03). Whether that default applies to sounds played through the vanilla `ISound` path was not traced. Check in `runClient` at 30 and 60 blocks. If the range differs, record it in a design record on audio ranges.
- **Channel budget of 8** is a proposal and has not been measured. It should be tuned in M2 with several shooters on screen.
