#!/usr/bin/env bash
#
# Manually run inside the Codespace to build a debug APK and send it to
# the Telegram debug chat for testing.
#
# Requires TG_TOKEN and TG_DEBUG as Codespace secrets (same TG_TOKEN bot
# used by the release workflow; TG_DEBUG is the debug-chat destination).

set -euo pipefail

# --- Toolchain (matches .devcontainer/setup.sh) ---
readonly LOCAL_TOOLS_DIR="${HOME}/.local"
export JAVA_HOME="${JAVA_HOME:-${LOCAL_TOOLS_DIR}/jdk-26}"
export ANDROID_HOME="${ANDROID_HOME:-${LOCAL_TOOLS_DIR}/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

: "${TG_TOKEN:?TG_TOKEN is not set}"
: "${TG_DEBUG:?TG_DEBUG is not set}"

readonly APK_DIR="app/build/outputs/apk/debug"
readonly BUILD_TOOLS_VERSION="37.0.0"
readonly AAPT="${ANDROID_HOME}/build-tools/${BUILD_TOOLS_VERSION}/aapt"

if ! command -v zip >/dev/null 2>&1; then
    sudo apt-get update -qq
    sudo apt-get install -y -qq zip
fi

echo "==> Building debug APK"
chmod +x ./gradlew
./gradlew :app:assembleDebug --console=plain

apk_path="$(find "${APK_DIR}" -maxdepth 1 -name '*.apk' | head -1)"
if [[ -z "${apk_path}" ]]; then
    echo "No APK found in ${APK_DIR}" >&2
    exit 1
fi

echo "==> Reading APK metadata"
if [[ ! -x "${AAPT}" ]]; then
    echo "aapt not found at ${AAPT} - check BUILD_TOOLS_VERSION matches your installed build-tools" >&2
    exit 1
fi
badging="$("${AAPT}" dump badging "${apk_path}")"
version_name="$(grep -oP "versionName='\K[^']+" <<<"${badging}")"
version_code="$(grep -oP "versionCode='\K[^']+" <<<"${badging}")"
commit_hash="$(git rev-parse --short HEAD)"
commit_subject="$(git log -1 --pretty=%s)"
build_date="$(date '+%Y-%m-%d %H:%M %Z')"

escape_html() {
    sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' <<<"$1"
}
commit_subject_esc="$(escape_html "${commit_subject}")"

echo "==> Compressing APK"
zip_name="Hail-debug-v${version_name}-${commit_hash}.zip"
zip_path="/tmp/${zip_name}"
rm -f "${zip_path}"
zip -j -9 "${zip_path}" "${apk_path}" >/dev/null
zip_size_mb="$(du -m "${zip_path}" | cut -f1)"
echo "Zip size: ${zip_size_mb} MB"

if (( zip_size_mb >= 50 )); then
    echo "Warning: ${zip_size_mb}MB is at/over Telegram's 50MB bot upload limit; the send may fail." >&2
fi

caption="<b>Hail Debug Build</b>
Version: ${version_name} (${version_code})
Commit: <code>${commit_hash}</code> - ${commit_subject_esc}
Built: ${build_date}"

caption_file="$(mktemp)"
trap 'rm -f "${caption_file}"' EXIT
printf '%s' "${caption}" > "${caption_file}"

echo "==> Uploading to Telegram"
curl -sS --fail \
    -F "chat_id=${TG_DEBUG}" \
    -F "document=@${zip_path}" \
    -F "caption=@${caption_file}" \
    -F "parse_mode=HTML" \
    "https://api.telegram.org/bot${TG_TOKEN}/sendDocument" >/dev/null

rm -f "${zip_path}"
echo "==> Done: sent ${zip_name} (${zip_size_mb} MB)"
