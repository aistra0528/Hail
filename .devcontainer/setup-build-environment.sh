#!/usr/bin/env bash

set -euo pipefail

readonly LOCAL_TOOLS_DIR="${HOME}/.local"
readonly JDK_DIR="${LOCAL_TOOLS_DIR}/jdk-26"
readonly ANDROID_SDK_ROOT="${LOCAL_TOOLS_DIR}/android-sdk"
readonly CMDLINE_TOOLS_DIR="${ANDROID_SDK_ROOT}/cmdline-tools/latest"
readonly JDK_ARCHIVE="/tmp/temurin26-jdk.tar.gz"
readonly SDK_ARCHIVE="/tmp/android-commandline-tools.zip"
readonly JDK_URL="https://github.com/adoptium/temurin26-binaries/releases/download/jdk-26.0.2.1%2B1/OpenJDK26U-jdk_x64_linux_hotspot_26.0.2.1_1.tar.gz"
readonly SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip"

mkdir -p "${LOCAL_TOOLS_DIR}" "${ANDROID_SDK_ROOT}/cmdline-tools" "${HOME}/.gradle"

if [[ ! -x "${JDK_DIR}/bin/java" ]]; then
    curl --fail --location --retry 3 --connect-timeout 20 --max-time 600 \
        -o "${JDK_ARCHIVE}" "${JDK_URL}"
    jdk_extract_dir="$(tar -tzf "${JDK_ARCHIVE}" | head -1 | cut -d/ -f1)"
    rm -rf "${LOCAL_TOOLS_DIR:?}/${jdk_extract_dir}"
    tar -xzf "${JDK_ARCHIVE}" -C "${LOCAL_TOOLS_DIR}"
    ln -sfn "${LOCAL_TOOLS_DIR}/${jdk_extract_dir}" "${JDK_DIR}"
fi

if [[ ! -x "${CMDLINE_TOOLS_DIR}/bin/sdkmanager" ]]; then
    curl --fail --location --retry 3 --connect-timeout 20 --max-time 600 \
        -o "${SDK_ARCHIVE}" "${SDK_URL}"
    rm -rf "${ANDROID_SDK_ROOT}/cmdline-tools/latest" "${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools"
    unzip -q "${SDK_ARCHIVE}" -d "${ANDROID_SDK_ROOT}/cmdline-tools"
    mv "${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools" "${CMDLINE_TOOLS_DIR}"
fi

export JAVA_HOME="${JDK_DIR}"
export ANDROID_HOME="${ANDROID_SDK_ROOT}"
export PATH="${JAVA_HOME}/bin:${CMDLINE_TOOLS_DIR}/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

yes | sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" --licenses >/dev/null
sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" \
    "platform-tools" \
    "platforms;android-36" \
    "platforms;android-37.0" \
    "build-tools;37.0.0" \
    "ndk;28.0.13004108"

profile_file="${HOME}/.bashrc"
profile_marker="# Hail Android build environment"
if ! grep -Fqx "${profile_marker}" "${profile_file}" 2>/dev/null; then
    cat >>"${profile_file}" <<EOF

${profile_marker}
export JAVA_HOME="${JDK_DIR}"
export ANDROID_HOME="${ANDROID_SDK_ROOT}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT}"
export PATH="\${JAVA_HOME}/bin:\${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:\${ANDROID_SDK_ROOT}/platform-tools:\${PATH}"
EOF
fi

git config --global user.name "rahaaatul"
git config --global user.email "rahatulghazi@gmail.com"

printf 'Build environment ready: Java %s, Android SDK %s\n' \
    "$(${JAVA_HOME}/bin/java -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -1)" \
    "${ANDROID_SDK_ROOT}"