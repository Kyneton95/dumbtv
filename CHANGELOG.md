# Changelog

Changes in this fork relative to upstream dumbtv home. Upstream baseline:
[dumbtv home 1.1](https://github.com/humblemuzzu/dumbtv).

## 1.7 — 2026-10-23

**Sharper, brighter daylight.**

- Replaced the soft sea/sky daylight scene with a layered mountain landscape:
  crisp 4×-antialiased ridgelines, a defined sun, cloud and open blue sky. The
  old scene's smooth sea read as blurry on a 4K panel.
- Removed a ridge-clipping artefact that drew a flat horizontal line through the
  first ridge.
- Lightened the day-only scrim (left 55%, top 50%, vignette 44%) so the scene no
  longer looks like dusk; text shadows and the darker chips keep the UI legible.
- Renamed the scene from Daylight coast to Daylight. The auto hours are unchanged.
- APK: 611,198 bytes.

## 1.6 — 2026-10-23

**Auto scene hours.**

- Daylight 09:00–17:00.
- Dunes 05:00–09:00 and 17:00–20:00.
- Moonlit coast 20:00–00:00.
- Aurora 00:00–05:00.
- No asset or size change; only the `Auto` clock mapping in `Config.autoScene()`.

## 1.5 — 2026-10-23

**Daylight background, and `Auto` now follows a full day.**

- Added a fourth scene, **Daylight coast**: blue sky, cloud, sun and open sea, no
  stars or moon.
- `Auto` now maps 06:00–17:00 to Daylight coast, 17:00–20:00 to Dunes at dusk and
  the rest to Aurora. Previously 06:00–17:00 used the moonlit coast, so a 4pm
  screen showed a starry night.
- The day scene gets a stronger left/top scrim; the date, row labels and idle
  chips also gained contrast so white type holds over a bright sky.
- APK: 627,582 bytes with four scenes; SHA-256 pinned in `launcher/version.env`.

## 1.4 — 2026-10-23

**Merged upstream 1.2 (95 commits) and kept the fork's look.**

- Upstream launcher 1.2 features: grid layout, built-in HDMI input screen
  (`Input`/`TvView`), adaptive solid/tint backgrounds (`FocusColor`), the new
  `Open inputs with` option, host-side launcher tests, and the cover-based Fire OS
  8 HOME fix.
- The fork's three scenes and visual layer are preserved on top: Aurora, Dunes at
  dusk and Moonlit coast, `Auto`, layered `Shade` scrim, 64 sp clock, larger cards,
  softer focus and the grouped Options screen.
- HOME now tries the existing Home task first with `AppTask.moveToFront` (no extra
  permission), retries briefly on the first press after an install, and falls back
  to upstream's cover/alarm path. Tested on AFTKRT / Fire OS 8.1.8.2: first press
  and repeated/rapid presses all return in about a second.
- APK: 488,247 bytes with the three scenes; SHA-256 pinned in `launcher/version.env`.

## 1.3 — 2026-10-22

**Fire OS 8 HOME takeover fixed (interim; superseded by 1.4's combined path).**

- Android 11 refuses the Guard's alarm-fired activity start from the background
  (`callingUidHasAnyVisibleWindow: false`), which left HOME on Amazon's launcher.
- Guard now moves its existing Home task to the front with `moveTaskToFront`
  instead. It remembers the Home task ID and otherwise selects the task whose
  base activity is `tv.dumbtv.home/.Home`; it never moves an arbitrary task.
- No new permission. The alarm remains only as the cold-start fallback before a
  Home task exists.
- Tested on AFTKRT (Fire TV Stick 4K Max, Fire OS 8.1.8.2 / Android 11): HOME
  returns in about a second, including repeated and rapid presses.
- Updated `docs/GOTCHAS.md`, `catalog/devices/fire-os-8-aftkm.yaml` and
  `launcher/README.md`.
- APK: 480,053 bytes; SHA-256 pinned in `launcher/version.env`.

## 1.2 — 2026-10-22

**Visual refresh.**

- Three original 1920×1080 scenes bundled in the APK: Aurora, Dunes at dusk and
  Moonlit coast. `Auto` follows the time of day; scenes crossfade; `Next
  background` steps through them.
- New layered scrim (left, top, bottom, vignette) replaces the flat dim.
- 64 sp light clock, larger type and cards, 6.7% overscan margins, softer
  multi-cue focus (scale + outline + shadow), redesigned Options screen with
  grouped sections.
- New `Shade` view; wallpapers packed into the APK with `aapt2 -A`.
- APK about 480 KB including the three scenes.

**Artwork pass.**

- Ridgelines are drawn at 4× and box-filtered; mountain profiles are smoothed
  along their length.
- Removed the hard crest/rim lines, which were the visible staircase.
- Lossless PNG source, WebP q95, and a 0.35 px soften for the 1080p→4K upscale.

**Build and install.**

- `build.sh` packs `assets/`; falls back to `jar` when `zip` is unavailable.
- `install.sh` accepts newer adb's `Install command complete` output.
- Docs cover the Fire OS 8 `Android/data` adb block and the built-in scenes.

## 1.1 — upstream baseline

- No fork changes. See the upstream repository.
