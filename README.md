# mappand

Native Kotlin Android port of [mappho](https://github.com/iltommi/mappho) — a photo geo-management app for pCloud.

## What it does

- Sign in with your pCloud account (EU or US datacenter, email/password + optional TOTP 2FA)
- Scan your pCloud library and extract GPS coordinates from photo EXIF data
- Display geotagged photos as markers on an OpenStreetMap map
- Browse ungeotagged photos in a thumbnail grid
- Background sync: periodically uploads a metadata index back to pCloud
- Local cache via Room database for fast subsequent loads

## Build

The app is built automatically on every push to `main` via GitHub Actions. The signed APK is published as a [latest release](../../releases/tag/latest).

To build locally:

```bash
./gradlew assembleRelease
```

Requires Android SDK (API 35) and JDK 17+. Set `ANDROID_SDK_ROOT` or add an `local.properties` file with `sdk.dir=<path>`.

## Signing setup

Signing credentials are stored as GitHub Actions secrets. To generate a keystore and upload the secrets in one step, run:

```bash
python3 setup_signing.py "Your Name" CC password
```

- `Your Name` — your full name (e.g. `Tommaso Vinci`)
- `CC` — two-letter country code (e.g. `IT`)
- `password` — keystore and key password

The script generates `mappand.keystore`, then uploads four secrets to the repository via the `gh` CLI:

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (`mappand`) |
| `KEY_PASSWORD` | Key password |

Requires `keytool` (bundled with any JDK) and the [GitHub CLI](https://cli.github.com/) authenticated to your account.
