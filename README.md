<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
</h2>
<h2 align="center">
   Quản lý sách - thư viện qua mạng
</h2>
<div align="center">
    <p align="center">
        <img width="170" alt="aiotlab_logo" src="https://github.com/user-attachments/assets/41ef702b-3d6e-4ac4-beac-d8c9a874bca9" />
        <img width="180" alt="fitdnu_logo" src="https://github.com/user-attachments/assets/ec4815af-e477-480b-b9fa-c490b74772b8" />
        <img width="200" alt="dnu_logo" src="https://github.com/user-attachments/assets/2bcb1a6c-774c-4e7d-b14d-8c53dbb4067f" />
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

## 1. Giới thiệu hệ thống

📚 **Đề tài:** Quản lý sách - thư viện qua mạng

Hệ thống quản lý sách - thư viện qua mạng được xây dựng nhằm hỗ trợ các hoạt động quản lý, tra cứu, mượn/trả sách trong môi trường hiện đại, thân thiện và bảo mật.  
Ứng dụng giúp kết nối giữa người dùng và quản trị viên thông qua giao diện trực quan, dễ sử dụng, đồng thời đảm bảo dữ liệu luôn được cập nhật và đồng bộ.

### 🎯 **Mục tiêu hệ thống**
- Tối ưu hóa quy trình quản lý sách, người dùng, hoạt động mượn/trả.
- Đáp ứng nhu cầu tra cứu, mượn/trả sách nhanh chóng, chính xác.
- Hỗ trợ phân quyền (admin, user) để đảm bảo bảo mật và hiệu quả vận hành.
- Cung cấp giao diện hiện đại, dễ sử dụng, phù hợp với nhiều đối tượng người dùng.

### 📊 **Sơ đồ cấu trúc tổng quan**

Dưới đây là sơ đồ kiến trúc tổng quan của hệ thống quản lý thư viện:

```mermaid
graph LR
    subgraph "Client Layer"
        A[User Interface<br/>Swing UI]
    end
    
    subgraph "Server Layer"
        B[ClientHandler]
        C[Session Manager]
        D[Rate Limiter]
        E[REST API Handler<br/>with JSON Parser]
        F[Recommendation Engine]
        G[Cloud Integration]
        H[Metrics Collector]
    end
    
    subgraph "Data Layer"
        I[Enhanced DAO Layer]
        J((SQLite Database))
    end
    
    A -->|"TCP Socket"| B
    B --> I
    I --> J
    C --> B
    D --> B
    E --> B
    F --> B
    G --> B
    H --> B
    
    style A fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    style B fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    style C fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    style D fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    style E fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    style F fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    style G fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    style H fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    style I fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    style J fill:#ffebee,stroke:#c62828,stroke-width:2px
```

**Giải thích sơ đồ:**
- **Client**: Giao diện người dùng (Swing) cho admin và user.
- **Server**: Xử lý logic nghiệp vụ, kết nối TCP.
- **ClientHandler**: Quản lý kết nối từ client.
- **Enhanced DAO Layer**: Truy cập dữ liệu với các phương thức nâng cao.
- **SQLite Database**: Cơ sở dữ liệu lưu trữ thông tin.
- **Session Manager**: Quản lý phiên đăng nhập.
- **Rate Limiter**: Giới hạn tốc độ yêu cầu để bảo mật.
- **REST API Handler**: Cung cấp API HTTP.
- **JSON Parser**: Xử lý dữ liệu JSON.
- **Recommendation Engine**: Đề xuất sách thông minh.
- **Cloud Integration**: Tích hợp đám mây.
- **Metrics Collector**: Thu thập số liệu hiệu suất.

### 🖥️ **Chức năng của Server**
- 🗄️ Quản lý dữ liệu người dùng, sách, mượn/trả, hoạt động.
- 🔗 Xử lý các yêu cầu từ Client: đăng nhập, đăng ký, tìm kiếm, mượn/trả sách, quản lý yêu thích, lịch sử hoạt động.
- 🔒 Đảm bảo an toàn và đồng bộ dữ liệu
- ✅ Ưu tiên hiển thị `expected_return_date` từ `borrow_requests` khi có (hỗ trợ due-date chính xác).
- ⏱️ Job định kỳ (scheduler) để tự động đánh dấu quá hạn, gửi nhắc trả và cập nhật trạng thái.
- 🔐 Audit log cho các hành động quan trọng (mượn/trả, duyệt yêu cầu, thay đổi user) kèm admin_id và timestamp.
- 🔁 Migration & seed scripts có quản lý (Init/Update) để nâng cấp schema an toàn.
- 🔂 Connection pooling và giao dịch an toàn (transaction) để tránh tranh chấp khi nhiều client.
- 🔍 Validation & rate-limiting (chống SQL injection, kiểm tra đầu vào) và tùy chọn mã hóa kênh (TLS) cho bảo mật liên lạc.

