# Walkthrough 150: Triển khai các Opcode thiếu và sửa lỗi Graphics

Tôi đã khắc phục thành công chuỗi lỗi crash liên quan đến các Opcode chưa được triển khai trong Interpreter và một lỗi nghiêm trọng trong hàm xử lý đồ họa.

## Các thay đổi chính

### 1. Bổ sung các Opcode Stack Manipulation
- Triển khai toàn bộ các biến thể của `dup2`:
  - `DUP_X2` (0x5B)
  - `DUP2`   (0x5C) - Nguyên nhân gây crash ban đầu.
  - `DUP2_X1` (0x5D)
  - `DUP2_X2` (0x5E)
- Các opcode này được xử lý linh hoạt tùy theo kiểu dữ liệu (Category 1: int/ref, Category 2: long/double) để đảm bảo stack luôn chính xác.

### 2. Bổ sung các Opcode Tính toán (Arithmetic)
- **Long**: `LDIV`, `LREM` (0x71 - lỗi tiếp theo sau khi fix dup2), `LNEG`.
- **Float/Double**: `FADD`, `DADD`, `FSUB`, `DSUB`, `FMUL`, `DMUL`, `FDIV`, `DDIV`, `FREM`, `DREM`, `FNEG`, `DNEG`.

### 3. Bổ sung các Opcode Chuyển đổi kiểu dữ liệu (Conversion)
- Triển khai đầy đủ dải opcode từ 0x85 đến 0x90: `I2D`, `L2I`, `L2F`, `L2D`, `F2I`, `F2L`, `F2D`, `D2I`, `D2L`, `D2F`.

### 4. Sửa lỗi logic trong `Graphics.drawSubstring`
- Phát hiện và sửa lỗi thứ tự pop tham số từ stack (anchor, y, x, len, offset).
- Fix lỗi crash `StringIndexOutOfBoundsException` do truyền sai giá trị offset và length.
- Đồng bộ hóa cách truy xuất chuỗi từ `HeapObject` thông qua trường `value:[C]`.

## Kết quả kiểm chứng

- **Build Status**: Thành công.
- **Runtime Log**: Lỗi `Unimplemented opcode` cho 0x5c và 0x71 đã biến mất hoàn toàn.
- **Game Status**: Game đã vận hành được vòng lặp đồ họa, thực hiện các lệnh `drawRegion` để hiển thị hình ảnh. Không còn gặp crash tại `drawSubstring`.

![Ảnh chụp màn hình Game Library sau khi build thành công](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/game_screenshot_final_v2.png)

> [!NOTE]
> Game hiện tại đã vượt qua được giai đoạn khởi tạo ban đầu và đang thực thi logic render. Nếu gặp thêm opcode lạ nào khác, tôi sẽ tiếp tục bổ sung.
