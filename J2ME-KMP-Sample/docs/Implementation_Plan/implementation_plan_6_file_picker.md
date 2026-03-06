# Kế hoạch triển khai Phase 4.3: App Shell - Game Thư Viện (File Picker)

**Mục tiêu**: Thay vì hardcode để chạy một file class duy nhất giả lập (`JanLoader` tải `MyTestGame`), ứng dụng cần một giao diện thư viện game đơn giản và một nút "Thêm Game" (Add Game) để người dùng chọn tệp `.jar` từ thiết bị, sau đó tải game lên khung mô phỏng J2ME.

---

## 1. Giao diện Thư viện Game (Compose UI)

Mở rộng `MainActivity.kt` hiện tại để hỗ trợ cấu trúc đa màn hình đơn giản (State-based Navigation).

- **Trạng thái Màn hình**: Định nghĩa enum `AppScreen { LIBRARY, EMULATOR }`.
- **Màn hình Library (Thư viện)**:
  - Hiển thị danh sách các game đã thêm. Mỗi mục trong danh sách có nút "Chơi" và "Xoá". Nút "Xoá" sẽ xoá file `.jar` tương ứng khỏi bộ nhớ nội bộ của máy và cập nhật lại danh sách hiển thị.
  - Một nút Floating Action Button (FAB) góc dưới bên phải ghi chữ "+" hoặc "Add Game".
- **Màn hình Emulator (Chơi game hiện tại)**:
  - Component `EmulatorScreen` và `VirtualKeypad` đã hoàn thiện trước đó. 
  - Sẽ được kích hoạt khi người dùng nhấn vào một game trong Library. Bổ sung nút "Back" để thoát game và quay lại thư viện.

## 2. Truy cập File `.jar` bằng System File Picker (Android Storage Access Framework)

Khi người dùng nhấn nút "Add Game", ứng dụng sẽ mở File Picker mặc định của Android để cho phép chọn file có đuôi `.jar`.

- **ActivityResultContracts.GetContent()**: Sử dụng compose activity result launcher: `rememberLauncherForActivityResult(ActivityResultContracts.GetContent())`.
- Mimetype cần lọc: `application/java-archive` hoặc `*/*`.

## 3. Tải Game & Chuyển giao cho Interpreter

Sau khi thu được `Uri` của file `.jar` từ File Picker:
1. Copy file đó vào thư mục ẩn nội bộ của App (Ví dụ: `context.filesDir/games/`) để có thể truy xuất dễ dàng lần sau mà không lo người dùng xoá hay di chuyển mất file gốc.
2. Lưu danh sách đường dẫn các game đã thêm vào bộ nhớ (Có thể dùng tạm một mảng `List<String>`).
3. Chỉnh sửa hàm `loader.loadClassFromJar()`: Thay vì đọc từ tài nguyên `res` hardcode, hãy cấu hình `JarLoader` nhận vào đường dẫn tệp trên Device Storage (mở `FileInputStream` hoặc luồng byte). Kế đến, phải giải nén tệp JAR, quét file `META-INF/MANIFEST.MF` để đọc thuộc tính `MIDlet-1` nhằm tìm ra class khởi chạy chính (EntryPoint) ví dụ `demo.game.MyTestGame`.
4. Gọi hàm `startGameLoop(classFile)` kèm class chính yếu vừa detect được thay vì cứng nhắc gọi `<init>` rồi tới `startApp`.

---

## Các bước thực hiện (Verification Plan)

1. **Bước 1**: Cấp quyền/Xử lý IO (Không cần Permission Read_External_Storage phức tạp nếu dùng SAF launcher của Android).
2. **Bước 2**: Code `File Picker Launcher` bằng Composable.
3. **Bước 3**: Cập nhật logic của `JarLoader` để parse `.jar` thực tế và đọc Manifest.
4. **Bước 4**: Nối luồng Compose -> Xử lý giải nén -> Chuyển màn hình Emulator -> Kích hoạt Execution Engine.
5. **Bước 5**: Chạy ứng dụng và Add một file `.jar` J2ME thực tế nhỏ để Test!