### 👤 **Chức năng của Client**
- 🔑 Đăng nhập, đăng ký tài khoản.
- 🔍 Tìm kiếm sách, xem thông tin chi tiết, mượn/trả sách.
- ❤️ Quản lý sách yêu thích, xem lịch sử hoạt động, hóa đơn mượn trả.
- ⏱️ **Debounce tối ưu**: Giảm 80-90% database queries khi nhập liệu (xem `DEBOUNCE_FEATURE.md`)
- 🛠️ Quản lý người dùng (dành cho admin).
- 🌓 Dark Mode toàn bộ app (Client + Admin) — giữ màu riêng cho các nút call-to-action.
- 🖼️ Hiển thị avatar người dùng và preview ảnh bìa sách (upload / drag & drop / thumbnail lazy-load).
- ✋ Luồng yêu cầu mượn rõ ràng: chỉ tạo `borrow_request` khi user bấm Submit (không tự động khi đóng dialog).
- ⚠️ Highlight hàng sắp đến hạn (1–3 ngày) và quá hạn (màu/tooltip/icon) trong danh sách mượn.
- 📊 Thống kê nâng cao: cards + textual/graphical charts + recent activities + export (CSV/PDF).
- ⏭️ Pagination, caching trang sách và lazy-load ảnh để trải nghiệm mượt trên mạng chậm.
- ⌨️ Phím tắt & accessibility: Enter/Esc, focus order, labels rõ ràng.
- 📂 Import/Export dữ liệu (CSV) và backup profile/avatar.

### ⚙️ **Chức năng hệ thống**
- 📚 Quản lý sách, người dùng, hoạt động mượn/trả, yêu thích.
- 🛡️ Phân quyền người dùng (admin, user).
- 🖼️ Giao diện thân thiện, dễ sử dụng, sinh động với icon và màu sắc.
- 💾 Sao lưu định kỳ (scheduled DB backups) và cơ chế restore dễ dùng.
- 📈 Health-check & monitoring (uptime, last-backup, queue length) + logs tập trung.
- ✅ CI + unit/integration tests cho DAO, server handler và UI smoke tests.
- 🔐 Mã hóa mật khẩu (bcrypt/argon2), sanitize uploads và chính sách giữ dữ liệu (archive/cleanup sau N tháng).
- ⚙️ Script build/deploy và hướng dẫn chạy (server + client), kèm tùy chọn package/installer.
- 🔔 Hệ thống thông báo (email / in-app) với retry logic và lịch gửi thông minh.
- 🧭 RBAC chi tiết cho admin (quyền theo chức năng) và audit trail cho truy vấn cao cấp.

## 2. Công nghệ sử dụng

- ☕ Java (Swing, JDBC)
- 🗃️ SQLite
- 🌐 TCP Socket (Client/Server)
- 🖼️ GroupLayout, GridBagLayout (giao diện responsive)
- 📦 Các thư viện Java chuẩn

## 3. Hình ảnh các chức năng

**Hình 1: <img src="https://img.icons8.com/ios-filled/24/000000/login-rounded-right.png"/> Giao diện đăng nhập**
<p align="center">
    <img width="478" height="838" alt="image" src="https://github.com/user-attachments/assets/456c4de1-5260-4c31-b35a-84f76979ffb5" />
</p>

**Hình 2: <img src="https://img.icons8.com/ios-filled/24/000000/login-rounded-right.png"/> Giao diện đăng ký**
<p align="center">
   <img width="566" height="940" alt="image" src="https://github.com/user-attachments/assets/49a795a6-99e3-49bc-8d45-86b8a5a308de" />
   </p>

**Hình 3: <img src="https://img.icons8.com/ios-filled/24/000000/books.png"/> Giao diện đăng nhập admin**
<p align="center">
  <img width="534" height="743" alt="image" src="https://github.com/user-attachments/assets/4ad5b180-833a-499e-931c-225d3d5d785f" />
</p>

**Hình 4: <img src="https://img.icons8.com/ios-filled/24/000000/user-group-man-man.png"/> Giao diện quản lý người dùng**
<p align="center">
   <img width="1919" height="1018" alt="image" src="https://github.com/user-attachments/assets/0dfdb2f8-8879-4021-82d9-e5d0d1a44705" />
</p>

**Hình 5: <img src="https://img.icons8.com/ios-filled/24/000000/borrow-book.png"/> Giao diện quản lý admin**
<p align="center">
<img width="1919" height="1020" alt="image" src="https://github.com/user-attachments/assets/cce3c268-ddc4-4dd8-80f0-3bbc58db56fc" />
</p>

**Hình 6: <img src="https://img.icons8.com/ios-filled/24/000000/borrow-book.png"/> Giao diện quản lý sách**
<p align="center">
    <img width="1916" height="1019" alt="image" src="https://github.com/user-attachments/assets/0e8ce591-cc20-465e-9422-5c8aece5c5a1" />
</p> 

