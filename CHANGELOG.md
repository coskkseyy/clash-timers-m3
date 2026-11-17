# Changelog

All notable changes to this project are listed here. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

1.0.2 is the first version published on GitHub. Earlier versions were shared privately.

## [1.1.0-material] - 2026-11-17

First release of the Material You fork. Everything below is new compared to Clash Timers 1.0.2.

### Added

- Material You look: colors follow the system palette, rounded shapes, the Goblin badge next to the building icon, and dynamic row sizing so a tall widget has no empty space and a short one keeps its footer.
- Two more widgets: a 2×2 mini widget and a Builder Base widget (builders, Star Laboratory, Clock Tower).
- Two accounts: each widget shows one account with its own name, chosen in a screen when the widget is placed (and from its reconfigure option). Sync only updates that widget's account.
- Footer with the time until the Builder's Apprentice and the Lab Assistant reset, for example `Helpers: reset in 15h 32m`.
- The helper badge gets the Goblin-style dark background when its bar is nearly full.
- Research Potion, Pet Potion and Study Soup are read from the export; a Gold Pass boost setting for the progress bars.
- English and Türkçe, with the game's own Turkish item names. 24-hour clocks.
- Town Hall 19: new buildings (Squad Barracks, Spiky Tower, Firecracker Tower, Guardian Building), Heroic Spell, Smasher levels 6 to 10, the rebalanced upgrade times and new icons. Buildings show the art of their own level where the game has several looks.

### Changed

- Upgrade times and names now also come from the game's own logic files, not only from the clash-of-clans-data package.
- Turkish hours are written `sa` instead of `s`.

### Fixed

- The widget settings screen is readable on phones with "force dark" turned on.

## [1.0.2] - 2026-11-03

### Fixed

- Fixed a crash at start-up on Android 10 that affected 1.0 and 1.0.1. It came from code optimization in the release build.

### Changed

- Release builds no longer use code shrinking or optimization, so the app runs reliably across phones and Android versions. The APK is larger as a result, about 9 MB instead of about 2 MB.

## 1.0.1 - 2026-11-03

### Added

- Crash reporting that stays on the phone: a crash screen, and a **Last crash** section in the app with Copy, Show and Clear.
- A notification that opens the crash screen when the app crashes in the background.

## 1.0 - 2026-11-03

First release.

### Added

- Home screen widget with builder, laboratory and pet house timers, live countdowns and progress bars.
- Sync from the game's village data export through the clipboard.
- Finish times that include Builder's Apprentice and Lab Assistant sessions, potions and Builder Bites.
- Support for the event Goblin Builder.
- Notifications when a builder, the lab or the pet house is free.
- Settings for boost strength, helper savings, helper interval and builder count.

[1.1.0-material]: https://github.com/coskkseyy/clash-timers-m3/releases/tag/latest
[1.0.2]: https://github.com/Kuswaa/clash-timers/releases/tag/v1.0.2
