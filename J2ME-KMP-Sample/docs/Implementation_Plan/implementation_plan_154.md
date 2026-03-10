# Implementation Plan 154 - Hoàn thiện Canvas.kt (MIDP 2.0)

Kế hoạch này tập trung vào việc bổ sung các hằng số và phương thức còn thiếu trong `javax.microedition.lcdui.Canvas` để hỗ trợ xử lý phím bấm và các thuộc tính màn hình.

## Proposed Changes

### [J2ME API Layer]

#### [MODIFY] [Canvas.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/Canvas.kt)
- Bổ sung hằng số Game Action: `UP`, `DOWN`, `LEFT`, `RIGHT`, `FIRE`, `GAME_A`, `GAME_B`, `GAME_C`, `GAME_D`.
- Bổ sung hằng số Key Code: `KEY_NUM0` đến `KEY_NUM9`, `KEY_STAR`, `KEY_POUND`.
- Triển khai các hàm:
    - `getGameAction(frame)`: Ánh xạ keyCode sang mã hành động game.
    - `getKeyCode(frame)`: Ánh xạ hành động game ngược lại keyCode.
    - `getKeyName(frame)`: Trả về tên phím.
    - `hasPointerEvents`, `hasPointerMotionEvents`, `hasRepeatEvents`: Trả về khả năng hỗ trợ (mặc định true).
    - `isDoubleBuffered`: Trả về true.
    - `getWidth`, `getHeight`: Trả về kích thước màn hình (stub hoặc lấy từ Display).
    - `repaint(x, y, w, h)`: Overload hỗ trợ vẽ vùng cụ thể.

#### [MODIFY] [NativeMethodBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- Cập nhật hàm `registerJavaxMicroeditionLcduiCanvas` để ánh xạ các phương thức mới.

## Verification Plan

### Automated Tests
- Build lại project.
- Chạy "Bounce Tales" hoặc game J2ME bất kỳ và kiểm tra xem các phím bấm có bắt đầu hoạt động không (nếu engine game gọi `getGameAction`).

### Manual Verification
- Kiểm tra log debug khi nhấn phím trên emulator.
