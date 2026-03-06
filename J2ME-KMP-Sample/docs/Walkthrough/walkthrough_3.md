# Walkthrough: Fix Opcodes — Session 4

**Ngày**: 2026-03-06
**Kết quả**: ✅ BUILD SUCCESSFUL

---

## Bug `Unimplemented opcode: 0x3f`

**Nguyên nhân:**
Opcode `0x3F` (`LSTORE_0`) và các opcode thao tác lưu biến `long`, `float` đang bị thiếu trong bảng hằng số `Opcodes.kt` và logic `ExecutionEngine.kt`. Cụ thể là:
- `LSTORE_0` (0x3F) - `LSTORE_3` (0x42)
- `FSTORE_0` (0x43) - `FSTORE_3` (0x46)

**Thay đổi đã thực hiện:**
- Bổ sung tên hằng số và `nameOf` mapper vào `Opcodes.kt`.
- Bổ sung luồng điều hướng (execution flow) bằng `frame.pop()` và lưu vào mảng `frame.locals[]` trong `ExecutionEngine.kt`.

**Kết quả Build:**
- Build successful. Lỗi bytecode stop crash ở PC=3 thuộc `MyTestGame.startApp` đã được giải quyết.
