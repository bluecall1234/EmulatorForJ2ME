# Sửa lỗi hiển thị Text (Alignment & Rendering)

Game hiện tại không hiển thị text do `nativeDrawString` ở phía native (C++) chỉ là một hàm rỗng (stub). Ngoài ra, tham số `anchor` (căn lề) cũng chưa được xử lý, dẫn đến vị trí hiển thị có thể bị sai.

## Proposed Changes

### [Component: Graphics & Bridge]

#### [MODIFY] [Graphics.kt](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/Graphics.kt)
- Bổ sung các hằng số `anchor` (HCENTER, VCENTER, LEFT, RIGHT, TOP, BOTTOM, BASELINE).
- Cập nhật `drawString` và `drawSubstring` để truyền `anchor` sang `NativeGraphicsBridge`.

#### [MODIFY] [NativeGraphicsBridge.kt (common)](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/NativeGraphicsBridge.kt)
- Cập nhật chữ ký hàm `drawString` để bao gồm tham số `anchor`.

#### [MODIFY] [NativeGraphicsBridge.kt (android)](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/androidMain/kotlin/emulator/core/api/javax/microedition/lcdui/NativeGraphicsBridge.kt)
- Triển khai `drawString` bằng cách:
  1. Sử dụng `android.graphics.Paint` để đo kích thước văn bản và định nghĩa màu sắc.
  2. Xử lý `anchor` để tính toán tọa độ (x, y) bắt đầu vẽ.
  3. Vẽ văn bản ra một `Bitmap` tạm.
  4. Lấy mảng pixel từ `Bitmap` và gọi `NativeGraphicsBridgeJni.nativeDrawImage` để đẩy lên framebuffer native.

## Verification Plan

### Automated Tests
- Build lại APK và chạy game.
- Kiểm tra các màn hình có text (Menu, Highscores).

### Manual Verification
- So sánh ảnh chụp màn hình game sau khi fix với ảnh `Game_normal.png` do người dùng cung cấp.
