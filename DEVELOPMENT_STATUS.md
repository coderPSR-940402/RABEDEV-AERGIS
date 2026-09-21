# Aergis — Development Status

> Single current-truth engineering checkpoint.
> Update existing entries in place. Do not append chronological R19/vc64/etc. sections.

## 1. Project identity

| Field | Current value |
|---|---|
| App | Aergis |
| Package | `com.airgesture.control` |
| Min SDK | 26 |
| Compile/target SDK | 36 |
| Launcher | `AergisActivity` |
| Current delivery | `0.18.38-preview` / vc63 |
| Pointer measurement | MediaPipe landmark #8 |
| Camera format | `YUV_420_888` |
| Pointer estimator | `PointerKinematicFilter` |
| Automated tests | 279+ last recorded count |
| Hardware validation | **NOT VERIFIED** |
| Default branch | `main` |

The current delivery identity and pointer invariants are encoded in CI. CI checks versionCode 63 / versionName `0.18.38-preview` and the single `AergisActivity` launcher.

## 2. Current state

- Aergis uses package `com.airgesture.control`, Min SDK 26 and compile/target SDK 36.
- `AergisActivity` is the sole `MAIN`/`LAUNCHER` activity. `ProductionActivity` and `MainActivity` remain compiled legacy UI and are not launchable.
- Typography is bundled Rajdhani SemiBold (`res/font/aergis_display.ttf`) with the SIL OFL 1.1 license under `app/src/main/assets/licenses/`.
- The pointer path uses CameraX `YUV_420_888`, MediaPipe GestureRecognizer, landmark #8, and `PointerKinematicFilter`.
- The estimator is intended to remain causal, confidence-aware, cadence-aware, no-forward-extrapolating, and no-overshooting.
- Tracked preview keystores were removed from `main`; `.gitignore` and CI reject tracked signing material. Historical keys remain compromised in repository history and must not be treated as trusted release identities until rotation and approved history-removal work are complete.
- Latest known `main` CI run #6, commit `747f9f3b075378569b8cd4cfbdb5b833fed91a0d`, failed at `Unit tests, lint and APK`; downstream APK/invariant verification did not run. CI is **NOT GREEN**.

## 3. Verification status

### CI verification

The workflow is designed to verify:

- unit-test reports with zero failures/errors;
- Android lint;
- debug APK assembly;
- exactly one launcher: `com.airgesture.control.AergisActivity`;
- bundled Aergis typography and no `FontFamily.SansSerif` fallback in the checked UI path;
- CameraX `YUV_420_888` analysis;
- landmark-8 integration with `PointerKinematicFilter`;
- 8 ms pointer submission ceiling;
- disabled debug floating camera overlay;
- APK signing and badging.

**Current CI status: NOT GREEN.** These final checks are not currently a passing release baseline because the latest build failed before the verification stage.

### Hardware verification

**NONE CURRENTLY.** The following remain unverified on physical hardware:

- camera capture FPS;
- submitted analysis-frame FPS;
- MediaPipe result FPS;
- pointer end-to-end latency;
- pointer jitter and accuracy;
- false-click rate;
- gesture recognition reliability;
- sustained CPU/performance behavior;
- thermal behavior/throttling;
- service restart/recovery;
- screen rotation/orientation behavior.

### Observed, not root-caused

- Galaxy A54 MediaPipe result cadence: approximately 8–12 FPS.

This is an observation for investigation, not a hardware acceptance result.

## 4. Runtime architecture

```text
CameraX
  ↓
Frame pacing / backpressure
  ↓
MediaPipe GestureRecognizer
  ↓
Landmark #8 measurement
  ↓
Control-hand validation
  ↓
PointerKinematicFilter
  ↓
Coordinate mapping
  ↓
Accessibility pointer injection
  ↓
Gesture/action semantics
```

| Component | Responsibility |
|---|---|
| CameraX | Frame acquisition |
| `VisionFramePacer` | Analysis submission cadence / pacing |
| `GestureCaptureService` | Runtime orchestration |
| MediaPipe | Hand/gesture measurement |
| `ControlHandSelector` | Control-hand ownership |
| `PointerKinematicFilter` | Cursor estimation |
| `AirAccessibilityService` | OS interaction |
| `ActionMappingStore` | User action mapping |

## 5. Protected behavior and contracts

### Protected behavior

Unless an investigation explicitly targets one of these areas, do not change:

- landmark #8 as the pointer position measurement;
- one-hand pointer-session constraint;
- control-hand ownership semantics;
- click/gesture semantics;
- no-forward-extrapolation;
- no-overshoot;
- filter reset conditions;
- raw landmark telemetry availability;
- single launcher policy;
- bundled Aergis typography.

