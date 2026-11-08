<div align="center">

<img src="https://capsule-render.vercel.app/api?type=venom&color=0:0F2027,50:2E86DE,100:8E44AD&height=260&section=header&text=TMDT&fontSize=96&fontColor=ffffff&fontAlignY=42&animation=twinkling&desc=Tutor%20Marketplace%20%E2%80%A2%20Android%20%C3%97%20Firebase%20%C3%97%20VNPay&descSize=20&descAlignY=66" alt="TMDT banner" width="100%"/>

<a href="https://github.com/pvhNam/TMDT_CK">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=21&duration=3200&pause=900&color=36D1DC&center=true&vCenter=true&width=700&lines=T%C3%ACm+gia+s%C6%B0+ph%C3%B9+h%E1%BB%A3p+trong+v%C3%A0i+ch%E1%BA%A1m+%F0%9F%8E%93;%C4%90%C4%83ng+k%C3%BD+l%E1%BB%9Bp+%E2%80%A2+Thanh+to%C3%A1n+%E2%80%A2+Qu%E1%BA%A3n+l%C3%BD+h%E1%BB%93+s%C6%A1+%F0%9F%9A%80;Android+%C3%97+Firebase+%C3%97+VNPay+%F0%9F%94%A5" alt="Typing SVG" />
</a>

<br/><br/>

