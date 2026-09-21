# Aergis — Development Status

This file replaces a long, episode-by-episode narrative (R17/R18/vc60/vc62/vc63...) that had
piled up across many AI-assisted sessions in the old repository. What matters from it has been
folded in below; the play-by-play is gone. Treat this as the single current-truth checkpoint —
edit it in place as you make real progress, don't append a new "R19 — vc64" section per session.

## Where things stand

- **App name:** Aergis. **Package:** `com.airgesture.control`. **Min SDK 26 / target-compile 36.**
- **Launcher:** `AergisActivity` is the sole `MAIN`/`LAUNCHER` activity. `ProductionActivity`
  (legacy UI) and `MainActivity` are compiled but non-exported/non-launchable — see "Cleanup
  backlog" below on whether to keep them.
- **Typography:** bundled Rajdhani SemiBold (`res/font/aergis_display.ttf`, SIL OFL 1.1,
  license under `app/src/main/assets/licenses/`). No `FontFamily.SansSerif` fallback anywhere in
  the theme — this is a CI-enforced invariant.
- **Pointer pipeline:** CameraX analysis in `YUV_420_888`. MediaPipe GestureRecognizer constrained
  to one hand during pointer sessions. Landmark #8 (index fingertip) is the sole position
  measurement, run through `PointerKinematicFilter` — a causal alpha-beta/Kalman-family estimator
  with confidence-aware innovation gating, a hard no-overshoot/no-forward-extrapolation
  constraint, and a raised position-gain floor (0.25) so deliberate motion tracks faster at low
  camera cadence. Filter state resets on session start, orientation/calibration change, control-
  hand discontinuity, and tracking loss. Raw landmark-8 telemetry stays available separately from
  the filtered cursor for debugging.
- **Automated verification:** unit tests (279+ last count), lint, debug APK assembly, and hard
  invariant checks are defined in `ci.yml`.
- **Signing security:** the previously tracked `aergis-release.jks` has been removed from `main`.
  `.gitignore` already blocks `*.jks` and `keystore.properties`, and CI now fails if signing
  material is ever tracked again. The old keystore remains compromised in repository history and
  must be rotated before any trusted release is distributed; historical removal requires an
  approved secret-removal/history-rewrite process.
- **Device verification:** not current. There is no recent confirmed pass on real hardware for
  camera FPS, pointer latency, jitter, or false-click rate at the current filter tuning.

## Known open engineering issue

Real MediaPipe result cadence has been observed around 8–12 FPS on a Galaxy A54, well below what
the pointer filter and pacing constants assume. This is upstream of the pointer-filter tuning
work (it's a CameraX/MediaPipe throughput problem, not a smoothing problem) and hasn't been
root-caused yet. Next investigation step: trace actual camera FPS → submitted-frame FPS →
MediaPipe result FPS, and check whether a higher-throughput CameraX stream configuration is
available and worth the trade-off.

## Cleanup backlog (safe to pick up any time)

- `ProductionActivity.kt` (~1180 lines) and `MainActivity.kt` (~560 lines) are legacy/unused UI
  kept compiled-but-unreachable. Decide whether anything in them is still needed as reference,
  then either delete them or fold anything useful into the current `AergisActivity` surface. This
  is pure cleanup — don't do it in the same change as a behavior fix, so a regression is easy to
  attribute.
- A handful of user-facing strings inside `ProductionActivity.kt` and `MainActivity.kt` still say
  "AERMOTUS" — irrelevant if those files get deleted per the point above; otherwise rename them
  for consistency.
- `AermotusVisuals.kt`, `AirModels.kt`, and other filenames still carry the old `AERMOTUS`/`Air`
  naming even though behavior and app identity have moved to Aergis. Renaming files is safe but
  touch-heavy (imports everywhere) — worth doing once, deliberately, not as a side effect of an
  unrelated change.

## Immediate next action

1. Validate the current `main` revision through GitHub Actions. The repository currently reports
   no workflow runs, so there is not yet CI evidence for this revision.
2. Once CI is green, install the resulting debug APK on a real device and measure camera →
   MediaPipe result FPS, pointer latency, jitter, and false-click rate.
3. Investigate the observed 8–12 FPS MediaPipe cadence as a separate performance change, using
   measured camera/submission/result rates before changing pointer-filter constants.
4. Rotate the compromised preview signing key and perform an approved historical secret-removal
   process before treating any future release signing identity as trusted.
