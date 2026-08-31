#!/usr/bin/env bash
#
# Sends release notification to Telegram with APK files.
# Called from release.yml with APK paths as args.
#
# Environment variables:
#   TG_TOKEN     - bot token
#   VERSION      - version string (e.g. 1.11.3)
#   RELEASE_URL  - GitHub release URL
#   RELEASE_TYPE - "release" or "pre-release"
#
# Hardcoded destinations:
#   TG_GROUP = -1004396394059
#   RELEASE_TOPIC = 85
#   PRE_RELEASE_TOPIC = 95
#
# Never fails the calling workflow: any problem prints a warning and exits 0,
# since a failed notification shouldn't block a successful release.

set -uo pipefail

: "${TG_TOKEN:?TG_TOKEN is not set}"
: "${VERSION:?VERSION is not set}"
: "${RELEASE_URL:?RELEASE_URL is not set}"

readonly TG_GROUP="-1004396394059"
readonly RELEASE_TOPIC="85"
readonly PRE_RELEASE_TOPIC="95"

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

echo "==> TG_GROUP: ${TG_GROUP}"
echo "==> RELEASE_TOPIC: ${RELEASE_TOPIC}"
echo "==> Version: ${VERSION}"
echo "==> Release URL: ${RELEASE_URL}"
echo "==> APK files: ${apk_files[*]}"

# Determine target topic
RELEASE_TYPE="${RELEASE_TYPE:-release}"
if [[ "$RELEASE_TYPE" == "pre-release" ]]; then
    target_topic="${PRE_RELEASE_TOPIC}"
else
    target_topic="${RELEASE_TOPIC}"
fi

echo "==> Release type: ${RELEASE_TYPE}"
echo "==> Target topic: ${target_topic}"

format_changelog() {
    local text="$1"
    text="${text//&/\&amp;}"
    text="${text//</\&lt;}"
    text="${text//>/\&gt;}"
    text="$(echo "$text" | sed 's/\*\*\([^*]*\)\*\*/\<b\>\1\<\/b\>/g')"
    text="$(echo "$text" | sed 's/^- /• /')"
    printf '%s' "$text"
}

extract_changelog_section() {
    local section="$1"
    local text=""
    if [[ -f "CHANGELOG.md" ]]; then
        text="$(sed -n "/^## \[${VERSION}\]/,/^## \[/p" CHANGELOG.md | sed -n "/^### ${section}/,/^### /p" | sed '1d;$d' | sed '/^$/d')"
    fi
    echo "$text"
}

changelog_sections=()
highlights="$(extract_changelog_section "Highlights")"
added="$(extract_changelog_section "Added")"
changed="$(extract_changelog_section "Changed")"
fixed="$(extract_changelog_section "Fixed")"
removed="$(extract_changelog_section "Removed")"

if [[ -n "$highlights" ]]; then
    changelog_sections+=("<b>Highlights</b>")
    changelog_sections+=("<blockquote expandable>$(format_changelog "$highlights")</blockquote>")
fi
if [[ -n "$added" ]]; then
    changelog_sections+=("<b>Added</b>")
    changelog_sections+=("<blockquote expandable>$(format_changelog "$added")</blockquote>")
fi
if [[ -n "$changed" ]]; then
    changelog_sections+=("<b>Changed</b>")
    changelog_sections+=("<blockquote expandable>$(format_changelog "$changed")</blockquote>")
fi
if [[ -n "$fixed" ]]; then
    changelog_sections+=("<b>Fixed</b>")
    changelog_sections+=("<blockquote expandable>$(format_changelog "$fixed")</blockquote>")
fi
if [[ -n "$removed" ]]; then
    changelog_sections+=("<b>Removed</b>")
    changelog_sections+=("<blockquote expandable>$(format_changelog "$removed")</blockquote>")
fi

changelog_text=""
if [[ ${#changelog_sections[@]} -gt 0 ]]; then
    changelog_text="$(printf '%s\n' "${changelog_sections[@]}")"
fi

if [[ -n "$changelog_text" ]]; then
    echo "==> Changelog excerpt loaded"
fi

send_notification() {
    local topic_id="$1"
    local label="$2"

    echo "==> Sending release notification to ${label} (chat=${TG_GROUP} topic=${topic_id:-none})"

    local caption="<b>Version</b>
<blockquote>${VERSION}</blockquote>"

    if [[ -n "$changelog_text" ]]; then
        caption="${caption}

${changelog_text}"
    fi

    caption="${caption}

<b>See more</b>
<blockquote><a href=\"${RELEASE_URL}\">${RELEASE_URL}</a></blockquote>"

    local doc_args=(-F "chat_id=${TG_GROUP}")
    if [[ -n "$topic_id" ]]; then
        doc_args+=(-F "message_thread_id=${topic_id}")
    fi

    local http_code=""
    if [[ ${#apk_files[@]} -eq 1 ]]; then
        response="$(curl -sS -w '\n%{http_code}' \
            "${doc_args[@]}" \
            -F "document=@${apk_files[0]}" \
            --form-string "caption=${caption}" \
            --form-string "parse_mode=HTML" \
            "https://api.telegram.org/bot${TG_TOKEN}/sendDocument" 2>&1)" || true
    else
        local media_args=()
        local media_items=()
        for i in "${!apk_files[@]}"; do
            local field="f${i}"
            media_args+=(-F "${field}=@${apk_files[$i]}")
            if [[ "$i" -eq 0 ]]; then
                media_items+=("{\"type\":\"document\",\"media\":\"attach://${field}\",\"caption\":\"${caption}\",\"parse_mode\":\"HTML\"}")
            else
                media_items+=("{\"type\":\"document\",\"media\":\"attach://${field}\"}")
            fi
        done
        local media_json="[$(IFS=,; echo "${media_items[*]}")]"

        response="$(curl -sS -w '\n%{http_code}' \
            "${doc_args[@]}" \
            -F "media=${media_json}" \
            "${media_args[@]}" \
            "https://api.telegram.org/bot${TG_TOKEN}/sendMediaGroup" 2>&1)" || true
    fi

    local http_code="$(tail -n1 <<<"${response}")"
    local body="$(sed '$d' <<<"${response}")"

    if [[ "${http_code}" != "200" ]]; then
        echo "::warning::tg_release.sh: Telegram document to ${label} failed (HTTP ${http_code:-unknown}): ${body}"
    else
        echo "==> Sent ${#apk_files[@]} APK(s) to ${label}"
    fi
}

send_notification "${target_topic}" "release topic"

echo "==> Done: release Telegram notification completed"
