# Phân tích Source Code J2ME-Loader

## 1. Tổng quan kiến trúc
J2ME-Loader là một emulator J2ME cho Android cực kỳ mạnh mẽ với các thành phần chính:

- **Chuyển đổi Bytecode**: Sử dụng thư viện `dexlib` để dịch mã Java (.class) từ file JAR sang Dalvik (.dex). Điều này giúp game chạy gần như tốc độ native trên Android bằng cách tận dụng máy ảo ART/Dalvik.
- **API Runtime**: Triển khai lại toàn bộ thư viện `javax.microedition.*` bằng Android API.
- **Đồ họa**:
  - 2D: Dùng Android `Canvas` và `View`.
  - 3D (M3G): Sử dụng JNI liên kết với thư viện C++ để render bằng OpenGL.
  - Mascot Capsule 3D: Implement bằng C++ (open-source của woesss).
- **Hệ thống phím**: Virtual Keyboard có khả năng tùy biến cao, map các event chạm màn hình thành mã phím (Key Codes) của điện thoại Java cũ.

## 2. Các điểm hạn chế khi muốn chạy Cross-platform
- **Phụ thuộc vào Android Runtime**: Code hiện tại phụ thuộc chặt chẽ vào các class của Android như `Context`, `Canvas`, `Bitmap`, `DexClassLoader`.
- **Cơ chế DEX**: Chỉ hoạt động trên Android. iOS không hiểu định dạng file DEX và không có máy ảo thực thi nó.
- **UI Framework**: Giao diện cài đặt và quản lý game được viết bằng XML Android Layouts truyền thống.

## 3. Chiến lược cho Emulator mới (Android & iOS)
Như đã phân tích trong kế hoạch chuyển đổi, việc sử dụng **Kotlin Multiplatform (KMP)** là hướng đi khả thi nhất để tận dụng lại logic của J2ME-Loader mà vẫn đảm bảo chạy được trên iOS.

### Điểm mấu chốt:
Phải thay thế cơ chế `DexClassLoader` (DEX) bằng một **Java Bytecode Interpreter** viết bằng mã Native hoặc Kotlin thuần để có thể chạy trên trình biên dịch Native của iOS.
