# Contributing to RippleEffect

Thanks for taking the time to contribute! This project is a small, focused
library and we want to keep it that way — quick, dependency-free, and easy to
audit. Every change should respect those goals.

## Ground rules

- **No new runtime dependencies.** The library AAR is ~19 KB and ships with
  zero AndroidX / Material deps. Pull requests that add a dependency to
  `:ripple` will not be merged.
- **`minSdk` stays at 21.** Don't use APIs that require a higher minimum
  without a working fallback.
- **No new public types unless necessary.** Prefer adding fields/methods to
  `RippleBackgroundView` over introducing helper classes.
- **Existing XML attribute names are stable.** Renaming any `rb_*` attribute
  is a breaking change.

## Getting set up

Requirements:

- JDK 17
- Android SDK with API 36 + build-tools 36.0.0
- Gradle 9.3.1 (the wrapper script will fetch it on first run)

```bash
git clone https://github.com/hamzaious/ripple.git
cd ripple
./gradlew :app:assembleDebug      # build the demo
./gradlew :ripple:assembleRelease # build the library AAR
```

## Project layout

```
RippleEffect/
├── ripple/              ← the publishable library (no AndroidX, no Material)
│   ├── build.gradle.kts
│   ├── consumer-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/github/ripple/effect/RippleBackgroundView.kt
│       └── res/values/attrs.xml
├── app/                 ← Material 3 demo app
└── README.md
```

You will likely only touch one of:

- `ripple/src/main/java/com/github/ripple/effect/RippleBackgroundView.kt`
- `ripple/src/main/res/values/attrs.xml`
- `app/src/main/...` (demo)

## Pull request checklist

Before opening a PR:

- [ ] `./gradlew :ripple:assembleRelease :app:assembleDebug --warning-mode all`
      builds with no warnings.
- [ ] If you added a new property, also add the matching `app:rb_*` XML
      attribute and document it in the README's attribute table.
- [ ] If you added a new public API, document it in the **Public API** table
      in the README.
- [ ] Update `CHANGELOG.md` under the `## [Unreleased]` heading.

## Reporting bugs

Use the GitHub Issues tracker. Include:

- Android version + device / emulator
- AGP / Gradle versions
- A minimal reproducer (XML or Kotlin snippet) if possible
- The full stack trace, if any

## Suggesting features

Before opening a feature request, please check that:

- It cannot be reasonably built by composing existing properties.
- It does not require a new runtime dependency.
- It doesn't increase the AAR by more than a few KB.

## Releasing (maintainers)

1. Update `CHANGELOG.md`: move entries from `## [Unreleased]` to a new
   `## [x.y.z] - YYYY-MM-DD` section.
2. Bump `libVersion` in `ripple/build.gradle.kts`.
3. Commit, tag (`git tag vX.Y.Z`), push tag (`git push origin vX.Y.Z`).
4. JitPack will build automatically from `jitpack.yml`. Verify on
   `https://jitpack.io/#<your-username>/RippleEffect`.

## Code style

Plain idiomatic Kotlin. No ktlint / detekt config is enforced, but please:

- Use 4-space indents.
- Prefer `val` over `var`.
- Avoid lateinit / nullable types in hot paths inside `onDraw`.
- Allocations inside `onDraw` are forbidden — reuse the `scratchPath`,
  `scratchMatrix`, `scratchOutline` fields.

That last point matters. The whole reason this library is small and fast is
that it allocates nothing per frame.
