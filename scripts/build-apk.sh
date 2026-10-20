#!/usr/bin/env bash
# Build with official Android SDK tools only; no third-party runtime dependencies.
set -euo pipefail
repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
: "${sdk_dir:?Set ANDROID_HOME to your Android SDK}"
build_tools="$sdk_dir/build-tools/35.0.0"
android_jar="$sdk_dir/platforms/android-35/android.jar"
work_dir="$repo_dir/android/app/build/manual"
mkdir -p "$work_dir/gen" "$work_dir/classes" "$work_dir/dex"
rm -f "$work_dir/base.apk" "$work_dir/resources.zip" "$work_dir/classes.jar"
python3 - "$repo_dir" "$work_dir" <<'PY'
import pathlib,sys
repo,work=map(pathlib.Path,sys.argv[1:]);manifest=(repo/'android/app/src/main/AndroidManifest.xml').read_text()
(work/'AndroidManifest.xml').write_text(manifest.replace('<manifest ', '<manifest package="com.artist.king" ',1))
PY
"$build_tools/aapt2" compile --dir "$repo_dir/android/app/src/main/res" -o "$work_dir/resources.zip"
"$build_tools/aapt2" link -o "$work_dir/base.apk" -I "$android_jar" --manifest "$work_dir/AndroidManifest.xml" --java "$work_dir/gen" --min-sdk-version 28 --target-sdk-version 34 --version-code 3 --version-name 1.2.0  -A "$repo_dir/android/app/src/main/assets" "$work_dir/resources.zip"
java com.sun.tools.javac.Main --release 8 -classpath "$android_jar" -d "$work_dir/classes" "$work_dir/gen/com/artist/king/R.java" "$repo_dir"/android/app/src/main/java/com/artist/king/*.java
python3 - "$work_dir" <<'PY'
import pathlib,sys,zipfile
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'classes.jar','w') as z:
 for f in (p/'classes').rglob('*.class'):z.write(f,str(f.relative_to(p/'classes')))
PY
"$build_tools/d8" --lib "$android_jar" --min-api 28 --output "$work_dir/dex" "$work_dir/classes.jar"
python3 - "$work_dir" <<'PY'
import pathlib,sys,zipfile
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'base.apk','a') as z:
 for f in (p/'dex').glob('*.dex'):z.write(f,f.name)
PY
"$build_tools/zipalign" -f 4 "$work_dir/base.apk" "$work_dir/aligned.apk"
if [[ -n "${KING_KEYSTORE:-}" ]]; then
  : "${KING_SIGN_PASS:?Set KING_SIGN_PASS to the private keystore password}"
  "$build_tools/apksigner" sign --ks "$KING_KEYSTORE" --ks-key-alias king --ks-pass env:KING_SIGN_PASS --key-pass env:KING_SIGN_PASS --out "$work_dir/KING-Focus.apk" "$work_dir/aligned.apk"
else
  # Local development key. This cannot update the signed distributed APK.
  dev_key="$work_dir/development.keystore"
  if [[ ! -f "$dev_key" ]]; then
    keytool -genkeypair -keystore "$dev_key" -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug'
  fi
  "$build_tools/apksigner" sign --ks "$dev_key" --ks-key-alias androiddebugkey --ks-pass pass:android --out "$work_dir/KING-Focus.apk" "$work_dir/aligned.apk"
fi
"$build_tools/apksigner" verify --verbose "$work_dir/KING-Focus.apk"
printf '\nBuilt: %s\n' "$work_dir/KING-Focus.apk"
