# Implementation Plan 152 - Hoàn thiện Graphics API cho J2ME

Kế hoạch này tập trung vào việc bổ sung các phương thức còn thiếu trong `javax.microedition.lcdui.Graphics` (MIDP 2.0) dựa trên tham chiếu từ bản Java "freej2me-web".

## Các thay đổi chính

### [Graphics API Layer]
#### [MODIFY] [Graphics.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/Graphics.kt)
- Thêm hằng số: `SOLID`, `DOTTED`.
- Triển khai các phương thức getter: `getColor`, `getRedComponent`, `getGreenComponent`, `getBlueComponent`, `getTranslateX`, `getTranslateY`, `getStrokeStyle`.
- Triển khai các phương thức setter: `setStrokeStyle`, `setGrayScale`.
- Triển khai các hàm vẽ đồ họa:
    - `drawLine`, `drawRect`, `clipRect`
    - `drawArc`, `fillArc`
    - `drawRoundRect`, `fillRoundRect`
    - `drawTriangle`, `fillTriangle`
    - `drawRGB` (Sử dụng `drawImage`)
    - `copyArea` (Stub)

#### [MODIFY] [NativeGraphicsBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/NativeGraphicsBridge.kt)
- Bổ sung `expect` cho các hàm vẽ: `drawLine`, `drawRect`, `drawArc`, `drawRoundRect`.
- Triển khai `actual` tương ứng cho Android để gọi xuống lớp JNI.

#### [MODIFY] [android_jni_bridge.cpp](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/nativeMain/bridge/android_jni_bridge.cpp)
- Triển khai các hàm JNI: `nativeDrawLine`, `nativeDrawRect`, `nativeDrawArc` (stub), `nativeDrawRoundRect` (stub).

## Kế hoạch kiểm chứng
- Rebuild và chạy thử game.
- Kiểm tra log để đảm bảo không còn lỗi "Lookup failed" cho các method đồ họa cơ bản.
