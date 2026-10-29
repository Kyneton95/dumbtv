# Changelog

Changes in this fork relative to upstream dumbtv home. Upstream baseline:
[dumbtv home 1.1](https://github.com/humblemuzzu/dumbtv).

## 1.14 — 2026-10-29

**Sync with upstream main, verified on Fire OS 8 (AFTKRT).**

- Merged upstream main beyond 1.2: Afterglow tile style, brand colors,
  memory meter, recent-apps tracking, new device catalog entries (35 valid),
  blocklist updates and launcher fixes.
- Full pass verified on an AFTKRT running Fire OS 8.1.8.2: debloat (34 Amazon
  packages disabled), DNS filter (Amazon telemetry and update hosts blocked,
  YouTube/Prime Video/Appstore reachable), launcher 1.13 bound as HOME;
  `verify.sh` exits 0.
- Fork features retained: four built-in scenes (Daylight, Aurora, Dunes at
  dusk, Moonlit coast), cinematic look, fast Fire OS 8 HOME takeover path.
- README and FORK.md updated to reflect the sync; homepage screenshot replaced
  with a live capture from the test TV.
- APK: 717,696 bytes.

## 1.13 — 2026-10-23

**Review pass: rendering, HOME handoff and Options.**

- Shade now caches its four gradients and uses a separate paint for the flat dim,
  so the dim colour's alpha cannot leak into the next gradient. Removed identical
  conditional branches and the unsupported contrast claim.
- Wallpaper crossfades are replaced by the final drawable when they finish;
  generation checks stop a stale completion from overwriting a newer scene. No
  bitmap is recycled while a drawable can still reference it.
- HOME handoff now uses generation tokens for retries, arrival checks and launch
  callbacks. `moveToFront` is treated as a requested move, not proof of arrival;
  Home must acknowledge, with a bounded alarm fallback.
- Empty stale Home tasks are skipped. If Options is above Home, the task is
  brought forward and Home is started with CLEAR_TOP rather than leaving Options
  on screen.
- Duplicate Amazon-event suppression is reset by a real app event, so a quick
  app-to-HOME return is not swallowed.
- Cover cleanup is cancellable and runs on success, cancellation, failure and
  service destruction; transient task errors are logged.
- Options groups Appearance and Home screen settings separately, fills the
  available width, and has a corrected scene list. Next background now advances
  from Auto's resolved scene, becomes explicit, and does nothing for solid or
  adaptive backgrounds.
- `Config.autoScene(hour)` and scene stepping are host-tested at all documented
  boundaries.

## 1.12 — 2026-10-23

**First-return HOME cold start and the missing cover fixed.**

- Recents can hold two dumbtv Home tasks, including an empty stale one. The fast
  path could move the empty task, forcing a cold Home launch and leaving Amazon
  on screen until it finished. It now skips empty tasks and prefers the task with
  Home on top.
- The cold retry was 3 seconds before falling back; it is now 750 ms, so a cold
  first return does not sit on Amazon while waiting for a task that will not
  appear.
- Home is declared `alwaysRetainTaskState` and no longer clears its task on
  launch, so the first app switch is less likely to lose it.
- 1.11 left a regression where the cover window was never added at all. The cover
  is now created only for the handoff and removed afterwards; it is present when
  needed and absent over apps.
- APK: 709,501 bytes.

## 1.11 — 2026-10-23

**No launcher window over running apps.**

- The full-screen accessibility cover is now created only when the HOME handoff
  begins and removed immediately afterwards. While YouTube or any other app is in
  front, there is no dumbtv overlay window at all. This targets the intermittent
  YouTube fade/flash; the hidden cover layer was the only launcher window that
  existed over playback.
- Removed the `Home.onTrimMemory` hidden-window GPU trim, which manipulated the
  launcher's window manager while another app was playing. It saved about 35 MB
  but is not worth a display-side side effect.
- The 1.10 HOME handoff/cover timing is unchanged.
- No new permission.

## 1.10 — 2026-10-23

**HOME handoff no longer reveals Amazon's home during the fallback.**

- The black cover now stays up for up to 6 seconds, until our Home is resumed,
  instead of dropping after 1.5 seconds and exposing Amazon while the alarm start
  completes.
- A duplicate Amazon window event within 1.5 seconds of a successful task move is
  ignored, so it cannot re-cover the screen after Home has already resumed.
- The cover is removed 800 ms after a successful move and on Home's first frame.
- No new permission: this stays on the accessibility `AppTask` path.
- Fire OS still shows Amazon for a moment before its accessibility event; that
  first frame cannot be removed without system privileges.

## 1.9 — 2026-10-23

**Back to the day-home2 ocean, refined.**

- Restored the open-ocean composition from the original daylight scene instead of
  replacing it.
- Lifted the exposure and reduced the vignette so it no longer reads as dusk.
- Local contrast, a defined sun, a cleaner horizon and fine wave texture give the
  same scene more clarity without changing what it depicts.
- No new land, beach or mountain shapes.
- APK: 709,502 bytes.

## 1.8 — 2026-10-23

**Daylight beach, on the same horizon as the other scenes.**

- Replaced the daylight mountain landscape with a beach whose horizon sits at the
  same level (about 61% down) as the coast and dunes scenes, so it underlines the
  app row instead of running into it.
- Warm sand, a crisp surf edge, broken foam lines, blue sky and cloud. The day-only
  bottom scrim is lighter so the sand stays visible.
- Renamed the scene from Daylight to Daylight beach. The auto hours are unchanged.
- APK: 635,774 bytes.

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
