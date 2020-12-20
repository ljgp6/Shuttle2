cd ./exoplayer
EXOPLAYER_ROOT="$(pwd)"
NDK_PATH="C:\\Users\\tim\\AppData\\Local\\Android\\Sdk\\ndk\\21.3.6528147"

# Fetch latest exoplayer
git pull

# Update FLAC
FLAC_EXT_PATH="${EXOPLAYER_ROOT}/extensions/flac/src/main"
cd "${FLAC_EXT_PATH}/jni" && \
curl https://ftp.osuosl.org/pub/xiph/releases/flac/flac-1.3.2.tar.xz | tar xJ && \
mv flac-1.3.2 flac
cd "${FLAC_EXT_PATH}"/jni && \
${NDK_PATH}\\ndk-build.cmd APP_ABI=all -j4

# Update Opus
OPUS_EXT_PATH="${EXOPLAYER_ROOT}/extensions/opus/src/main"
cd "${OPUS_EXT_PATH}/jni" && \
git pull
cd ${OPUS_EXT_PATH}/jni && ./convert_android_asm.sh
### Note: There's an issue where libopus\celt\arm\celt_pitch_xcorr_arm_gnu.s is incorrectly generate - see line 34, remove linebreak before "
echo "** Not complete!"
echo "** Fix line break issue, then re-comment and run remainder of update-exoplayer-win.sh"
#cd "${OPUS_EXT_PATH}"/jni && \
#${NDK_PATH}\\ndk-build.cmd APP_ABI=all -j4
$SHELL