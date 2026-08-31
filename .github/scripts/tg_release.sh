#!/usr/bin/env bash
#
# Sends release notification to Telegram:
#   1. Logo as photo with title + "See changelog" link
#   2. APK files as grouped album (no caption)
# Called from release.yml with APK paths as args.
#
# Environment variables:
#   TG_TOKEN      - bot token
#   TG_RELEASE    - chat ID
#   VERSION       - version string (e.g. 1.11.3)
#   RELEASE_URL   - GitHub release URL
#   TG_LOGO       - path to logo image
#   RELEASE_TYPE  - "release" or "pre-release"
#
# Never fails the calling workflow: any problem prints a warning and exits 0,
# since a failed notification shouldn't block a successful release.

set -uo pipefail

: "${TG_TOKEN:?TG_TOKEN is not set}"
: "${TG_RELEASE:?TG_RELEASE is not set}"
: "${VERSION:?VERSION is not set}"
: "${RELEASE_URL:?RELEASE_URL is not set}"
: "${TG_LOGO:?TG_LOGO is not set}"

apk_files=("$@")

if [[ ${#apk_files[@]} -eq 0 ]]; then
    echo "::warning::tg_release.sh: no APK files given, skipping Telegram notification"
    exit 0
fi

for f in "${apk_files[@]}"; do
    if [[ ! -f "$f" ]]; then
        echo "::warning::tg_release.sh: file not found: $f - skipping Telegram notification"
        exit 0
    fi
done

# Determine title prefix
RELEASE_TYPE="${RELEASE_TYPE:-release}"
if [[ "$RELEASE_TYPE" == "pre-release" ]]; then
    title="Pre-release v${VERSION}"
else
    title="Release v${VERSION}"
fi

# Message 1: Logo photo with title + "See changelog" link
caption="<b>${title}</b>
<a href=\"${RELEASE_URL}\">See changelog</a>"

response="$(curl -sS -w '\n%{http_code}' \
    -F "chat_id=${TG_RELEASE}" \
    -F "photo=@${TG_LOGO}" \
    --form-string "caption=${caption}" \
    --form-string "parse_mode=HTML" \
    "https://api.telegram.org/bot${TG_TOKEN}/sendPhoto" 2>&1)" || true

http_code="$(tail -n1 <<<"${response}")"
body="$(sed '$d' <<<"${response}")"

if [[ "${http_code}" != "200" ]]; then
    echo "::warning::tg_release.sh: Telegram photo notification failed (HTTP ${http_code:-unknown}): ${body}"
    exit 0
fi

echo "==> Sent logo photo to Telegram"

# Message 2: APK files (sendDocument for 1 file, sendMediaGroup for 2+)
if [[ ${#apk_files[@]} -eq 1 ]]; then
    # sendMediaGroup requires 2-10 items; fall back to a single document
    response="$(curl -sS -w '\n%{http_code}' \
        -F "chat_id=${TG_RELEASE}" \
        -F "document=@${apk_files[0]}" \
        "https://api.telegram.org/bot${TG_TOKEN}/sendDocument" 2>&1)" || true
else
    curl_args=()
    media_items=()
    for i in "${!apk_files[@]}"; do
        field="f${i}"
        curl_args+=(-F "${field}=@${apk_files[$i]}")
        media_items+=("{\"type\":\"document\",\"media\":\"attach://${field}\"}")
    done
    media_json="[$(IFS=,; echo "${media_items[*]}")]"

    response="$(curl -sS -w '\n%{http_code}' \
        -F "chat_id=${TG_RELEASE}" \
        -F "media=${media_json}" \
        "${curl_args[@]}" \
        "https://api.telegram.org/bot${TG_TOKEN}/sendMediaGroup" 2>&1)" || true
fi

http_code="$(tail -n1 <<<"${response}")"
body="$(sed '$d' <<<"${response}")"

if [[ "${http_code}" != "200" ]]; then
    echo "::warning::tg_release.sh: Telegram APK notification failed (HTTP ${http_code:-unknown}): ${body}"
    exit 0
fi

echo "==> Sent ${#apk_files[@]} APK(s) to Telegram"
