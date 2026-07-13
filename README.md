# RemoteHelper

## Trạng thái MVP End-to-End
Dự án đã được tái cấu trúc thành công thành một MVP WebRTC Remote Control theo đúng hướng dẫn audit.

### Các phase đã hoàn thành:
1. **Chuẩn hóa Project & Dependency**: Đổi namespace thành `com.hiroshimeow.remotehelper`, xóa bỏ các dependency thừa (Firebase, Room, Retrofit, v.v.), chỉ giữ lại Ktor, WebRTC và Jetpack Compose.
2. **Protocol & Authentication Gate**: Chuẩn hóa envelope giao tiếp theo bản v1 (`ProtocolEnvelope`, `ProtocolTypes`), yêu cầu xác thực bằng PIN trước khi cho phép WebRTC SDP/ICE negotiation hoặc điều khiển gesture.
3. **Foreground Service & Session Ownership**: `MainActivity` không còn giữ trạng thái server; `RemoteSessionService` (Foreground Service) là chủ sở hữu của MediaProjection, Ktor server và WebRTC PeerConnection, với thông báo liên tục khi màn hình đang được chia sẻ.
4. **Android WebRTC Publisher**: Triển khai `WebRtcStreamEngine` tích hợp `ScreenCapturerAndroid` và tạo `Offer` chuẩn để đẩy qua WebSocket.
5. **Browser WebRTC Client**: Cập nhật file `index.html` tích hợp màn hình nhập PIN xác thực, thiết lập kết nối `RTCPeerConnection` sau khi xác thực thành công, trả về `Answer` và xử lý letterboxing (scale/crop tọa độ pointer cho chính xác).
6. **Input Pipeline & Gesture Logic**: Cải tiến `BasicCoordinateMapper` sử dụng `WindowMetrics`, và thêm `GestureResultCallback` kết hợp cơ chế `isDispatching` để đảm bảo hệ thống không nhận lệnh chồng chéo, tránh crash `AccessibilityService`.
7. **Bảo mật**: Vô hiệu hóa `allowBackup`, chặn kết nối nếu chưa đăng nhập, giới hạn 1 controller đồng thời, và vô hiệu hóa controller cũ khi session kết thúc.

## Hướng dẫn cài đặt
1. Build file APK thông qua Android Studio hoặc CLI `gradle :app:assembleDebug`.
2. Cài đặt APK trên thiết bị Android, cấp quyền Accessibility cho Remote Helper trong Settings.
3. Mở ứng dụng, nhấn **START SESSION** và cấp quyền MediaProjection (quay màn hình).
4. Truy cập URL hiển thị trên màn hình ứng dụng từ trình duyệt PC/Mobile (cùng mạng LAN hoặc Tailscale).
5. Nhập mã PIN bảo mật 6 chữ số và bắt đầu điều khiển từ xa.
