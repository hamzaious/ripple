# Changelog

All notable changes to this project are documented here. The format is based
on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-05-13

### Added

- Kotlin-first `RippleBackgroundView` (`com.github.ripple.effect`) extending
  `FrameLayout` with single-canvas rendering driven by one master
  `ValueAnimator`.
- Built-in shapes: `CIRCLE`, `STAR`, `ARROW`, `DIAMOND`, `MOON`.
- `MATCH_VIEW` shape mode — derives the ripple shape from any target view's
  `GradientDrawable` background or `ViewOutlineProvider`. Handles Material
  buttons, FAB, CardView, circular avatars, and rectangle/oval drawables.
- `sourceView: View?` property that auto-switches to `MATCH_VIEW` and
  re-centers ripples on the source view.
- `matchViewCornerRadius: Float` override with `MATCH_VIEW_CORNER_RADIUS_AUTO`
  sentinel — lets callers force sharp / rounded / pill ripples regardless of
  the target view's natural shape.
- Static `RippleBackgroundView.attach(view)` for one-call wrapping.
- Kotlin extension `View.addRippleEffect { … }` for fluent configuration.
- Auto-detection of the wrapper's first child as the source view when
  `app:rb_shape="matchView"` is declared via XML.
- 15 styleable XML attributes (`rb_color`, `rb_radius`, `rb_rippleAmount`,
  `rb_duration`, `rb_scale`, `rb_type`, `rb_strokeWidth`, `rb_shape`,
  `rb_matchViewCornerRadius`, `rb_starPoints`, `rb_starInnerRatio`,
  `rb_shapeRotation`, `rb_startAlpha`, `rb_endAlpha`, `rb_autoStart`,
  `rb_interpolator`).
- Material 3 demo app with live controls (shape, style, speed, ripple count,
  scale, match-view corner) that adapts to light + dark system theme.
- `maven-publish` configuration with full POM (license, developers, SCM,
  issue tracker) and a sources jar.
- JitPack support via `jitpack.yml`.

### Library characteristics

- `minSdk` 21, `compileSdk` 36 / minor API 1, JVM target 11.
- Zero runtime dependencies (no AndroidX, no Material, no Kotlin reflection).
- Release AAR ~19 KB.

[Unreleased]: https://github.com/hamzaious/ripple/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/hamzaious/ripple/releases/tag/v1.0.0
