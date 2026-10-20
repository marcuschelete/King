"""Build a signed KING APK on Windows, Linux or macOS using official SDK tools.

Set ANDROID_HOME, JAVA_HOME, KING_KEYSTORE and KING_SIGN_PASS. No development
key fallback: distributed updates must use the original private signing key.
Build progress assets with `cd progress && npm ci && npm run build` first.
"""
import os
import pathlib
import subprocess
import sys
import zipfile

repo = pathlib.Path(__file__).resolve().parents[1]
sdk = pathlib.Path(os.environ['ANDROID_HOME']).resolve()
java = pathlib.Path(os.environ['JAVA_HOME']).resolve() / 'bin'
tools = sdk / 'build-tools/35.0.0'
platform = sdk / 'platforms/android-35/android.jar'
work = repo / 'android/app/build/manual'
for directory in ['gen', 'classes', 'dex', 'tests']:
    (work / directory).mkdir(parents=True, exist_ok=True)
ext = '.exe' if os.name == 'nt' else ''
def run(*args):
    subprocess.run([str(a) for a in args], check=True)
def java_tool(jar, *args):
    run(java / ('java' + ext), '-jar', tools / 'lib' / jar, *args)
manifest = (repo / 'android/app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
(work / 'AndroidManifest.xml').write_text(manifest.replace('<manifest ', '<manifest package="com.artist.king" ', 1), encoding='utf-8')
run(tools / ('aapt2' + ext), 'compile', '--dir', repo / 'android/app/src/main/res', '-o', work / 'resources.zip')
run(tools / ('aapt2' + ext), 'link', '-o', work / 'base.apk', '-I', platform, '--manifest', work / 'AndroidManifest.xml', '--java', work / 'gen', '--min-sdk-version', '28', '--target-sdk-version', '34', '--version-code', '3', '--version-name', '1.2.0', '-A', repo / 'android/app/src/main/assets', work / 'resources.zip')
sources = list((repo / 'android/app/src/main/java/com/artist/king').glob('*.java'))
run(java / ('javac' + ext), '--release', '8', '-encoding', 'UTF-8', '-classpath', platform, '-d', work / 'classes', work / 'gen/com/artist/king/R.java', *sources)
with zipfile.ZipFile(work / 'classes.jar', 'w') as archive:
    for file in (work / 'classes').rglob('*.class'):
        archive.write(file, file.relative_to(work / 'classes'))
run(java / ('java' + ext), '-cp', tools / 'lib/d8.jar', 'com.android.tools.r8.D8', '--lib', platform, '--min-api', '28', '--output', work / 'dex', work / 'classes.jar')
with zipfile.ZipFile(work / 'base.apk', 'a') as archive:
    for file in (work / 'dex').glob('*.dex'):
        archive.write(file, file.name)
run(tools / ('zipalign' + ext), '-f', '4', work / 'base.apk', work / 'aligned.apk')
java_tool('apksigner.jar', 'sign', '--ks', os.environ['KING_KEYSTORE'], '--ks-key-alias', 'king', '--ks-pass', 'env:KING_SIGN_PASS', '--key-pass', 'env:KING_SIGN_PASS', '--out', work / 'KING-Focus.apk', work / 'aligned.apk')
java_tool('apksigner.jar', 'verify', '--verbose', '--print-certs', work / 'KING-Focus.apk')
run(java / ('javac' + ext), '-d', work / 'tests', repo / 'android/app/src/main/java/com/artist/king/GestureRoutes.java', repo / 'tests/GestureRoutesTest.java')
run(java / ('java' + ext), '-cp', work / 'tests', 'com.artist.king.GestureRoutesTest')
print('Built:', work / 'KING-Focus.apk')
