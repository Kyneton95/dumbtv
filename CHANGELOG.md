# Changelog

Changes in this fork relative to upstream dumbtv home. Upstream baseline:
[dumbtv home 1.1](https://github.com/humblemuzzu/dumbtv).

## 1.3 — 2026-10-22

**Fire OS 8 HOME takeover fixed.**

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