### Pointer estimator contract

`PointerKinematicFilter` MUST:

- be causal;
- use only the newest available measurement and prior state;
- never predict beyond the newest measurement;
- never overshoot the measured target;
- reject/gate sufficiently low-confidence innovations;
- reset on session start;
- reset on orientation/calibration change;
- reset on control-hand discontinuity;
- reset on tracking loss;
- preserve raw landmark #8 telemetry independently of filtered output.

It MUST NOT:

- use future samples;
- interpolate against future measurements;
- intentionally lead the newest measurement;
- silently retain stale state after tracking loss.

## 6. Current blockers

### BLOCKER 1 — CI

**Status: IN PROGRESS**

Latest `main` run failed during `Unit tests, lint and APK`; signing/badging/invariant checks were skipped.

Acceptance:

- [ ] `ci.yml` completes successfully;
- [ ] unit tests pass;
- [ ] lint passes;
- [ ] debug APK assembles;
- [ ] launcher invariant passes;
- [ ] typography invariant passes;
- [ ] pointer pipeline invariants pass;
- [ ] APK signing/badging checks pass.

### BLOCKER 2 — Hardware validation

**Status: NOT STARTED**

Required device: Samsung Galaxy A54 5G.

Required measurements: camera FPS, submitted analysis FPS, MediaPipe result FPS, pointer latency, jitter, accuracy, false-click rate, gesture reliability, and thermal/performance behavior.

### BLOCKER 3 — Signing identity hygiene

**Status: OPEN**

- Historical preview signing identities are compromised.
- Rotate both compromised identities.
- Complete an approved historical secret-removal process before treating a future release signing identity as trusted.

## 7. Active investigations

### Camera / MediaPipe throughput

**Hypothesis:** MediaPipe inference throughput may be limiting pointer responsiveness on Galaxy A54.

Do not change pointer-filter tuning until the throughput bottleneck is identified.

#### Measurements required

1. Camera frames produced/sec
2. Frames accepted by analysis pipeline/sec
3. Frames submitted to MediaPipe/sec
4. MediaPipe results/sec
5. Median inference time
6. P95 inference time
7. Frame age at inference
8. Dropped frames
9. Analyzer queue depth
10. CPU utilization
11. Thermal state
12. Resolution
13. Camera exposure/FPS configuration

#### Required configurations

- current configuration;
- lower analysis resolution;
- higher camera FPS request;
- alternate CameraX backpressure strategy;
- analyzer executor configuration;
- MediaPipe-only throughput test.

Record raw measurements before and after every performance change. Keep throughput changes separate from pointer-filter tuning.

## 8. Performance budgets

| Metric | Target | Warning | Failure |
|---|---:|---:|---:|
| Camera FPS | ≥30 | <24 | <15 |
| MediaPipe result FPS | ≥20 | <15 | <10 |
| Pointer submission interval | ≤8 ms | >12 ms | >20 ms |
| Pointer latency | TBD | TBD | TBD |
| Tracking jitter | TBD | TBD | TBD |
| False-click rate | TBD | TBD | TBD |
| CPU sustained load | TBD | TBD | TBD |
| Thermal throttling | None | Detectable | Severe |

> TBD thresholds must be measured on Galaxy A54 before becoming product requirements.

## 9. Release gate

A build MUST NOT be considered hardware-validated or release-ready merely because CI passes.

- [ ] CI green
- [ ] Unit tests green
- [ ] Lint green
- [ ] APK integrity verified
- [ ] Signing verified
- [ ] Galaxy A54 installation verified
- [ ] Camera throughput measured
- [ ] MediaPipe throughput measured
- [ ] Pointer latency measured
- [ ] Pointer jitter measured
- [ ] False-click rate measured
- [ ] Gesture regression check completed
- [ ] No unresolved P0/P1 issues
- [ ] Trusted release signing identity confirmed

## 10. Security / release hygiene

### P0

- Remove literal preview keystore passwords from tracked source/configuration.
- Verify no production credentials exist in Git history.
- Rotate compromised preview signing identities.
- Complete approved historical secret-removal/history-rewrite work.
- Add and maintain secret scanning.

CI already rejects tracked `*.jks` files and `keystore.properties`.

## 11. Cleanup backlog

### P0 — Security / release hygiene

- Rotate compromised preview signing identities.
- Complete historical secret-removal process.
- Add secret scanning.

### P1 — Performance

- Root-cause Galaxy A54 MediaPipe throughput.
- Establish camera/result/pointer performance budgets from measurements.
- Measure thermal behavior.

### P2 — Architecture

- Split `GestureCaptureService`.
- Split `AirAccessibilityService`.
- Reduce `AergisActivity` responsibilities.