**Hình 7: <img src="https://img.icons8.com/ios-filled/24/000000/invoice.png"/> Giao diện quản lý người dùng**
<p align="center">
  <img width="1484" height="988" alt="image" src="https://github.com/user-attachments/assets/c61007d0-9066-49d8-bd7a-57db681c05e4" />
</p>

**Hình 8: <img src="https://img.icons8.com/ios-filled/24/000000/invoice.png"/> Giao diện đăng ký mượn sách**
<p align="center">
 <img width="1474" height="985" alt="image" src="https://github.com/user-attachments/assets/f57d1a19-a4ac-4d44-9d45-158495090f41" />
</p>

**Hình 9: <img src="https://img.icons8.com/ios-filled/24/000000/invoice.png"/> Giao diện quản lý đăng ký mượn**
<p align="center">
   <img width="1485" height="982" alt="image" src="https://github.com/user-attachments/assets/f5b9bf1c-50a3-4a34-a7cc-14c4598a5845" />
</p>

## 4. Các tính năng enterprise mới

Hệ thống đã được nâng cấp với 23 tính năng enterprise để đáp ứng các yêu cầu hiện đại của quản lý thư viện:

### 🔐 Bảo mật và Xác thực
- **Mã hóa mật khẩu**: Sử dụng SHA-256 với salt để bảo mật thông tin người dùng
- **Quản lý phiên**: Token-based session management với timeout tự động
- **Giới hạn tốc độ**: Rate limiting để chống DDoS và bảo vệ server

### 🌐 API và Tích hợp
- **REST API Layer**: Cung cấp HTTP endpoints cho tích hợp với các hệ thống khác
- **Xử lý JSON**: Parsing và serialization dữ liệu JSON hiệu quả
- **Tích hợp đám mây**: Mô phỏng tích hợp AWS cho lưu trữ và sao lưu

### 🤖 Trí tuệ nhân tạo
- **Động cơ đề xuất AI**: Sử dụng collaborative filtering để gợi ý sách
- **Lọc dựa trên nội dung**: Phân tích thể loại sách để đề xuất phù hợp
- **Học sở thích người dùng**: Thu thập đánh giá ngầm để cải thiện đề xuất

### 🌍 Quốc tế hóa
- **Đa ngôn ngữ**: Hỗ trợ 5 ngôn ngữ khác nhau
- **Tự động phát hiện ngôn ngữ**: Điều chỉnh giao diện theo ngôn ngữ người dùng

### 🏗️ Kiến trúc doanh nghiệp
- **Kiến trúc microservices**: Chia nhỏ hệ thống thành các dịch vụ độc lập
- **Cấu hình Kubernetes**: Sẵn sàng triển khai trên container orchestration
- **Pooling kết nối cơ sở dữ liệu**: Tối ưu hóa hiệu suất và quản lý tài nguyên

### 📊 Giám sát và Hiệu suất
- **Giám sát hiệu suất**: Thu thập metrics về hoạt động hệ thống
- **Thu thập số liệu**: Theo dõi các chỉ số quan trọng
- **Quản lý cấu hình**: Cấu hình tập trung và linh hoạt

### 🔄 Sao lưu và Khôi phục
- **Hệ thống sao lưu**: Sao lưu tự động và thủ công
- **Quản lý cấu hình**: Cấu hình sao lưu và khôi phục dễ dàng

### 🔍 Tìm kiếm nâng cao
- **Công cụ tìm kiếm nâng cao**: Tìm kiếm thông minh với nhiều tiêu chí
- **Xử lý lỗi và ghi log**: Ghi log chi tiết cho debugging và audit

### 🛡️ Mô hình bảo mật doanh nghiệp
- **Kiến trúc có thể mở rộng**: Thiết kế để xử lý tải lớn và mở rộng dễ dàng

## 5. Hướng dẫn cài đặt và sử dụng

### Yêu cầu hệ thống

- 💻 Máy tính cài đặt Java 8 trở lên
- 🗃️ SQLite JDBC driver
- 🖥️ Hệ điều hành Windows (khuyến nghị)

### Cài đặt và triển khai

1. 📥 Clone hoặc tải source code về máy.
2. 🏗️ Chạy file `InitDatabase.java` để khởi tạo CSDL SQLite tại `C:/data/library.db`.
3. 🚀 Chạy `LibraryServer.java` để khởi động server.
4. 🖱️ Chạy `MainApp.java` để mở giao diện client.

### Sử dụng ứng dụng

- 🔑 Đăng nhập bằng tài khoản admin mặc định:  
  - Tên đăng nhập: `admin`  
  - Mật khẩu: `admin`
- 📝 Đăng ký tài khoản mới nếu chưa có.
- 📚 Quản lý sách, người dùng, mượn/trả, yêu thích, hóa đơn qua giao diện.

## 6. Thông tin liên hệ

- **👤 Họ tên:** Lâm Ngọc Tú
- **🏫 Lớp:** CNTT 16-01
- **✉️ Email:** lamngoctuk55@gmail.com
