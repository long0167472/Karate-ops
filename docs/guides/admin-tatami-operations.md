# Báo cáo nghiệp vụ màn Tatami

## Mục lục

1. [Phạm vi tài liệu](#1-phạm-vi-tài-liệu)
2. [Tổng quan 2 màn chính](#2-tổng-quan-2-màn-chính)
3. [Màn điều khiển Tatami Control](#3-màn-điều-khiển-tatami-control)
4. [Màn hiển thị chính Display](#4-màn-hiển-thị-chính-display)
5. [Luồng thao tác nghiệp vụ điển hình](#5-luồng-thao-tác-nghiệp-vụ-điển-hình)
6. [Các điểm cần lưu ý khi vận hành](#6-các-điểm-cần-lưu-ý-khi-vận-hành)

## 1. Phạm vi tài liệu

Tài liệu này chỉ tập trung vào góc nhìn nghiệp vụ của:

- màn điều khiển `Tatami Control`
- màn hiển thị chính `Display`

Tài liệu tạm thời bỏ qua:

- chi tiết backend, API, realtime, phân quyền kỹ thuật
- màn OBS/live overlay
- luồng tích hợp kỹ thuật với các màn khác

## 2. Tổng quan 2 màn chính

### 2.1 Màn điều khiển

Đây là màn thư ký hoặc người vận hành tatami dùng để:

- theo dõi trận đang diễn ra
- điều khiển đồng hồ
- ghi điểm cho AKA và AO
- ghi nhận cảnh cáo, phạt
- xác nhận kết quả thắng thua
- quan sát nhanh bản xem trước của màn hiển thị chính

### 2.2 Màn hiển thị chính

Đây là màn dành cho:

- khán giả
- trọng tài bàn cần nhìn nhanh tình trạng trận
- màn hình lớn hoặc máy chiếu tại khu vực thi đấu

Mục tiêu của màn này là:

- hiển thị rõ ràng tỷ số
- hiển thị thời gian và trạng thái trận
- hiển thị bên nào đang có lợi thế hoặc đã thắng
- giúp người xem nắm tình hình trận đấu ngay lập tức

## 3. Màn điều khiển Tatami Control

## 3.1 Mục đích nghiệp vụ

Màn Tatami Control là trung tâm thao tác của thư ký. Đây là nơi toàn bộ quyết định ghi nhận trong trận được nhập vào hệ thống.

Màn này hỗ trợ hai kiểu điều hành chính:

- điều hành trận kumite
- điều hành trận kata

## 3.2 Bố cục chính

Màn điều khiển được chia thành 3 vùng:

- cột trái: điều khiển cho bên AKA
- cột giữa: đồng hồ, thông tin trận, luồng điều hành
- cột phải: điều khiển cho bên AO

Phía trên cùng là thanh tiêu đề, nơi thư ký nhìn nhanh được:

- tên màn hình điều khiển
- tatami hiện tại
- nội dung trận đang diễn ra
- chế độ kumite hoặc kata
- nút bật/tắt bản xem trước của màn hiển thị chính

## 3.3 Các chức năng chính cho kumite

### 3.3.1 Quản lý thông tin hai bên

Mỗi bên AKA và AO đều có khu vực riêng để hiển thị:

- tên vận động viên
- câu lạc bộ
- bib
- điểm hiện tại

Về nghiệp vụ, đây là vùng để thư ký kiểm tra đúng người, đúng bên, đúng trận trước khi thao tác điểm hoặc phạt.

### 3.3.2 Ghi điểm

Mỗi bên có các nút chấm điểm:

- `+1`
- `+2`
- `+3`

Ý nghĩa vận hành:

- thư ký bấm ngay khi trọng tài công nhận điểm
- hệ thống cộng thẳng vào tổng điểm của bên tương ứng

Ngoài ra còn có các nút:

- `-1`
- `-2`
- `-3`

Các nút này phục vụ tình huống:

- thư ký nhập nhầm điểm
- cần hoàn tác thủ công một điểm vừa nhập

### 3.3.3 Ghi Senshu

Mỗi bên có nút `Senshu`.

Về nghiệp vụ:

- dùng để đánh dấu bên đang có lợi thế Senshu
- màn điều khiển và màn hiển thị chính đều cho thấy rõ Senshu đang thuộc về AKA hay AO

### 3.3.4 Ghi cảnh cáo và phạt

Màn điều khiển hỗ trợ các mức xử lý chính:

- Chui
- Hansoku Chui
- Hansoku
- Shikkaku
- Kiken

Trong đó:

- `Chui` có thể chỉnh theo mức `0, 1, 2, 3`
- các mức còn lại là dạng bật/tắt theo từng bên

Về nghiệp vụ, đây là phần giúp thư ký ghi nhận toàn bộ tiến trình xử phạt trong trận, để màn hiển thị chính phản ánh đúng trạng thái tranh tài.

### 3.3.5 Điều khiển đồng hồ

Khu vực Timer cho phép thư ký:

- bắt đầu đồng hồ
- dừng đồng hồ
- reset đồng hồ
- cộng/trừ nhanh thời gian
- chọn nhanh mốc thời lượng có sẵn

Mục đích nghiệp vụ:

- bám sát diễn biến trận
- hỗ trợ tình huống cần điều chỉnh lại thời gian
- giúp thư ký phản ứng nhanh khi có quyết định của trọng tài chính

### 3.3.6 Điều hành kết thúc trận

Trong kumite, màn điều khiển có các chức năng:

- bật `Hantei`
- xác nhận bên thắng

Ý nghĩa nghiệp vụ:

- `Hantei` dùng khi trận đi vào tình huống cần phân định bằng quyết định trọng tài
- `Confirm winner` là thao tác chốt kết quả cuối cùng của thư ký sau quyết định chuyên môn

## 3.4 Các chức năng chính cho kata

Với kata, màn điều khiển chuyển trọng tâm từ điểm số sang phiếu.

Các phần nghiệp vụ chính gồm:

- theo dõi tổng số phiếu AKA và AO
- nhìn được số lượng judge đang áp dụng
- nhập nhanh phiếu theo từng judge
- xác nhận người thắng khi đã đủ cơ sở kết luận

Mục tiêu của khu vực kata control là:

- giúp thư ký tổng hợp quyết định của trọng tài
- nhìn nhanh bên nào đang chiếm đa số
- chốt kết quả cuối cùng một cách rõ ràng

## 3.5 Khu vực lịch sử thao tác

Màn điều khiển có khu vực hiển thị các sự kiện gần nhất, ví dụ:

- cộng điểm
- trừ điểm
- phạt
- vote kata
- xác nhận thắng

Về nghiệp vụ, vùng này giúp thư ký:

- tự kiểm tra lại thao tác vừa làm
- rà soát khi có tranh luận tại bàn thư ký
- nhìn lại chuỗi diễn biến gần nhất của trận

## 3.6 Bản xem trước màn hiển thị chính

Màn điều khiển cho phép bật một cửa sổ preview của màn Display.

Giá trị nghiệp vụ của preview:

- thư ký không cần quay sang màn hình lớn vẫn biết khán giả đang nhìn thấy gì
- có thể kiểm tra nhanh xem điểm, phạt, thời gian, người thắng đã hiển thị đúng chưa
- thuận tiện khi vận hành một mình hoặc trong khu vực chật

## 4. Màn hiển thị chính Display

## 4.1 Mục đích nghiệp vụ

Màn Display là màn công khai, thiên về quan sát hơn thao tác. Tất cả thông tin ở đây phải:

- dễ đọc từ xa
- ưu tiên điểm, thời gian, trạng thái
- nhấn mạnh bên thắng hoặc lợi thế quan trọng

## 4.2 Nội dung hiển thị chung

Ở cả kumite và kata, màn hình chính đều thể hiện:

- tên hạng mục
- tên tatami
- round
- mã hoặc số trận

Nhóm thông tin này giúp người xem biết ngay:

- đây là trận nào
- đang thi đấu ở đâu
- thuộc vòng nào

## 4.3 Hiển thị cho kumite

Ở chế độ kumite, màn hiển thị chính có 3 cụm rõ ràng:

- cụm trái AKA
- cụm giữa
- cụm phải AO

### 4.3.1 Cột AKA và AO

Mỗi bên hiển thị:

- tên vận động viên
- câu lạc bộ
- bib
- điểm số lớn, dễ nhìn
- trạng thái Senshu nếu có
- thang cảnh cáo/phạt

Ý nghĩa nghiệp vụ:

- người xem thấy ngay ai đang dẫn điểm
- thư ký và bàn trọng tài có thể đối chiếu nhanh với quyết định trên sàn
- trạng thái phạt được nhìn thấy ngay, tránh bỏ sót

### 4.3.2 Cột giữa

Phần giữa là vùng nghiệp vụ quan trọng nhất trên màn hiển thị:

- trạng thái trận
- đồng hồ đếm ngược cỡ lớn
- cảnh báo khi sắp hết giờ
- tín hiệu Hantei nếu trận vào giai đoạn phân định
- tín hiệu gợi nhớ Senshu hoặc lợi thế quan trọng

Điểm mạnh của vùng này là giúp toàn bộ khu vực thi đấu nhìn được "nhịp trận" chỉ bằng một cái nhìn.

### 4.3.3 Hiệu ứng điểm vừa ghi

Khi một bên vừa được cộng điểm, màn hiển thị có hiệu ứng nổi bật ngay trên bên đó.

Ý nghĩa nghiệp vụ:

- xác nhận trực quan rằng điểm vừa được ghi thành công
- giúp khán giả và thư ký cùng nhận ra thay đổi tức thì

### 4.3.4 Vùng người thắng

Khi đã chốt kết quả, màn hiển thị sẽ nổi bật bên thắng:

- hiện chữ Winner
- hiện rõ bên thắng
- hiện tên vận động viên thắng

Đây là phần kết thúc trận trên phương diện trình bày công khai.

## 4.4 Hiển thị cho kata

Với kata, màn hình chính tập trung vào:

- hai vận động viên hoặc hai bên trình diễn
- tên bài kata
- phiếu của judge
- tổng phiếu hai bên
- bên chiếm đa số

### 4.4.1 Hai bên thi đấu

Mỗi bên hiển thị:

- tên vận động viên
- câu lạc bộ
- bib
- tên bài kata

Nếu đã đến giai đoạn công bố, màn còn thể hiện thêm số phiếu của từng bên.

### 4.4.2 Khu vực trung tâm

Phần giữa của màn kata thể hiện:

- giai đoạn hiện tại
- đồng hồ countdown
- các ô judge
- tổng phiếu AKA và AO
- số phiếu cần thiết để thắng

Ý nghĩa nghiệp vụ:

- khán giả thấy rõ tiến trình từ chờ phiếu đến công bố kết quả
- thư ký nhìn nhanh được đã đủ đa số hay chưa
- người xem dễ hiểu cách bên thắng được xác định

### 4.4.3 Công bố đa số và người thắng

Khi đủ điều kiện kết luận, màn hình sẽ nhấn mạnh:

- bên đang chiếm đa số
- người thắng cuối cùng sau khi chốt kết quả

## 5. Luồng thao tác nghiệp vụ điển hình

## 5.1 Luồng kumite

1. Thư ký kiểm tra đúng trận, đúng AKA, đúng AO
2. Bắt đầu đồng hồ khi trận khởi động
3. Ghi điểm cho từng bên theo quyết định của trọng tài
4. Ghi nhận Senshu nếu có
5. Ghi cảnh cáo hoặc phạt nếu có
6. Theo dõi thời gian và trạng thái trận
7. Nếu cần, chuyển sang Hantei
8. Xác nhận bên thắng
9. Màn hiển thị chính chuyển sang trạng thái công bố kết quả

## 5.2 Luồng kata

1. Thư ký theo dõi thông tin hai bên và nội dung biểu diễn
2. Ghi hoặc theo dõi phiếu theo từng judge
3. Kiểm tra tổng phiếu và bên đang chiếm đa số
4. Xác nhận người thắng
5. Màn hiển thị chính chuyển sang trạng thái công bố kết quả

## 6. Các điểm cần lưu ý khi vận hành

- Màn điều khiển là nơi nhập liệu chính, nên thư ký cần kiểm tra đúng bên trước khi bấm điểm hoặc phạt
- Màn hiển thị chính nên được xem như "mặt tiền" của trận đấu, vì toàn bộ khán giả sẽ nhìn vào đây
- Preview trên màn điều khiển rất hữu ích để thư ký tự kiểm tra đầu ra hiển thị
- Với kumite, 3 nhóm thông tin quan trọng nhất là điểm, thời gian, phạt
- Với kata, 3 nhóm thông tin quan trọng nhất là phiếu judge, tổng phiếu, bên chiếm đa số
- Khi chốt người thắng, thao tác xác nhận cần được thực hiện cẩn thận vì đây là kết luận cuối cùng của trận trên hệ thống
