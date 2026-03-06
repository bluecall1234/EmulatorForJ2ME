#include <android/log.h>
#include <jni.h>
#include <stdint.h>


#define LOG_TAG "J2ME_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

/**
 * JNI Bridge for the J2ME Emulator - Software Framebuffer Edition
 *
 * FIX #2: JNI function names updated to match the Kotlin class
 * "NativeGraphicsBridgeJni" which is a Kotlin internal object in the
 * `emulator.core.api.javax.microedition.lcdui` package. The JNI naming
 * convention for Kotlin objects is the same as Java classes:
 *           Java_{package}_{ClassName}_{methodName}
 *
 * FIX #3: SDL2 requires an ANativeWindow (SurfaceView) to create a renderer.
 *         Since Phase 4 (SurfaceView integration) is not done yet, we use a
 *         software pixel buffer (offline framebuffer) as a safe placeholder.
 *         The Kotlin interpreter can call all graphics commands without
 * crashing. When Phase 4 is ready, replace these stubs with real SDL2 calls.
 */

// ============================================================
// Software Framebuffer (offline render target)
// ============================================================
#define FB_WIDTH 240
#define FB_HEIGHT 320

static uint32_t gFramebuffer[FB_WIDTH * FB_HEIGHT];
static bool gInitialized = false;

// ============================================================
// Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeInitSDL
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeInitSDL(
    JNIEnv *env, jobject /* this */) {

  if (gInitialized) {
    LOGI("nativeInitSDL: already initialized.");
    return;
  }

  for (int i = 0; i < FB_WIDTH * FB_HEIGHT; i++) {
    gFramebuffer[i] = 0xFF000000; // clear to black
  }
  gInitialized = true;
  LOGI("nativeInitSDL: Software framebuffer %dx%d initialized.", FB_WIDTH,
       FB_HEIGHT);
  // NOTE: Real SDL2 init requires ANativeWindow from a SurfaceView.
  // This will be wired up in Phase 4 (SurfaceView + SDL2 integration).
}

// ============================================================
// nativeClearScreen
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeClearScreen(
    JNIEnv *env, jobject /* this */) {

  if (!gInitialized) {
    LOGE("nativeClearScreen: not initialized!");
    return;
  }
  for (int i = 0; i < FB_WIDTH * FB_HEIGHT; i++) {
    gFramebuffer[i] = 0xFF000000;
  }
  LOGD("nativeClearScreen: cleared to black.");
}

// ============================================================
// nativePresentScreen
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativePresentScreen(
    JNIEnv *env, jobject /* this */) {

  if (!gInitialized) {
    LOGE("nativePresentScreen: not initialized!");
    return;
  }
  // Phase 4: blit gFramebuffer → ANativeWindow / SurfaceView here.
  LOGI("nativePresentScreen: frame ready (Phase 4: blit to SurfaceView).");
}

// ============================================================
// nativeFillRect
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeFillRect(
    JNIEnv *env, jobject /* this */, jint x, jint y, jint w, jint h, jint r,
    jint g, jint b, jint a) {

  if (!gInitialized) {
    LOGE("nativeFillRect: not initialized!");
    return;
  }

  // Pack ARGB8888
  uint32_t color = ((uint32_t)(a & 0xFF) << 24) | ((uint32_t)(r & 0xFF) << 16) |
                   ((uint32_t)(g & 0xFF) << 8) | ((uint32_t)(b & 0xFF));

  // Clamp to framebuffer bounds
  int x0 = (x < 0) ? 0 : x;
  int y0 = (y < 0) ? 0 : y;
  int x1 = ((x + w) > FB_WIDTH) ? FB_WIDTH : (x + w);
  int y1 = ((y + h) > FB_HEIGHT) ? FB_HEIGHT : (y + h);

  for (int py = y0; py < y1; py++) {
    for (int px = x0; px < x1; px++) {
      gFramebuffer[py * FB_WIDTH + px] = color;
    }
  }
  LOGD("nativeFillRect(%d,%d,%d,%d) color=0x%08X OK", x, y, w, h, color);
}

// ============================================================
// Legacy bridge — kept for backward compat with old NativeBridge.initSDL()
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_com_example_j2me_android_NativeBridge_initSDL(JNIEnv *env,
                                                   jobject /* this */) {
  LOGI("Legacy NativeBridge.initSDL called (use NativeGraphicsBridgeJni "
       "instead).");
}
