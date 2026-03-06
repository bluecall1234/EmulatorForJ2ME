# 📁 docs/ — Tài liệu dự án J2ME Emulator

Thư mục này lưu toàn bộ tài liệu kỹ thuật của dự án **J2ME KMP Emulator**.

## Cấu trúc

| File | Mô tả | Cập nhật khi |
|------|--------|--------------|
| `implementation_plan.md` | Kế hoạch kỹ thuật chi tiết cho từng tính năng/fix | Trước khi bắt đầu implement |
| `walkthrough.md` | Kết quả sau khi hoàn thành — code diff, build results | Sau khi fix/implement xong |
| `chat_history.md` | Lịch sử toàn bộ phiên làm việc, quyết định thiết kế | Cuối mỗi phiên làm việc |

## Quy trình cập nhật

```
1. Bắt đầu tính năng/fix → cập nhật implementation_plan.md
2. Hoàn thành → cập nhật walkthrough.md (thêm section mới)
3. Kết thúc phiên → cập nhật chat_history.md (thêm Session mới)
```

## Trạng thái dự án

| Phase | Nội dung | Trạng thái |
|-------|----------|------------|
| Phase 1 | Native Interop (JNI + C-Interop + SDL2) | ✅ Hoàn thành |
| Phase 2 | Bytecode Interpreter | ✅ Hoàn thành |
| Phase 3 | J2ME APIs (CLDC + MIDP) | ✅ Hoàn thành (bugs fixed) |
| Phase 4 | Compose App Shell + SurfaceView + Virtual Keyboard | ⏳ Chưa bắt đầu |
| Phase 5 | Optimization (FastArrayHeapObject) | ⏳ Chưa bắt đầu |