### P3 — Cleanup

- Delete unused `ProductionActivity` after dependency/reachability verification.
- Delete unused `MainActivity` after dependency/reachability verification.
- Remove obsolete AERMOTUS strings.
- Rename remaining AERMOTUS/Air files deliberately and in an isolated change.

### P4 — Documentation

- Keep this document synchronized with verified behavior.
- Document performance-test methodology.
- Preserve evidence links and test-build identity for important verification claims.

## 12. Architecture backlog

### Legacy UI deletion criteria

Before deleting `ProductionActivity.kt` or `MainActivity.kt`:

- search all source references;
- search manifest references;
- search navigation references;
- search string/resource references;
- search tests;
- inspect unique functionality;
- confirm neither is reachable through an exported component;
- run CI after deletion.

If no unique reachable behavior remains, delete the dead implementation.

### Change discipline

Performance, behavior, cleanup, architecture, naming, signing, and UI changes should be isolated.

Do not combine:

- behavior change + legacy deletion;
- filter tuning + CameraX architecture changes;
- signing changes + UI refactoring;
- large rename + functional changes.

Each isolated change must have its own CI result.

## 13. Third-party assets

| Asset | Source | Version | Hash | License | Verification |
|---|---|---|---|---|---|
| Rajdhani SemiBold | Bundled in repository | Current bundled asset | TBD | SIL OFL 1.1 | CI presence/invariant |
| MediaPipe model/assets | Provenance to be recorded | TBD | TBD | TBD | TBD |

> Complete provenance fields only when verified. Do not invent hashes or license metadata.

## 14. Durable engineering decisions

### Pointer measurement

Landmark #8 is the sole pointer position measurement.

**Reason:** maintains a simple, deterministic measurement path and preserves the established pointer contract.

### No forward extrapolation

Pointer output may not intentionally lead the newest MediaPipe measurement.

**Reason:** prevents visually responsive but causally incorrect cursor behavior.

### Single launcher

`AergisActivity` is the sole launchable activity.

**Reason:** avoids legacy UI ambiguity and accidental entry-point regressions.

### Separate raw telemetry

Raw landmark #8 telemetry remains independently available from filtered cursor output.

**Reason:** allows debugging and performance analysis without conflating measurement with estimator output.

## 15. Evidence / verification references

### CI

- Workflow: `.github/workflows/ci.yml`
- Latest known `main` commit: `747f9f3b075378569b8cd4cfbdb5b833fed91a0d`
- Latest known run: `#6` — **FAILED**
- Failed step: `Unit tests, lint and APK`
- Final signing/invariant step: **SKIPPED**

### Tests

- Last recorded automated test count: 279+
- Current successful count: **NOT VERIFIED until CI is green again**

### Hardware

- Device: Samsung Galaxy A54 5G
- Current hardware validation: **NOT VERIFIED**
- Observed MediaPipe cadence: approximately 8–12 FPS
- Full raw performance report: **NOT YET RECORDED**

### Evidence rule

Every important status claim must have an evidence trail in one or more of:

- CI workflow/run;
- test report artifact;
- APK/build-evidence artifact;
- hardware test report;
- source-level invariant or behavioral test.

Do not promote an observation or historical claim to "verified" without current evidence.

## 16. Maintenance rules for this document

1. This file is a current-state checkpoint, not a development diary.
2. Update existing sections in place.
3. Do not append R19/R20/vc64/etc. chronological sections.
4. Separate implementation facts, CI verification, hardware verification, observations, hypotheses, and planned work.
5. Never mark hardware behavior verified from CI alone.
6. Do not invent performance thresholds, hashes, test counts, device results, or release readiness.
7. When a claim changes, update its evidence and status in the same change.
8. Keep security issues and release blockers visible until independently verified closed.
9. Keep performance experiments isolated from behavior/filter changes.
10. Preserve this document as the canonical checkpoint for future AI-assisted engineering sessions.

## Priority order

### P0

1. Restore green CI.
2. Maintain signing-material hygiene and rotate compromised identities.
3. Establish the CI-vs-hardware verification distinction.
4. Instrument the complete camera → MediaPipe → pointer pipeline.

### P1

5. Root-cause the 8–12 FPS result cadence.
6. Establish measured performance budgets.
7. Establish hardware release gates.
8. Add security/secret scanning.

### P2

9. Refactor the three oversized Kotlin components.
10. Convert source-string CI assertions into behavioral tests where practical.
11. Delete legacy activities after dependency/reachability verification.

### P3

12. Complete AERMOTUS → Aergis naming cleanup.
13. Make asset provenance/reproducibility explicit.
14. Improve documentation/evidence tracking.
