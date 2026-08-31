#!/usr/bin/env bash
#
# Manually run inside the Codespace to build a debug APK and send it to
# the Telegram debug chat for testing.
#
# Requires TG_TOKEN and TG_GROUP as Codespace secrets (same TG_TOKEN bot
# used by the release workflow; TG_GROUP is the chat destination).
#
# Hardcoded debug topic: 84

set -euo pipefail

# --- Toolchain (matches .devcontainer/setup.sh) ---
readonly LOCAL_TOOLS_DIR="${HOME}/.local"
export JAVA_HOME="${JAVA_HOME:-${LOCAL_TOOLS_DIR}/jdk-26}"
export ANDROID_HOME="${ANDROID_HOME:-${LOCAL_TOOLS_DIR}/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

: "${TG_TOKEN:?TG_TOKEN is not set}"
: "${TG_GROUP:?TG_GROUP is not set}"

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
commit_hash_full="$(git rev-parse HEAD)"
commit_subject="$(git log -1 --pretty=%s)"
build_date="$(date '+%Y-%m-%d %H:%M %Z')"

escape_html() {
    sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' <<<"$1"
}
commit_subject_esc="$(escape_html "${commit_subject}")"

echo "==> Compressing APK"
debug_apk_name="Hail-v${version_name}-g${commit_hash}-debug.apk"
debug_apk_path="/tmp/${debug_apk_name}"
cp "${apk_path}" "${debug_apk_path}"
zip_name="Hail-v${version_name}-g${commit_hash}-debug.zip"
zip_path="/tmp/${zip_name}"
rm -f "${zip_path}"
zip -j -9 "${zip_path}" "${debug_apk_path}" >/dev/null
zip_size_mb="$(du -m "${zip_path}" | cut -f1)"
echo "Zip size: ${zip_size_mb} MB"

if (( zip_size_mb >= 50 )); then
    echo "Warning: ${zip_size_mb}MB is at/over Telegram's 50MB bot upload limit; the send may fail." >&2
fi

commit_url="https://github.com/rahaaatul/Hail/commit/${commit_hash_full}"
echo "==> Commit URL: ${commit_url}"
echo "==> TG_GROUP: ${TG_GROUP}"
echo "==> TG_DEBUG_TOPIC: 84"

send_debug_notification() {
    local topic_id="$1"
    local label="$2"

    echo "==> Sending debug notification to ${label} (chat=${TG_GROUP} topic=${topic_id:-none})"

    local doc_args=(-F "chat_id=${TG_GROUP}")
    if [[ -n "$topic_id" ]]; then
        doc_args+=(-F "message_thread_id=${topic_id}")
    fi

    local caption="<b>Debug Build v${version_name}-${commit_hash}</b>

<b>Version</b>
<blockquote>${version_name}-${commit_hash} (${version_code})</blockquote>

<b>Description</b>
<blockquote expandable>${commit_subject_esc}</blockquote>

<b>See more</b>
<blockquote><a href=\"${commit_url}\">${commit_hash}</a></blockquote>"

    local response="$(curl -sS -w '\n%{http_code}' \
        "${doc_args[@]}" \
        -F "document=@${zip_path}" \
        --form-string "caption=${caption}" \
        --form-string "parse_mode=HTML" \
        "https://api.telegram.org/bot${TG_TOKEN}/sendDocument" 2>&1)" || true

    local http_code="$(tail -n1 <<<"${response}")"
    local body="$(sed '$d' <<<"${response}")"

    if [[ "${http_code}" != "200" ]]; then
        echo "::warning::tg_debug.sh: Telegram document to ${label} failed (HTTP ${http_code:-unknown}): ${body}"
    else
        echo "==> Sent debug document to ${label}"
    fi
}

send_debug_notification "84" "debug topic"

rm -f "${zip_path}" "${debug_apk_path}"
echo "==> Done: sent ${zip_name} (${zip_size_mb} MB)"
