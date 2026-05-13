<div align="center">

# RippleEffect

**A lightweight, Kotlin-first ripple background view for Android.**

Pick a color, drop it behind any view, and get smooth, expanding ripples that
either match a built-in shape (circle, star, arrow, diamond, moon) **or
automatically take the shape of whatever view you point them at** — a Material
button, a circular avatar, a card, a chip, an image.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![JitPack](https://jitpack.io/v/ripple-effect/RippleEffect.svg)](https://jitpack.io/#ripple-effect/RippleEffect)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![AGP](https://img.shields.io/badge/AGP-9.1%2B-success.svg)](https://developer.android.com/build/releases/gradle-plugin)
[![AAR size](https://img.shields.io/badge/AAR-~19%20KB-informational.svg)](#performance-notes)
[![Build](https://github.com/ripple-effect/RippleEffect/actions/workflows/build.yml/badge.svg)](https://github.com/ripple-effect/RippleEffect/actions/workflows/build.yml)

</div>

```text
   ┌───────────────────────────────┐
   │            (•)                │   ◉ ◉ ◉   ripples auto-match
   │       (   ◉   )               │           the wrapped view's
   │     ( (  ◉◉◉  ) )             │           shape and size
   │       (   ◉   )               │
   │            (•)                │
   └───────────────────────────────┘
```

---

## Highlights

| | |
|---|---|
| Pure Kotlin, fluent property-style API | XML, Kotlin DSL, and Java entry points |
| AGP 9 / Gradle 9 / JDK 17 / `minSdk` 21 | Modern Android Canvas + `Path` rendering |
| 5 built-in shapes + auto **match-view** mode | Fill or stroke styles |
| Single `ValueAnimator` drives N ripples | Cached unit `Path` reused every frame |
| Lifecycle-safe (auto-stops on detach) | `maven-publish` + JitPack configured |
| **Zero runtime dependencies** | **~19 KB release AAR** |

---

## Table of contents

1. [Screenshots](#screenshots)
2. [Install](#install)
3. [Quick start](#quick-start)
4. [Match the shape of any view](#match-the-shape-of-any-view)
5. [XML attributes](#xml-attributes)
6. [Public API](#public-api)
7. [Performance notes](#performance-notes)
8. [Publishing your own build](#publishing-your-own-build)
9. [Contributing](#contributing)
10. [License](#license)

---

## Screenshots

> Demo APK can be built with `./gradlew :app:assembleDebug` or downloaded from
> the [latest CI run](https://github.com/ripple-effect/RippleEffect/actions).
> Once you've recorded screenshots, drop them in `docs/` and update the paths
> below.

| Light theme | Dark theme | Match-view |
|---|---|---|
| `docs/light.png` | `docs/dark.png` | `docs/match-view.gif` |

---

## Install

This repository contains two modules:

- **`:ripple`** — the publishable library.
- **`:app`** — a Material 3 demo with live shape, style, speed, ripple-count,
  scale and match-view-corner controls. It adapts to light and dark system
  themes.

```bash
./gradlew :app:installDebug         # install the demo on a device/emulator
./gradlew :ripple:assembleRelease   # produce ripple-release.aar
```

### Option 1 — Gradle (recommended)

Add JitPack to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

Then in your module:

```kotlin
dependencies {
    implementation("com.github.ripple.effect:ripple:1.0.0")
}
```

Replace `1.0.0` with any [tagged release](https://github.com/ripple-effect/RippleEffect/releases)
or a short commit SHA. JitPack builds the library from `jitpack.yml` and
serves the AAR on demand. The first build of a tag takes ~1–3 minutes,
subsequent fetches are instant.

> The actual dependency string that JitPack emits depends on the GitHub
> organisation that hosts the repo. If you fork this project, your
> coordinates become `com.github.<your-github-username>:RippleEffect:<tag>`.

### Option 2 — As a Gradle module (vendoring)

```kotlin
// settings.gradle.kts
include(":ripple")

// build.gradle.kts
dependencies {
    implementation(project(":ripple"))
}
```

### Option 3 — Maven Central / self-hosted

If you've pushed the library to your own repository:

```kotlin
dependencies {
    implementation("com.github.ripple.effect:ripple:1.0.0")
}
```

See [Publishing your own build](#publishing-your-own-build) for the workflow.

---

## Quick start

### XML

```xml
<com.github.ripple.effect.RippleBackgroundView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:rb_color="#0099CC"
    app:rb_radius="32dp"
    app:rb_rippleAmount="4"
    app:rb_duration="3000"
    app:rb_scale="6"
    app:rb_shape="star"
    app:rb_type="fillRipple"
    app:rb_autoStart="true">

    <ImageView
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:layout_gravity="center"
        android:src="@drawable/my_logo" />

</com.github.ripple.effect.RippleBackgroundView>
```

### Kotlin

```kotlin
val ripple = findViewById<RippleBackgroundView>(R.id.ripple)

ripple.rippleShape    = RippleBackgroundView.Shape.STAR
ripple.rippleType     = RippleBackgroundView.Type.STROKE
ripple.rippleColor    = 0xFF0099CC.toInt()
ripple.rippleAmount   = 5
ripple.rippleDuration = 2_500L
ripple.rippleScale    = 6f
ripple.strokeWidth    = 4f * resources.displayMetrics.density
ripple.start()

if (ripple.isRunning) ripple.stop()
```

### Java

```java
RippleBackgroundView ripple = findViewById(R.id.ripple);
ripple.setRippleShape(RippleBackgroundView.Shape.DIAMOND);
ripple.setRippleColor(0xFF0099CC);
ripple.start();
```

---

## Match the shape of any view

The standout feature: point the ripple at a real `View` and it will
automatically take **that view's shape** — round-rect for Material buttons,
circle for circular avatars, oval for chips, plain rect for image views.

There are three ways to use it.

### 1. One-liner *(Kotlin)*

```kotlin
findViewById<MaterialButton>(R.id.cta).addRippleEffect {
    rippleColor    = Color.MAGENTA
    rippleAmount   = 5
    rippleDuration = 2_200
}
```

`addRippleEffect` wraps the target inside a `RippleBackgroundView` in the
same parent, derives the target's shape, centers ripples on it, disables
`clipChildren` / `clipToPadding` on the parent so the ripples can grow past
the target's bounds, and starts the animation. It returns the wrapper so
you can chain configuration on it.

### 2. Static helper *(Java-friendly)*

```java
RippleBackgroundView ripple = RippleBackgroundView.attach(myButton);
ripple.setRippleColor(0xFF4FC3F7);
```

`attach` is idempotent — calling it twice on the same view returns the
existing wrapper instead of creating nested ones.

### 3. XML

```xml
<com.github.ripple.effect.RippleBackgroundView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:clipChildren="false"
    android:clipToPadding="false"
    app:rb_color="#4FC3F7"
    app:rb_shape="matchView">

    <com.google.android.material.button.MaterialButton
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="Tap me"
        app:cornerRadius="20dp" />

</com.github.ripple.effect.RippleBackgroundView>
```

When `rb_shape="matchView"` is set, the first child of the wrapper is picked
up automatically on the first layout pass and its shape is re-detected
whenever its bounds change.

### Override the corner radius

You can take full control of the ripple corner radius **without changing the
target view** via `matchViewCornerRadius` (or `app:rb_matchViewCornerRadius`
in XML). It's in **pixels**.

| Value                     | Result                                       |
|---------------------------|----------------------------------------------|
| `< 0` (default, `AUTO`)   | Follow the source view's natural shape.      |
| `0`                       | Force sharp rectangular ripples.             |
| Any positive `< halfMin`  | Round-rect with that exact corner radius.    |
| `>= halfMin`              | Promotes to a perfect oval / circle.         |

```kotlin
findViewById<MaterialButton>(R.id.cta).addRippleEffect {
    // Match the button, but ripple with double the corner radius.
    matchViewCornerRadius = 40f * resources.displayMetrics.density
}

// Reset to following the view's natural shape:
ripple.matchViewCornerRadius = RippleBackgroundView.MATCH_VIEW_CORNER_RADIUS_AUTO
```

```xml
<com.github.ripple.effect.RippleBackgroundView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:rb_shape="matchView"
    app:rb_matchViewCornerRadius="24dp">
    <com.google.android.material.button.MaterialButton .../>
</com.github.ripple.effect.RippleBackgroundView>
```

### How shape detection works

`MATCH_VIEW` walks two probes, in order:

1. **`GradientDrawable` background** — catches `oval`, plain `rectangle`,
   and `rectangle` with `cornerRadius`.
2. **`ViewOutlineProvider`** — handles Material buttons, `CardView`, FABs,
   round-rect outlines and inscribed-circle outlines.
3. Falls back to **plain rect** for views with arbitrary path outlines.

Aspect ratio is preserved: a 100 × 40 button produces a unit path of
`(-1, -0.4, 1, 0.4)`, scaled uniformly per ripple. At `rippleScale = 1`
the first ripple sits exactly on the view's edges; at `rippleScale = 6`
it expands to 6× that size.

> **Tip:** to ripple along the alpha channel of a transparent PNG, install
> a custom `ViewOutlineProvider` on the `ImageView` that calls
> `outline.setPath(yourTracedPath)`. `MATCH_VIEW` will pick it up
> automatically.

---

## XML attributes

| Attribute              | Type      | Default               | Description |
|------------------------|-----------|-----------------------|-------------|
| `rb_color`             | color     | `#FF0099CC`           | Ripple color. |
| `rb_radius`            | dimension | `64dp`                | Base radius before scaling. Ignored in `matchView` mode (the source view's size wins). |
| `rb_rippleAmount`      | integer   | `4`                   | Concurrent ripples on screen. |
| `rb_duration`          | integer   | `3000`                | Duration of one ripple cycle, in ms. |
| `rb_scale`             | float     | `6`                   | Final scale factor at the end of a cycle. |
| `rb_type`              | enum      | `fillRipple`          | `fillRipple` or `strokeRipple`. |
| `rb_strokeWidth`       | dimension | `2dp`                 | Stroke width when `rb_type=strokeRipple`. |
| `rb_shape`             | enum      | `circle`              | `circle`, `star`, `arrow`, `diamond`, `moon`, `matchView`. |
| `rb_matchViewCornerRadius` | dimension | `auto`            | Override for the ripple corner radius in `matchView` mode. Unset / negative = follow the source view; `0dp` = sharp rectangle; large values promote to oval. |
| `rb_starPoints`        | integer   | `5`                   | Number of points for `star` (>= 3). |
| `rb_starInnerRatio`    | float     | `0.45`                | Inner-radius ratio for `star` (0..1). |
| `rb_shapeRotation`     | float     | `0`                   | Rotation per ripple, in degrees. |
| `rb_startAlpha`        | float     | `1.0`                 | Alpha at progress = 0 (0..1). |
| `rb_endAlpha`          | float     | `0.0`                 | Alpha at progress = 1 (0..1). |
| `rb_autoStart`         | boolean   | `false`               | Start animating as soon as the view is attached. |
| `rb_interpolator`      | reference | accelerate-decelerate | Interpolator applied to each ripple. |

Every attribute is also exposed as a Kotlin `var` on `RippleBackgroundView`,
so you can mutate it at runtime.

---

## Public API

### Class — `com.github.ripple.effect.RippleBackgroundView`

| Member | Type | Notes |
|---|---|---|
| `rippleColor`         | `Int`            | `@ColorInt` |
| `rippleRadius`        | `Float`          | px |
| `rippleAmount`        | `Int`            | clamped to ≥ 1 |
| `rippleDuration`      | `Long`           | ms; restarts the animator if changed mid-run |
| `rippleScale`         | `Float`          | final ripple scale |
| `rippleType`          | `Type`           | `FILL` or `STROKE` |
| `strokeWidth`         | `Float`          | px, used by `STROKE` |
| `rippleShape`         | `Shape`          | `CIRCLE`, `STAR`, `ARROW`, `DIAMOND`, `MOON`, `MATCH_VIEW` |
| `starPoints`          | `Int`            | clamped to ≥ 3 |
| `starInnerRatio`      | `Float`          | clamped to `0.05..0.95` |
| `shapeRotation`       | `Float`          | degrees |
| `startAlpha`          | `Float`          | clamped to `0..1` |
| `endAlpha`            | `Float`          | clamped to `0..1` |
| `rippleInterpolator`  | `Interpolator`   | per-ripple curve |
| `autoStart`           | `Boolean`        | start on attach |
| `sourceView`          | `View?`          | when set, switches to `MATCH_VIEW` and centers ripples on the view |
| `matchViewCornerRadius` | `Float`        | px override for `MATCH_VIEW` corner radius. `MATCH_VIEW_CORNER_RADIUS_AUTO` (`-1f`) follows the source view. |
| `isRunning`           | `Boolean` (get)  | animation state |
| `start()`             | `Unit`           | idempotent |
| `stop()`              | `Unit`           | clears the canvas |

### Companion / static

```kotlin
companion object {
    const val MATCH_VIEW_CORNER_RADIUS_AUTO: Float = -1f

    @JvmStatic
    fun attach(target: View): RippleBackgroundView
}
```

### Kotlin extension

```kotlin
fun View.addRippleEffect(
    configure: RippleBackgroundView.() -> Unit = {}
): RippleBackgroundView
```

---

## Performance notes

- **One `ValueAnimator` for N ripples.** Each ripple's progress is derived
  from a phase offset on a single master animator, instead of allocating
  one animator per ripple.
- **No child views per ripple.** All ripples are painted directly on the
  view's own canvas. The wrapped content is drawn on top.
- **Cached unit paths.** Star, arrow, diamond, moon and match-view shapes
  are built once into a `Path` of radius 1 and transformed each frame via
  a reused `Matrix`. Stroke width stays constant regardless of ripple scale.
- **Circle fast path.** Circles use `canvas.drawCircle` directly — no path
  allocation per frame.
- **Smart invalidation.** `invalidate()` is only called while the animator
  is actually running.
- **Leak-safe.** `onDetachedFromWindow` always cancels the animator; if
  `autoStart=true`, it auto-resumes on re-attach.
- **Tiny AAR.** No AndroidX, no Material, no Kotlin reflection. The release
  AAR is ~19 KB on disk.

---

## Publishing your own build

The `:ripple` module already applies `maven-publish` and configures a
release publication with a sources jar and full POM metadata (license,
developers, SCM, issue tracker).

### Maven Local (testing)

```bash
./gradlew :ripple:publishReleasePublicationToMavenLocal
```

That drops a `.aar`, sources jar and POM into `~/.m2/`. Default coordinates:

```text
com.github.ripple.effect:ripple:1.0.0
```

Change them in the `libGroup` / `libArtifact` / `libVersion` block near the
top of [`ripple/build.gradle.kts`](ripple/build.gradle.kts).

### JitPack (zero-setup hosted Maven)

A `jitpack.yml` is included at the repository root:

```yaml
jdk:
  - openjdk17

install:
  - ./gradlew :ripple:publishToMavenLocal -x test -x lint --no-daemon
```

To release a new version:

1. Bump `libVersion` in `ripple/build.gradle.kts`.
2. Update `CHANGELOG.md`.
3. `git tag v1.0.0 && git push origin v1.0.0`.

JitPack will build automatically. Verify on
`https://jitpack.io/#<your-username>/RippleEffect` — the first build of a
given tag takes ~1–3 minutes, subsequent fetches are instant.

Consumers then add (the actual group depends on the GitHub org or user that
hosts the repo — JitPack emits `com.github.<github-username-or-org>` as the
group ID, not the `libGroup` you set in `build.gradle.kts`):

```kotlin
implementation("com.github.<your-github-username>:RippleEffect:v1.0.0")
```

### Maven Central / GitHub Packages

If you want the literal coordinate `com.github.ripple.effect:ripple:1.0.0`
to resolve, publish to Maven Central (after claiming the namespace via
Sonatype) or a self-hosted Maven repo. Add a `repositories {}` block in the
publishing config:

```kotlin
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/<your-org>/RippleEffect")
            credentials {
                username = System.getenv("GH_USER")
                password = System.getenv("GH_TOKEN")
            }
        }
    }
}
```

Then `./gradlew :ripple:publishReleasePublicationToGitHubPackagesRepository`.
For Maven Central, sign the publication with the `signing` plugin and target
`s01.oss.sonatype.org`.

---

## Contributing

Pull requests and issues are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md)
for the ground rules. In short:

- **No new runtime dependencies** in `:ripple`.
- **`minSdk` stays at 21.**
- **No allocations in `onDraw`.**
- If you add a property, also add the matching `app:rb_*` XML attribute and
  document it in the [XML attributes](#xml-attributes) table.
- Update [CHANGELOG.md](CHANGELOG.md) under `## [Unreleased]`.

A GitHub Actions workflow (`.github/workflows/build.yml`) builds the
library + demo and uploads the AAR / APK on every push.

---

## License

[MIT](LICENSE) © 2026 RippleEffect contributors
