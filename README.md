# Lazy Durtles

Lazy Durtles is an Android app for [WaniKani](https://www.wanikani.com/), the kanji learning platform by Tofugu. It is maintained by [manuel-rw](https://github.com/manuel-rw) as a fork of [Smouldering Durtles](https://github.com/jerryhcooke/smouldering_durtles) by Jerry Cooke, which in turn is a fork of [Flaming Durtles](https://github.com/ejplugge/com.the_tinkering.wk) by Ernst Jan Plugge.

This fork carries the Jetpack Compose UI migration. The upstream maintainer declined to merge that contribution, so development continues here under a new name: the licenses of the original apps do not cover their names (see [LICENSE.md](LICENSE.md)), so a fork must pick its own.

It's set up as an Android project with Google's default gradle set up.
Use Android Studio or the Gradle command line to build it.

## Download

Download the latest APK from the [Releases Section](https://github.com/manuel-rw/smouldering_durtles/releases/latest).

The original apps are still available: Smouldering Durtles on [F-Droid](https://f-droid.org/packages/com.smouldering_durtles.wk/) and [Google Play](https://play.google.com/store/apps/details?id=com.smouldering_durtles.wk).

## Releases

Releases are fully automated with [semantic-release](https://semantic-release.gitbook.io/). Every push to `main` triggers analysis of the commits since the last release:

| Commit message | Version bump |
| --- | --- |
| `fix: ...` | patch (1.2.8 → 1.2.9) |
| `feat: ...` | minor (1.2.8 → 1.3.0) |
| `feat!: ...` or `BREAKING CHANGE:` footer | major (1.2.8 → 2.0.0) |
| anything else | no release |

When a release-worthy commit is found, the pipeline builds a signed universal APK, tags the commit `vX.Y.Z`, and publishes a GitHub release with the APK and auto-generated notes. Release notes come from the commit messages; there is no changelog file.

`versionCode` is derived from the version as `major * 10000 + minor * 100 + patch` (e.g. 1.3.0 → 10300), so it stays monotonically increasing. This caps minor and patch below 100.

### Commit messages

PR titles must follow the [Conventional Commits](https://www.conventionalcommits.org/) format (`type: subject`); a CI check enforces this. Since PRs are squash-merged, the PR title becomes the commit message that drives the next version.

### Signing

Release APKs are signed in CI with a keystore supplied through GitHub secrets:

- `RELEASE_KEYSTORE_BASE64` — the keystore file, base64-encoded
- `KEYSTORE_PASSWORD` — keystore and key password
- `KEY_ALIAS` — `lazy-durtles`
- `KEY_PASSWORD` — key password

The keystore itself is **not** in the repository (gitignored as `release.keystore`). Keep a backup of `release.keystore` and `keystore.properties` — losing them means users cannot upgrade in place ever again.

### Building a release locally

```sh
./scripts/build-release.sh 1.3.0
```

This requires `keystore.properties` in the repository root (see `keystore.properties` layout: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`) and writes the APK to `dist/`. Without signing material, Gradle falls back to the debug key.

## Preparing to build the code

This repository ships the fork's identification files (`Identification.java` and `strings.xml`) with real values, so a fresh checkout builds as Lazy Durtles out of the box.

If you create your own derivative product, follow the original instructions:

- Remove the existing `Identification.java` file from `app/src/main/java/com/lazy_durtles/wk`
- Copy the file `app/Identification.java.sample.txt` to `app/src/main/java/com/lazy_durtles/wk`
- Name the copy `Identification.java`
- Edit the file to supply your own identification for the app
- Remove the existing `strings.xml` file from `app/src/main/res/values`
- Copy the file `app/strings.xml.sample.txt` to `app/src/main/res/values`
- Name the copy `strings.xml`
- Edit the file to supply your own identification for the app

After this, you are ready to build the code.

## Credits

Lazy Durtles is built on the work of the authors of the base repositories. All credit for the original apps goes to them.

### Flaming Durtles

- [Ernst Jan Plugge (ejplugge)](https://github.com/ejplugge) — created Flaming Durtles, the original Android WaniKani client this project descends from

### Smouldering Durtles

- [Jerry Cooke (jerryhcooke)](https://github.com/jerryhcooke) — maintainer of Smouldering Durtles

With contributions from:

- [Austin Almond (austin226)](https://github.com/austin226)
- [Bastian (Kaiserdragon2)](https://github.com/Kaiserdragon2)
- [Can Baykar (canbaykar)](https://github.com/canbaykar)
- [Charlie Burnham (Cammymoop)](https://github.com/Cammymoop)
- [Daniel Scherf (daspositioned)](https://github.com/daspositioned)
- [hummusw](https://github.com/hummusw)
- [Izzy (IzzySoft)](https://github.com/IzzySoft)
- [Jamie Wright (the-wright-jamie)](https://github.com/the-wright-jamie)
- [Lasath Fernando (shocklateboy92 / lasath-msft)](https://github.com/shocklateboy92)
- [Lionel Pinto (lionelpnt)](https://github.com/lionelpnt)
- [LotusEcho](https://github.com/LotusEcho)
- [Mark van Dijk (voidzero)](https://github.com/voidzero)
- [Michael Nix (mchlnix)](https://github.com/mchlnix)
- [Pacharapol Withayasakpunt (patarapolw)](https://github.com/patarapolw)
- [Panda (Gamer-dude)](https://github.com/Gamer-dude)
- [Pol (legarspol)](https://github.com/legarspol)
- [Poussinou](https://github.com/Poussinou)
- [Scott Prutton (sprutton1)](https://github.com/sprutton1)
- [Tomas Berger (Togtja)](https://github.com/Togtja)
- [Tom Beckmann (tom95)](https://github.com/tom95)

### Lazy Durtles

- [manuel-rw](https://github.com/manuel-rw) — Jetpack Compose UI migration and fork maintenance

## License

See [LICENSE.md](LICENSE.md). The source code is licensed under the Apache License 2.0. The names "Flaming Durtles" and "Smouldering Durtles" are trademarks of their respective authors and are not covered by that license — which is why this fork carries a new name.
