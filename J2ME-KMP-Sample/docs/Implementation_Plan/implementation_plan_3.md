# Fix: App Launch Crash — Session 4 (2026-03-06)

**Trạng thái**: Đang lên kế hoạch fix lỗi thiếu Opcode `0x3F`.

---

## Root Cause Analysis

Dựa trên hình ảnh bạn cung cấp và log, app bị crash do lỗi:
`RuntimeException: Unimplemented opcode: 0x3f (unknown_0x3f) at PC=3 in demo/game/MyTestGame.startApp`

Trình thông dịch Bytecode (Interpreter) hiện tại chưa hỗ trợ một số opcode để lưu/đọc biến kiểu `long` (`long` chiếm 2 slot / 8 bytes) trong Stack Frame.
Cụ thể:
- `0x3F` chính là opcode `lstore_0` (lưu giá trị `long` từ stack vào local variable index 0).
- Kéo theo đó, chúng ta cũng đang thiếu các opcode `lstore_1`, `lstore_2`, `lstore_3` và cả `fstore_0`..`fstore_3` (lưu float), v.v.

Trong file `MyTestGame.startApp`, bytecode giả lập gọi `System.currentTimeMillis()` (trả về 1 số `long`), sau đó dùng lệnh `lstore_x` để cất số `long` này vào biến cục bộ. Trình thông dịch không biết lệnh `0x3F` này là gì nên ném ra Exception.

> **Ghi chú thêm**: Tôi cũng đã xuất file log của Android Emulator ra `docs/emulator_crash_log.txt` theo yêu cầu của bạn để bạn có thể xem lại rõ hơn trên PC nhé.

---

## Proposed Changes

Tôi sẽ thêm các Opcodes còn thiếu vào danh sách và xử lý chúng trong `ExecutionEngine`.

### [MODIFY] `Opcodes.kt`

Bổ sung các hằng số bị thiếu theo bảng JVM Specification:
```kotlin
    const val LSTORE_0     = 0x3F  // Store long to local var 0
    const val LSTORE_1     = 0x40  // Store long to local var 1
    const val LSTORE_2     = 0x41  // Store long to local var 2
    const val LSTORE_3     = 0x42  // Store long to local var 3
    ... (và thêm tên của chúng vào hàm nameOf để debug dễ hơn)
```

### [MODIFY] `ExecutionEngine.kt`

Bổ sung xử lý cho các lệnh `LSTORE_x` (và các lệnh nhánh tương tự) vào nhánh `when(opcode)`:
```kotlin
    Opcodes.LSTORE_0 -> frame.locals[0] = frame.popLong()
    Opcodes.LSTORE_1 -> frame.locals[1] = frame.popLong()
    Opcodes.LSTORE_2 -> frame.locals[2] = frame.popLong()
    Opcodes.LSTORE_3 -> frame.locals[3] = frame.popLong()
```

*(Tôi cũng sẽ kiểm tra và thêm các lệnh `LLOAD_x`, `FSTORE_x`, `FLOAD_x`... nếu thiếu để tránh lỗi tương tự khi bytecode phức tạp hơn).*

---

## Verification Plan

1. Sửa code `Opcodes.kt` và `ExecutionEngine.kt`.
2. Chạy `.\gradlew :androidApp:assembleDebug` để đảm bảo không có lỗi biên dịch.
3. Chạy lại app trên Android Emulator. App sẽ vượt qua đoạn `startApp()` mà không còn lỗi `0x3F` nữa.
