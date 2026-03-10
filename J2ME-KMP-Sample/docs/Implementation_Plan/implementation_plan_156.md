# Implementation Plan 156 - Triển khai Display.kt (MIDP 2.0)

Kế hoạch này tập trung vào việc hoàn thiện lớp `Display.kt` để quản lý hiển thị, màu sắc hệ thống và chuyển đổi giữa các màn hình (`Displayable`).

## Proposed Changes

### [J2ME API Layer]

#### [MODIFY] [Display.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/Display.kt)
- Bổ sung các hằng số: `LIST_ELEMENT`, `CHOICE_GROUP_ELEMENT`, `ALERT`, và các hằng số màu `COLOR_*`.
- Triển khai các phương thức:
    - `isColor(frame)`, `numColors(frame)`, `numAlphaLevels(frame)`: Trả về thông số màn hình.
    - `getColor(frame)`: Trả về màu hệ thống dựa trên specifier.
    - `getBestImageHeight(frame)`, `getBestImageWidth(frame)`: Trả về kích thước ảnh tối ưu.
    - `vibrate(frame)`, `flashBacklight(frame)`: Stub hỗ trợ rung và đèn nền.
    - `callSerially(frame)`: Stub hỗ trợ thực thi tuần tự.
    - `setCurrent(frame)`: Cập nhật để gọi `hideNotify()` trên màn hình cũ và `showNotify()` trên màn hình mới.
    - `getCurrent(frame)`: Trả về màn hình hiện tại.

#### [MODIFY] [NativeMethodBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- Cập nhật hàm `registerJavaxMicroeditionLcduiDisplay` để đăng ký toàn bộ các phương thức mới.

## Verification Plan

### Automated Tests
- Kiểm tra việc chuyển đổi `activeDisplayable` khi gọi `setCurrent`.
- Kiểm tra các giá trị trả về của `isColor`, `numColors` để đảm bảo API hoạt động đúng.

### Manual Verification
- Chạy emulator và kiểm tra log khi game thực hiện đổi màn hình hoặc truy vấn màu hệ thống.
