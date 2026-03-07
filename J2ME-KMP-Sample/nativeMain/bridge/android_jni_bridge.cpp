#include <android/log.h>
#include <android/native_window_jni.h>
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define LOG_TAG "J2ME_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

/**
 * JNI Bridge for the J2ME Emulator - Native Window Edition (Phase 4)
 */

// ============================================================
// Software Framebuffer & Native Window
// ============================================================
static uint32_t *gFramebuffer = nullptr;
static int gWidth = 240;
static int gHeight = 320;
static bool gInitialized = false;

static ANativeWindow *gWindow = nullptr;

// ============================================================
// Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeInitSDL
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeInitSDL(
    JNIEnv *env, jobject /* this */, jint width, jint height) {

  if (gInitialized) {
    LOGI("nativeInitSDL: already initialized.");
    return;
  }

  gWidth = width;
  gHeight = height;
  gFramebuffer = (uint32_t *)malloc(width * height * sizeof(uint32_t));

  for (int i = 0; i < width * height; i++) {
    gFramebuffer[i] = 0xFF000000; // clear to black
  }
  gInitialized = true;
  LOGI("nativeInitSDL: Software framebuffer %dx%d initialized.", width, height);
}

// ============================================================
// nativeSetSurface
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeSetSurface(
    JNIEnv *env, jobject /* this */, jobject surface) {

  if (gWindow != nullptr) {
    ANativeWindow_release(gWindow);
    gWindow = nullptr;
  }

  if (surface != nullptr) {
    gWindow = ANativeWindow_fromSurface(env, surface);
    if (gWindow != nullptr) {
      // Hardware Composer will scale the buffer mathematically to the screen
      // size
      ANativeWindow_setBuffersGeometry(gWindow, gWidth, gHeight,
                                       WINDOW_FORMAT_RGBA_8888);
      LOGI("nativeSetSurface: Connected ANativeWindow (%dx%d buffer geometry)",
           gWidth, gHeight);
    } else {
      LOGE("nativeSetSurface: Failed to get ANativeWindow from surface");
    }
  } else {
    LOGI("nativeSetSurface: Released ANativeWindow");
  }
}

// ============================================================
// nativeClearScreen
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeClearScreen(
    JNIEnv *env, jobject /* this */) {

  if (!gInitialized || gFramebuffer == nullptr) {
    LOGE("nativeClearScreen: not initialized!");
    return;
  }
  for (int i = 0; i < gWidth * gHeight; i++) {
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

  if (!gInitialized || gFramebuffer == nullptr) {
    LOGE("nativePresentScreen: not initialized!");
    return;
  }

  if (gWindow == nullptr) {
    return;
  }

  ANativeWindow_Buffer buffer;
  if (ANativeWindow_lock(gWindow, &buffer, nullptr) == 0) {
    LOGI("nativePresentScreen: Locked window (%dx%d, stride %d)", buffer.width,
         buffer.height, buffer.stride);

    auto *dest = (uint32_t *)buffer.bits;
    auto *src = gFramebuffer;

    int copyHeight = (gHeight < buffer.height) ? gHeight : buffer.height;
    int copyWidth = (gWidth < buffer.width) ? gWidth : buffer.width;

    for (int y = 0; y < copyHeight; y++) {
      memcpy(dest + (y * buffer.stride), src + (y * gWidth),
             copyWidth * sizeof(uint32_t));
    }
    ANativeWindow_unlockAndPost(gWindow);
  } else {
    LOGE("nativePresentScreen: Failed to lock ANativeWindow");
  }
}

// ============================================================
// nativeFillRect
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeFillRect(
    JNIEnv *env, jobject /* this */, jint x, jint y, jint w, jint h, jint r,
    jint g, jint b, jint a) {

  if (!gInitialized || gFramebuffer == nullptr) {
    LOGE("nativeFillRect: not initialized!");
    return;
  }

  // Pack ARGB8888
  uint32_t color = ((uint32_t)(a & 0xFF) << 24) | ((uint32_t)(r & 0xFF) << 16) |
                   ((uint32_t)(g & 0xFF) << 8) | ((uint32_t)(b & 0xFF));

  // Clamp to framebuffer bounds
  int x0 = (x < 0) ? 0 : x;
  int y0 = (y < 0) ? 0 : y;
  int x1 = ((x + w) > gWidth) ? gWidth : (x + w);
  int y1 = ((y + h) > gHeight) ? gHeight : (y + h);

  for (int py = y0; py < y1; py++) {
    for (int px = x0; px < x1; px++) {
      gFramebuffer[py * gWidth + px] = color;
    }
  }
  LOGD("nativeFillRect(%d,%d,%d,%d) color=0x%08X OK", x, y, w, h, color);
}

// ============================================================
// nativeSetClip
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeSetClip(
    JNIEnv *env, jobject /* this */, jint x, jint y, jint w, jint h) {
  // Stub for now. Full clipping requires modifying fillRect/drawImage
  LOGD("nativeSetClip(%d,%d,%d,%d) Stubbed", x, y, w, h);
}

// ============================================================
// nativeDrawImage
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeDrawImage(
    JNIEnv *env, jobject /* this */, jintArray imageRgb, jint imgW, jint imgH,
    jint x, jint y, jint anchor) {

  if (!gInitialized || gFramebuffer == nullptr || imageRgb == nullptr)
    return;

  jint *pixels = env->GetIntArrayElements(imageRgb, nullptr);
  jsize len = env->GetArrayLength(imageRgb);

  if (len < imgW * imgH) {
    env->ReleaseIntArrayElements(imageRgb, pixels, JNI_ABORT);
    return;
  }

  // Adjust X/Y based on anchor (stubbed to Top-Left for now)
  int startX = x;
  int startY = y;

  for (int py = 0; py < imgH; py++) {
    for (int px = 0; px < imgW; px++) {
      int screenX = startX + px;
      int screenY = startY + py;

      if (screenX >= 0 && screenX < gWidth && screenY >= 0 &&
          screenY < gHeight) {
        uint32_t color = pixels[py * imgW + px];
        // Simple alpha blending: if alpha is > 0
        uint8_t a = (color >> 24) & 0xFF;
        if (a > 128) {
          gFramebuffer[screenY * gWidth + screenX] = color;
        }
      }
    }
  }

  env->ReleaseIntArrayElements(imageRgb, pixels, JNI_ABORT);
  LOGD("nativeDrawImage rendered %dx%d into (%d,%d)", imgW, imgH, x, y);
}

// ============================================================
// nativeDrawString
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeDrawString(
    JNIEnv *env, jobject /* this */, jstring text, jint x, jint y, jint color) {
  // Full TTF rendering via FreeType/SDL_ttf is heavy. We stub or draw
  // rectangles for now.
  LOGD("nativeDrawString at (%d,%d) color=0x%08X", x, y, color);
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
