# Sun Assistant
Ứng dụng trợ lý ảo trên hệ điều hành Android
## Công nghệ sử dụng
-Ngôn ngữ: Kotlin </br>
-IDE: Android Studio</br>
-Speech Recognition, Text to Speech, Firebase, Room, OpenWeatherMap, Jsoup...
## Biểu đồ phân rã chức năng
<img src="https://user-images.githubusercontent.com/94952035/180855617-9d5667e4-a0ee-4cb3-af7e-12e417d7b7a7.png" width="900" /> </br>
## Giao diện ứng dụng
### Đăng nhập
Người dùng đăng nhập bằng tài khoản Google, sử dụng Firebase Authentication để xác thực </br>
<img src="https://user-images.githubusercontent.com/94952035/180857303-8276962a-d0a4-4950-818d-1fa0d6b3aba4.png" width="250" /> </br>
### Màn hình chính
Sau khi đăng nhập, người dùng được chuyển đến màn hình chính của ứng dụng gồm các thành phần sau :</br>
(1)	 TextView thể hiện lời chào của trợ lý ảo tới người dùng. Lời chào đã có thể tùy chỉnh theo tên người dùng và đưa ra các lời chào khác nhau vào các buổi trong ngày.</br>
(2)	 Nơi thể hiện Avatar của người dùng. Khi click vào avatar đã chuyển tới menu cài đặt như dự kiến ban đầu.</br>
(3)	 Thanh tiện ích giúp người dùng có thể truy cập nhanh vào một số chức năng của ứng dụng như ghi chú, gợi ý…</br>
(4)	 Nơi thể hiện lịch sử tương tác giữa người dùng và trợ lý, các văn bản được hiển thị dễ nhìn và chính xác nội dung. Ở đây các chức năng đề ra của ứng dụng đã hoạt động ổn định, các chức năng sau khi thực thi đã hiện thị yêu cầu của người dùng và câu trả lời của trợ lý ảo.</br>
(5)	 Button Micro để người dùng có thể bắt đầu trò chuyện với trợ lý. Hoạt động ổn định và có hiệu ứng khi được chạm vào, giúp người dùng có thể nhận diện được lúc chức năng hoạt động.</br>

<img src="https://user-images.githubusercontent.com/94952035/180858052-0ce39786-4007-4809-b4bd-10562b44ed8d.png" width="250" /> </br>
### Màn hình cài đặt
Người dùng có thể tùy chỉnh ứng dụng vs các chức năng như ngôn ngữ trợ lý, widget... </br>
<img src="https://user-images.githubusercontent.com/94952035/180858665-de5f04cc-2f31-4927-8f01-5dba462b2fc0.png" width="250" /> </br>
### Quản lý ghi chú
Các ghi chú sau khi được tạo sẽ được mã hóa AES và lưu ở Realtime Database </br>
<img src="https://user-images.githubusercontent.com/94952035/180859557-cc6724f0-ea18-43d4-bb42-a66d04912a58.png" width="500" /> </br>
Các ghi chú được giải mã và hiện thị dưới dạng recyclerview </br>
<img src="https://user-images.githubusercontent.com/94952035/180859351-f36af3dd-73c4-4689-9f6f-9c47f8f65ac6.png" width="250" /> </br>
Người dùng có thể chỉnh sửa, xóa ghi chú </br>
<img src="https://user-images.githubusercontent.com/94952035/180860004-8ebf75b0-1c84-4772-9c6c-d1e27dcd068f.png" width="250" /> </br>
### Tạo nhắc nhở
Để có thể sử dụng chức năng tạo nhắc nhở, người dùng ấn vào button microphone nói câu lệnh tạo nhắc nhở hoặc ấn vào biểu tượng nhắc nhắc trên thanh công cụ để truy cập nhanh.</br>
<img src="https://user-images.githubusercontent.com/94952035/180860529-4cc011ee-91ca-4864-bde6-2accebed563b.png" width="500" /> </br>
Chức năng sử dụng AlarmManager để lên lên lịch hẹn gửi thông báo nhắc nhở cho người dùng</br>
Đảm bảo khi không có kết nối Internet thì người dùng vẫn nhận được và không bị bỏ lỡ thông báo. </br>
<img src="https://user-images.githubusercontent.com/94952035/180860754-e0a94cbb-e844-4c2c-816a-593ea7964215.png" width="500" /> </br>
### Widget Floatting
Chạy dưới dạng service overlay, giúp người dùng có thể truy cập nhanh vào ứng dụng</br>
<img src="https://user-images.githubusercontent.com/94952035/180861499-49500f91-0307-4b7f-a5c6-19ce1eddae5a.png" width="500" /> </br>
### Màn hình gợi ý
Gợi ý các chức năng và câu lệnh mà người dùng có thể yêu cầu trợ lý </br>
<img src="https://user-images.githubusercontent.com/94952035/180861777-7e515b4e-7d89-483f-b993-45071e3681c0.png" width="250" /> </br>

# Thanks for reading
## Thái Bá Hùng KMA