![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-JDK%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Gradle](https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![VNPay](https://img.shields.io/badge/VNPay-TEST-0A5CA8?style=for-the-badge)

![Status](https://img.shields.io/badge/Status-Active%20Development-2E86DE?style=flat-square&logo=githubactions&logoColor=white)
![PRs](https://img.shields.io/badge/PRs-Welcome-brightgreen?style=flat-square)
![Made with](https://img.shields.io/badge/Made%20with-%E2%9D%A4%EF%B8%8F%20in%20Vietnam-E74C3C?style=flat-square)
![Stars](https://img.shields.io/github/stars/pvhNam/TMDT_CK?style=flat-square&logo=github&color=F1C40F)
![Last commit](https://img.shields.io/github/last-commit/pvhNam/TMDT_CK?style=flat-square&color=8E44AD)
![Repo size](https://img.shields.io/github/repo-size/pvhNam/TMDT_CK?style=flat-square&color=27AE60)

<br/>

**[🚀 Bắt đầu](#-bắt-đầu-nhanh) &nbsp;•&nbsp; [✨ Tính năng](#-tính-năng) &nbsp;•&nbsp; [🏗️ Kiến trúc](#️-kiến-trúc) &nbsp;•&nbsp; [⚙️ Cấu hình](#️-cấu-hình-môi-trường) &nbsp;•&nbsp; [🤝 Phát triển](#-quy-trình-phát-triển) &nbsp;•&nbsp; [🩺 Sự cố](#-xử-lý-sự-cố)**

</div>

<img src="https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/rainbow.png" width="100%" alt="divider"/>

> [!NOTE]
> **TMDT** là ứng dụng Android dành cho **học viên** và **gia sư**. Giao diện theo thiết kế Figma (`thiết kế giao diện mobile_1.docx`); dữ liệu rút gọn từ `03_Thiet_ke_CSDL_v4.md`, chỉ giữ các luồng nghiệp vụ đang dùng.

<!--
  ẢNH CHỤP MÀN HÌNH — bỏ comment và thay đường dẫn khi đã có ảnh:

  <div align="center">
    <img src="docs/screenshots/home.png"    width="22%" alt="Trang chủ"/>
    <img src="docs/screenshots/profile.png" width="22%" alt="Cá nhân"/>
    <img src="docs/screenshots/tutor.png"   width="22%" alt="Đăng ký gia sư"/>
    <img src="docs/screenshots/payment.png" width="22%" alt="Thanh toán"/>
  </div>
-->

## 🌟 Điểm nổi bật

<table>
<tr>
<td align="center" width="33%">

### 🔎 Tìm kiếm thông minh
Tìm **không dấu**, lọc theo môn, danh sách gia sư **cập nhật thời gian thực** từ Firestore.

</td>
<td align="center" width="33%">

### 🎓 Chuyển vai trò linh hoạt
Học viên đăng ký làm gia sư, chờ duyệt `PENDING`, rồi **chuyển qua lại** hai vai trò ngay trong app.

</td>
<td align="center" width="33%">

### 💳 Thanh toán có chữ ký
VNPay TEST trong WebView, chỉ ghi nhận thành công khi phản hồi có **chữ ký hợp lệ**.

</td>
</tr>
<tr>
<td align="center">

### 🛡️ Phân quyền chặt chẽ
Firestore Rules theo **UID**, chặn nâng quyền và chặn client sửa danh mục gia sư.

</td>
<td align="center">

### 🧼 Không dữ liệu giả
Database trống thì hiển thị trống — ứng dụng **không** chèn danh sách giả.

</td>
<td align="center">

### 🧪 Kiểm thử an toàn
Auth và Rules chạy trên **Emulator localhost**, không ghi dữ liệu lên Firebase thật.

</td>
</tr>
</table>

<img src="https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/rainbow.png" width="100%" alt="divider"/>

## 📚 Mục lục

<table>
<tr>
<td valign="top">

1. [🌟 Điểm nổi bật](#-điểm-nổi-bật)
2. [✨ Tính năng](#-tính-năng)
3. [👥 Vai trò](#-vai-trò-người-dùng)
4. [🏗️ Kiến trúc](#️-kiến-trúc)

</td>
<td valign="top">

5. [🚀 Bắt đầu nhanh](#-bắt-đầu-nhanh)
6. [⚙️ Cấu hình môi trường](#️-cấu-hình-môi-trường)
7. [🤝 Quy trình phát triển](#-quy-trình-phát-triển)
8. [🧪 Kiểm thử](#-kiểm-thử)

</td>
<td valign="top">

9. [🔐 Bảo mật](#-bảo-mật)
10. [🩺 Xử lý sự cố](#-xử-lý-sự-cố)
11. [🗺️ Lộ trình](#️-lộ-trình)
12. [💬 Liên hệ](#-đóng-góp-và-liên-hệ)

</td>
</tr>
</table>

---

## ✨ Tính năng

| | Hạng mục | Mô tả | Trạng thái |
| :-: | --- | --- | :-: |
| 🔑 | **Xác thực** | Email/Password, xác nhận mật khẩu, lưu phiên, quên mật khẩu qua email | ✅ Hoàn tất |
| 🏠 | **Trang chủ** | Banner, tìm kiếm không dấu, lọc theo môn, danh sách gia sư thời gian thực | ✅ Hoàn tất |
| 👤 | **Hồ sơ cá nhân** | Thông tin, địa chỉ, số điện thoại, ảnh đại diện, cấp học, mục tiêu | ✅ Hoàn tất |
| 🎓 | **Đăng ký gia sư** | Nhập → xem lại → gửi; hồ sơ `PENDING` chờ duyệt; chuyển đổi vai trò | ✅ Hoàn tất |
| 💳 | **Thanh toán** | VNPay TEST trong WebView, chỉ ghi nhận khi phản hồi có chữ ký hợp lệ | ✅ Hoàn tất |
| 🛡️ | **Phân quyền dữ liệu** | Firestore Rules theo UID, chặn nâng quyền, chặn client sửa danh mục | ✅ Hoàn tất |
| 📱 | **Xác thực điện thoại** | OTP, chức năng bổ sung | ⚙️ Tùy chọn |
| 📅 | **Lịch học** | Màn hình chờ | 🚧 Đang phát triển |
| 💬 | **Tin nhắn** | Màn hình chờ | 🚧 Đang phát triển |

### 🎓 Hành trình đăng ký làm gia sư

```mermaid
journey
    title Từ học viên đến gia sư
    section Đăng ký
      Nhập thông tin: 5: Học viên
      Xem lại hồ sơ: 4: Học viên
      Gửi lên Firebase: 5: Học viên
    section Xét duyệt
      Hồ sơ ở trạng thái PENDING: 3: Quản trị viên
      Quản trị viên duyệt: 5: Quản trị viên
    section Sử dụng
      Chuyển qua gia sư: 5: Gia sư
      Chuyển lại học viên: 4: Gia sư
```

### 💸 Luồng thanh toán

```mermaid
sequenceDiagram
    autonumber
    actor HV as 🎒 Học viên
    participant App as 📱 Ứng dụng
    participant VNP as 💳 VNPay TEST
    actor GS as 🎓 Gia sư
    actor QT as 🛠️ Quản trị viên

    rect rgb(232, 244, 253)
    HV->>App: Thanh toán đăng ký đã duyệt
    App->>VNP: Mở cổng thanh toán (WebView)
    VNP-->>App: Kết quả kèm chữ ký hợp lệ
    end
    rect rgb(233, 249, 239)
    GS->>App: Báo hoàn thành buổi học
    HV->>App: Xác nhận hoàn thành
    end
    rect rgb(243, 232, 253)
    QT->>App: Lập bảng chi trả theo tháng
    end
```

<details>
<summary><b>🗑️ Đã loại bỏ khỏi phạm vi (bấm để xem)</b></summary>

<br/>

Tin đăng / ứng tuyển / lời mời · minh chứng bằng cấp · hợp đồng chống dạy ngoài · học thử / điểm danh · đánh giá · cấu hình hệ thống.

Chi tiết: [FIRESTORE_SCHEMA.md](FIRESTORE_SCHEMA.md)

</details>

## 👥 Vai trò người dùng

| Vai trò | Có thể làm gì |
| :-: | --- |
| 🎒 **Học viên** | Tìm kiếm không dấu, lọc theo môn, xem hồ sơ gia sư, quản lý hồ sơ cá nhân, thanh toán học phí |
| 🎓 **Gia sư** | Đăng ký hồ sơ gia sư, chờ duyệt, chuyển đổi vai trò, báo hoàn thành buổi học |
| 🛠️ **Quản trị viên** | Duyệt hồ sơ gia sư, quản lý danh mục gia sư/môn dạy, lập bảng chi trả theo tháng |

## 🏗️ Kiến trúc

```mermaid
flowchart LR
    subgraph Client["📱 Android App"]
        UI["🧩 Fragment + XML"]:::ui
        ST["🧠 AccountState<br/>CatalogState"]:::state
        VAL["✔️ AccountValidation"]:::val
        UI --> ST
        UI --> VAL
    end
    ST -->|Auth| FA["🔑 Firebase<br/>Authentication"]:::fb
    ST -->|Read / Write| FS[("🔥 Cloud<br/>Firestore")]:::fb
    FS -.->|"🛡️ Rules theo UID"| ST
    UI -->|Thanh toán| VNP["💳 VNPay TEST<br/>WebView"]:::pay
    VNP -->|"Phản hồi có chữ ký"| UI

    classDef ui fill:#E8F4FD,stroke:#2E86DE,color:#0B3D66,stroke-width:2px;
    classDef state fill:#E9F9EF,stroke:#27AE60,color:#0F4D27,stroke-width:2px;
    classDef val fill:#FDEDEC,stroke:#E74C3C,color:#641E16,stroke-width:2px;
    classDef fb fill:#FFF8DC,stroke:#F1C40F,color:#5F4B00,stroke-width:2px;
    classDef pay fill:#FFF4E0,stroke:#F39C12,color:#6B4400,stroke-width:2px;
```

### 🧰 Công nghệ

<div align="center">

<img src="https://skillicons.dev/icons?i=java,androidstudio,firebase,gradle,nodejs,git,github&perline=7" alt="Tech stack" />

</div>

| Lớp | Công nghệ |
| --- | --- |
| 📱 Client | Android (Java), Fragment + XML, minSdk Android 10 |
| 🔨 Build | Gradle (Kotlin DSL), JDK 21 |
| 🔑 Xác thực | Firebase Authentication |
| 🔥 Dữ liệu | Cloud Firestore + Security Rules |
| 💳 Thanh toán | VNPay TEST |
| 🧪 Tooling | Firebase Emulator, Node.js scripts |

### 📁 Cấu trúc repository

```text
.
├── 📱 app/                      Mã nguồn ứng dụng Android
├── ✉️  email-otp-server/         Máy chủ hỗ trợ gửi OTP qua email
├── 🧪 scripts/                  Kiểm thử Firebase, kiểm tra cấu hình VNPay
├── 🔏 signing/                  Cấu hình ký ứng dụng
├── 🗄️  legacy/                   Mã mô phỏng cũ (ngoài source set)
├── 🛡️  firestore.rules           Security Rules
├── 📇 firestore.indexes.json    Chỉ mục Firestore
├── 🔥 firebase.json             Cấu hình Firebase CLI / Emulator
└── 💳 vnpay.properties.example  Mẫu cấu hình VNPay
```

<details>
<summary><b>🧩 Thành phần mã nguồn chính (bấm để xem)</b></summary>

<br/>

| File | Trách nhiệm |
| --- | --- |
| `MainActivity.java` | Host Fragment, điều hướng, kiểm tra đăng nhập |
| `ui/home/HomeFragment.java` | Trang chủ (`fragment_home.xml`) |
| `ui/auth/*.java` | Đăng nhập, đăng ký, quên mật khẩu, OTP |
| `ui/profile/*.java` | Trang cá nhân, chỉnh sửa hồ sơ |
| `CatalogState.java` | Lắng nghe danh mục Firestore, nối môn và học phí |
| `AccountState.java` | Firebase Auth, lưu/tải hồ sơ, xử lý lỗi |
| `UserProfile.java` | Ánh xạ trường dữ liệu |
| `AccountValidation.java` | Kiểm tra và chuẩn hóa đầu vào |

Mỗi màn hình có Fragment và XML riêng — xem [SCREEN_STRUCTURE.md](SCREEN_STRUCTURE.md).

</details>

<img src="https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/rainbow.png" width="100%" alt="divider"/>

## 🚀 Bắt đầu nhanh

### 📋 Yêu cầu

| Công cụ | Phiên bản |
| --- | --- |
| 🤖 Android Studio | Bản ổn định mới |
| ☕ JDK | 21 |
| 📦 Android SDK | Theo cấu hình dự án |
| 📲 Thiết bị / máy ảo | Android 10 trở lên |
| 🟢 Node.js | Dùng cho script kiểm thử |

### ⬇️ Cài đặt

```bash
git clone https://github.com/pvhNam/TMDT_CK.git
cd TMDT_CK
```

1. Mở thư mục bằng Android Studio và đợi Gradle sync.
2. Hoàn tất [cấu hình Firebase](#-firebase).
3. Sao chép `vnpay.properties.example` thành `vnpay.properties`, điền thông tin TEST ([xem VNPay](#-vnpay-test)).
4. Chọn thiết bị và nhấn **Run** ▶️.

### 🔨 Build bằng dòng lệnh

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

📦 Đầu ra: `app/build/outputs/apk/debug/app-debug.apk`

## ⚙️ Cấu hình môi trường

### 🔥 Firebase

**Project:** `tmdt-3e274`

- [ ] Bật **Email/Password** trong Authentication
- [ ] **Publish Firestore Rules** từ `firestore.rules`
- [ ] Nhập danh mục gia sư và môn dạy theo [schema](FIRESTORE_SCHEMA.md)

> [!WARNING]
> `google-services.json` chỉ cấu hình ứng dụng. File này **không** đồng nghĩa rules đã được publish hoặc phương thức đăng nhập đã được bật trên Console.

Hồ sơ `users/{UID}` được tạo khi đăng ký thành công. Chi tiết: [FIREBASE_SETUP.md](FIREBASE_SETUP.md).

### 💳 VNPay TEST

Tạo `vnpay.properties` ở thư mục gốc (cùng cấp `gradlew.bat`):

```properties
VNPAY_TMN_CODE=MA_WEBSITE_TEST
VNPAY_HASH_SECRET=KHOA_CHECKSUM_TEST
VNPAY_RETURN_URL=https://tmdt.example/vnpay/return
```

Kiểm tra cấu hình (không in secret):

```bash
node scripts/check-vnpay-config.mjs
.\gradlew.bat :app:checkVnpayConfig :app:assembleDebug
```

| | Nguyên tắc | Nội dung |
| :-: | --- | --- |
| 🔗 | Return URL | WebView bắt trực tiếp trong app, không cần website hoạt động ở địa chỉ này |
| 🔄 | Thay đổi cấu hình | Cần build và cài lại ứng dụng |
| 🧾 | Dữ liệu giao dịch | Collection `sandbox_` lưu giao dịch từ cổng TEST, tách khỏi sổ tiền thật |
| 📦 | Bản release | Không chứa secret TEST, tắt nhánh ký trực tiếp trong app |

> [!CAUTION]
> `vnpay.properties` chứa khóa bí mật và đã được Git bỏ qua. **Tuyệt đối không commit** file này.

Chi tiết: [PAYMENT_SETUP.md](PAYMENT_SETUP.md).

## 🤝 Quy trình phát triển

> [!IMPORTANT]
> Các quy ước dưới đây là chuẩn làm việc đề xuất cho nhóm; điều chỉnh theo thực tế trước khi áp dụng.

### 🌿 Nhánh

```mermaid
gitGraph
    commit id: "main ổn định"
    branch feature/ten-ngan
    checkout feature/ten-ngan
    commit id: "feat: tính năng mới"
    commit id: "test: bổ sung kiểm thử"
    checkout main
    branch fix/ten-ngan
    checkout fix/ten-ngan
    commit id: "fix: sửa lỗi"
    checkout main
    merge feature/ten-ngan id: "PR + review"
    merge fix/ten-ngan id: "PR + review "
```

| Nhánh | Mục đích |
| --- | --- |
| 🟢 `main` | Luôn ổn định, có thể build và chạy |
| ✨ `feature/<tên-ngắn>` | Phát triển tính năng mới |
| 🐞 `fix/<tên-ngắn>` | Sửa lỗi |
| 📝 `docs/<tên-ngắn>` | Cập nhật tài liệu |

### 📝 Commit

Theo [Conventional Commits](https://www.conventionalcommits.org/):

```text
feat(auth): thêm kiểm tra mật khẩu xác nhận
fix(home): sửa lọc theo môn khi không dấu
docs(readme): cập nhật hướng dẫn cấu hình VNPay
```

### 🔀 Pull Request

1. Tạo nhánh từ `main`, giữ thay đổi nhỏ và tập trung một mục tiêu.
2. Chạy `:app:testDebugUnitTest` và `:app:lintDebug` trước khi mở PR.
3. Mô tả rõ thay đổi, lý do, cách kiểm tra; đính kèm ảnh chụp màn hình nếu đổi giao diện.
4. Cần ít nhất một thành viên review và chấp thuận trước khi merge.
5. Thay đổi `firestore.rules` phải kèm kiểm thử trong `scripts/test-firebase.mjs`.

### 📐 Quy ước mã nguồn

- Mỗi màn hình một Fragment và một file XML riêng.
- Kiểm tra và chuẩn hóa đầu vào đặt trong `AccountValidation`, không rải trong Fragment.
- Truy cập dữ liệu và trạng thái tập trung ở lớp `*State`; Fragment chỉ xử lý hiển thị.
- Không thêm dữ liệu giả; trạng thái rỗng phải hiển thị đúng là rỗng.

## 🧪 Kiểm thử

| | Loại | Lệnh |
| :-: | --- | --- |
| ✅ | Unit test | `.\gradlew.bat :app:testDebugUnitTest` |
| 🔍 | Lint | `.\gradlew.bat :app:lintDebug` |
| 🛡️ | Auth và Rules | Firebase Emulator + `scripts/test-firebase.mjs` |

> [!TIP]
> Script kiểm thử chỉ dùng localhost và **không** ghi dữ liệu lên Firebase thật. OTP/SMS thật và email thật cần cấu hình Console và xác nhận riêng. Lệnh chạy Emulator: [FIREBASE_SETUP.md](FIREBASE_SETUP.md).

## 🔐 Bảo mật

- 🔒 Không commit `vnpay.properties`, khóa ký ứng dụng hoặc file chứa secret — các file này đã nằm trong `.gitignore`.
- 📤 Firestore Rules trong repository chỉ có hiệu lực sau khi được Publish trên Console.
- 🛡️ Rules bảo vệ hồ sơ theo UID, chặn nâng vai trò và chặn client sửa danh mục gia sư.
- 📮 Nếu phát hiện lỗ hổng, vui lòng báo riêng cho chủ repository thay vì mở issue công khai.

<img src="https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/rainbow.png" width="100%" alt="divider"/>

## 🩺 Xử lý sự cố

<details>
<summary><b>🔑 Không đăng nhập/đăng ký được</b></summary>

<br/>

**Nguyên nhân:** chưa bật Email/Password trên Console.
**Cách xử lý:** Firebase Console → Authentication → bật Email/Password.

</details>

<details>
<summary><b>🛡️ Báo thiếu quyền khi đọc/ghi dữ liệu</b></summary>

<br/>

**Nguyên nhân:** Rules chưa được Publish.
**Cách xử lý:** Publish `firestore.rules` lên Console.

</details>

<details>
<summary><b>🏠 Trang chủ trống</b></summary>

<br/>

**Nguyên nhân:** database chưa có gia sư/môn dạy (ứng dụng không hiển thị dữ liệu giả).
**Cách xử lý:** nhập dữ liệu theo [schema](FIRESTORE_SCHEMA.md).

</details>

<details>
<summary><b>⚙️ Build báo thiếu cấu hình VNPay</b></summary>

<br/>

**Nguyên nhân:** chưa có `vnpay.properties`.
**Cách xử lý:** sao chép từ `vnpay.properties.example` và điền thông tin TEST.

</details>

<details>
<summary><b>💳 VNPay trả mã 71 — terminal not approved</b></summary>

<br/>

**Nguyên nhân:** merchant TEST chưa được VNPay duyệt.
**Cách xử lý:** liên hệ VNPay kích hoạt. Không tự đánh dấu đơn thành công.

</details>

<details>
<summary><b>🔄 Đổi cấu hình VNPay nhưng không có hiệu lực</b></summary>

<br/>

**Nguyên nhân:** ứng dụng chưa được build lại.
**Cách xử lý:** build và cài lại ứng dụng.

</details>

## 🗺️ Lộ trình

```mermaid
timeline
    title Tiến độ phát triển TMDT
    section Đã hoàn tất
        Nền tảng : Trang chủ : Đăng nhập / đăng ký : Hồ sơ cá nhân
        Nghiệp vụ gia sư : Đăng ký làm gia sư : Chuyển đổi vai trò
        Thanh toán : VNPay TEST
    section Tiếp theo
        Mở rộng : Lịch học : Tin nhắn
```

- [x] 🏠 Trang chủ, đăng nhập, đăng ký, hồ sơ cá nhân
- [x] 🎓 Đăng ký làm gia sư và chuyển đổi vai trò
- [x] 💳 Thanh toán VNPay TEST
- [ ] 📅 Nghiệp vụ **Lịch học**
- [ ] 💬 Nghiệp vụ **Tin nhắn**

## 📖 Tài liệu

| | Tài liệu | Nội dung |
| :-: | --- | --- |
| 🔥 | [FIREBASE_SETUP.md](FIREBASE_SETUP.md) | Cấu hình Firebase và Emulator |
| 🗃️ | [FIRESTORE_SCHEMA.md](FIRESTORE_SCHEMA.md) | Schema Firestore rút gọn |
| 🎓 | [TUTOR_REGISTRATION.md](TUTOR_REGISTRATION.md) | Luồng đăng ký làm gia sư |
| 🎨 | [FIGMA_MENU.md](FIGMA_MENU.md) | Menu dưới theo Figma |
| 🧩 | [SCREEN_STRUCTURE.md](SCREEN_STRUCTURE.md) | Cấu trúc màn hình và Fragment |
| 💳 | [PAYMENT_SETUP.md](PAYMENT_SETUP.md) | Cấu hình thanh toán VNPay TEST |

## 💬 Đóng góp và liên hệ

Mọi đóng góp đều được hoan nghênh! Hãy đọc [Quy trình phát triển](#-quy-trình-phát-triển) trước khi mở Pull Request, và dùng [Issues](https://github.com/pvhNam/TMDT_CK/issues) để báo lỗi hoặc đề xuất tính năng.

<div align="center">

<a href="https://github.com/pvhNam/TMDT_CK">
  <img src="https://github-readme-stats.vercel.app/api/pin/?username=pvhNam&repo=TMDT_CK&theme=tokyonight&hide_border=true" alt="Repo card" />
</a>

<br/><br/>

[![Owner](https://img.shields.io/badge/Owner-%40pvhNam-181717?style=for-the-badge&logo=github)](https://github.com/pvhNam)
[![Contributors](https://img.shields.io/badge/Contributors-View%20all-2E86DE?style=for-the-badge)](https://github.com/pvhNam/TMDT_CK/graphs/contributors)
[![Issues](https://img.shields.io/badge/Issues-Report%20a%20bug-E74C3C?style=for-the-badge)](https://github.com/pvhNam/TMDT_CK/issues)

<br/>

⭐ **Nếu thấy dự án hữu ích, hãy tặng một star nhé!** ⭐

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0F2027,50:2E86DE,100:8E44AD&height=120&section=footer" alt="footer" width="100%"/>

</div>
