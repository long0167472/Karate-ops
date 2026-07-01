# Phân tích gap Kumite so với luật quốc tế WKF

## 1. Phạm vi và nguồn đối chiếu

### 1.1 Phạm vi

Tài liệu này đối chiếu phần **kumite** của KarateOps với bộ luật thi đấu quốc tế của **WKF** ở các khía cạnh:

- scoring và win condition
- Senshu
- warning / penalty
- Hantei / Hikiwake
- Video Review
- 10-second rule
- thời lượng trận
- khác biệt theo format thi đấu như elimination / round-robin / team kumite

### 1.2 Nguồn luật dùng để đối chiếu

Nguồn mình xác minh được trực tiếp trong môi trường hiện tại:

- WKF Kumite Competition Rules 2024
  - mirror: https://karateontario.wordpress.com/wp-content/uploads/2025/03/f57d8-wkf_kumite_competition_rules_2024.pdf
- WKF Kumite Examination Questions 2024
  - mirror: https://karateontario.wordpress.com/wp-content/uploads/2025/03/8e1a1-allquestionskumite_eng-2024.pdf
- Trang tổng hợp quy tắc WKF của Karate Ontario
  - https://karateontario.wordpress.com/wkf-rules-2/

### 1.3 Lưu ý quan trọng

- Repo hiện gắn nhãn `WKF_2026`, nhưng trong môi trường hiện tại mình **không xác minh được một bản full rulebook WKF 2026 chính thức có thể truy cập trực tiếp** từ `wkf.net`.
- Vì vậy, phần gap analysis này dùng **WKF 2024 đã xác minh được** làm chuẩn quốc tế gần nhất có thể kiểm chứng trực tiếp.
- Ngoài ra, mình cũng kiểm tra luôn việc code hiện tại có thực sự dùng `RulesetVersion.WKF_2026` để thay đổi hành vi hay không.

## 2. Kết luận nhanh

Nếu đánh giá theo mức “có thể vận hành trận kumite cơ bản”, hệ thống **đã đáp ứng được phần lõi tối thiểu**:

- ghi điểm 1 / 2 / 3
- ghi Chui, Hansoku Chui, Hansoku, Shikkaku, Kiken
- chạy/dừng/chỉnh đồng hồ
- chốt winner thủ công
- có rule engine mức cơ bản cho chênh 8 điểm, hết giờ, Senshu, Hantei

Nhưng nếu đánh giá theo mức **tuân thủ WKF quốc tế đủ để dùng cho giải chuẩn quốc tế**, hệ thống hiện tại **chưa đủ**.

Các gap lớn nhất là:

1. `RulesetVersion` chỉ là nhãn dữ liệu, chưa điều khiển logic thật.
2. Thiếu tie-break theo số `IPPON` và `WAZA-ARI` khi hòa điểm.
3. Thiếu logic `SENSHU` chuẩn WKF, đặc biệt phần **annulment / TORIMASEN** trong 15 giây cuối.
4. Thiếu workflow `Video Review`.
5. Thiếu workflow `10-second rule` và y tế.
6. Thiếu mô hình hóa `HIKIWAKE` và các case team / round-robin.
7. `KIKEN / HANSOKU / SHIKKAKU` đang bị gộp quá thô trong rule engine và luồng confirm kết quả.
8. `matchDurationSeconds` của category chưa được đẩy xuống `KumiteMatchState.durationMs` khi draw trận.

## 3. Đối chiếu theo từng nhóm luật

## 3.1 Scoring cơ bản

### Luật WKF

WKF 2024 quy định:

- `YUKO = 1`
- `WAZA-ARI = 2`
- `IPPON = 3`
- chênh **8 điểm** là thắng trận

### Hệ thống hiện tại

Đã có:

- `SCORE_DELTA` với các giá trị `-3, -2, -1, 1, 2, 3`
- FE hiển thị `+1 = Yuko`, `+2 = Waza-ari`, `+3 = Ippon`
- rule engine xử lý thắng khi chênh `>= 8`

### Đánh giá

- **Đáp ứng phần lõi**

### Gap còn lại

- hệ thống chỉ lưu tổng điểm, **không lưu cấu trúc thống kê riêng theo số lần IPPON / WAZA-ARI / YUKO**
- điều này làm hỏng các luật tie-break chuẩn WKF ở cuối trận

### Mức ưu tiên

- **P1**

## 3.2 Tiêu chí thắng khi hết giờ

### Luật WKF

Theo Điều 12.2:

- thắng khi chênh 8 điểm
- hoặc hết giờ có điểm cao hơn
- hoặc hòa điểm nhưng có `SENSHU`
- nếu vẫn chưa phân định thì:
  - ưu tiên số `IPPON`
  - sau đó ưu tiên số `WAZA-ARI`
  - sau đó mới tới `HANTEI`
- với round-robin / team có thể ra `HIKIWAKE` thay vì Hantei trong một số trường hợp

### Hệ thống hiện tại

`KumiteRuleEngine` hiện chỉ làm:

- chênh 8 điểm -> thắng
- hết giờ hơn điểm -> thắng
- hòa điểm mà có Senshu -> thắng
- nếu không thì -> `HANTEI`

### Đánh giá

- **Đáp ứng một phần**

### Gap

Thiếu hoàn toàn:

- tie-break theo số `IPPON`
- tie-break theo số `WAZA-ARI`
- `HIKIWAKE` cho những format cần draw
- phân nhánh khác nhau giữa elimination, round-robin, team extra bout

### Mức ưu tiên

- **P0**

### Cần sửa gì

1. Bổ sung dữ liệu để tính được số `IPPON / WAZA-ARI / YUKO` hợp lệ sau các lần cộng/trừ điểm.
2. Viết lại `KumiteRuleEngine` theo đúng thứ tự Điều 12.
3. Bổ sung khái niệm kết quả `HIKIWAKE`.
4. Phân nhánh logic theo `competition format`.

## 3.3 Senshu

### Luật WKF

WKF định nghĩa `SENSHU` là:

- **first unopposed point advantage**
- nếu hai bên cùng có điểm trước tín hiệu thì **không cấp Senshu**
- nếu Senshu bị hủy trong 15 giây cuối do avoiding combat/Jogai/running away/... thì:
  - phải `TORIMASEN`
  - không được cấp lại Senshu cho bất kỳ bên nào trong phần thời gian còn lại
- Video Review có thể dẫn tới hủy Senshu nếu hóa ra điểm không còn là unopposed

### Hệ thống hiện tại

Hiện tại:

- FE có nút bấm `Senshu` thủ công
- backend có event `SENSHU` để set bên được Senshu
- rule engine chỉ đọc boolean `akaSenshu / aoSenshu`
- **không có logic tự xác định “first unopposed point”**
- **không có logic hủy Senshu**
- **không có khái niệm khóa không cho cấp lại Senshu**

### Đánh giá

- **Thiếu nghiêm trọng**

### Gap

Đây là một trong những sai lệch lớn nhất so với luật WKF:

- Senshu đang là cờ bật/tắt bằng tay, không phải kết quả của luật
- không có `SENSHU TORIMASEN`
- không có “no further Senshu after withdrawal in last 15 seconds”

### Mức ưu tiên

- **P0**

### Cần sửa gì

1. Không để Senshu là một nút toggle đơn thuần nữa.
2. Tính Senshu từ chuỗi scoring event theo định nghĩa “first unopposed point”.
3. Thêm event/logic `SENSHU_CANCELLED`.
4. Lưu cờ `senshuLockedAfterAnnulment` hoặc tương đương cho phần cuối trận.
5. Hiển thị rõ trạng thái `SENSHU TORIMASEN` trên FE và log.

## 3.4 Warning / penalty và loại lỗi

### Luật WKF

WKF phân rất rõ:

- `CHUI`
- `HANSOKU CHUI`
- `HANSOKU`
- `SHIKKAKU`

Đồng thời còn phân rõ **loại hành vi vi phạm**, ví dụ:

- excessive contact
- Jogai
- passivity
- avoiding combat
- Mubobi
- clinching / grabbing / pushing
- không tuân lệnh trọng tài

Một số tình huống có hậu quả đặc biệt:

- passivity không được cho trong 15 giây đầu và 15 giây cuối
- avoiding combat trong 15 giây cuối tối thiểu là `HANSOKU CHUI` và mất Senshu
- Jogai cuối trận khi bên thua bỏ chạy có thể bị escalated mạnh hơn

### Hệ thống hiện tại

Hiện tại hệ thống chỉ lưu:

- `CHUI` dưới dạng số đếm
- `HANSOKU_CHUI`, `HANSOKU`, `SHIKKAKU`, `KIKEN` dưới dạng cờ boolean
- `penaltyCode` là string chung

Nhưng không mô hình hóa đầy đủ:

- loại foul là gì
- nó xảy ra ở giai đoạn nào của trận
- có thuộc nhóm “last 15 seconds” hay không
- có kéo theo mất Senshu hay không

### Đánh giá

- **Đáp ứng ghi nhận cơ bản, nhưng không đủ cho rule enforcement chuẩn WKF**

### Gap

- thiếu taxonomy foul chuẩn luật
- thiếu rule riêng cho `passivity`, `avoiding combat`, `jogai`, `mubobi`
- thiếu quan hệ giữa warning và trạng thái `SENSHU`

### Mức ưu tiên

- **P0**

### Cần sửa gì

1. Đổi `penaltyCode: String` sang enum hoặc cấu trúc chuẩn hơn.
2. Tách rõ:
   - `foulType`
   - `penaltyLevel`
   - `occurredInAtoShibaraku`
3. Thêm event riêng cho:
   - `JOGAI`
   - `PASSIVITY`
   - `AVOIDING_COMBAT`
   - `MUBOBI`
   - `EXCESSIVE_CONTACT`
4. Viết escalation rule theo WKF, đặc biệt đoạn cuối trận.

## 3.5 KIKEN, HANSOKU, SHIKKAKU

### Luật WKF

WKF phân biệt rõ:

- `KIKEN`: bỏ cuộc / không xuất hiện / không thể tiếp tục
- `HANSOKU`: disqualification khỏi bout
- `SHIKKAKU`: disqualification khỏi tournament

Đây không phải cùng một loại thắng.

### Hệ thống hiện tại

Trong `KumiteRuleEngine`, cả:

- `akaHansoku`
- `akaShikkaku`
- `akaKiken`

đều đang đi chung vào một nhánh và trả về:

- `WinType.DISQUALIFICATION`

Ngoài ra FE hiện không map thuận tiện để confirm ra đúng `KIKEN`, `HANSOKU`, `SHIKKAKU`.

### Đánh giá

- **Sai luật ở mức semantic**

### Gap

- conflation giữa nhiều loại kết thúc trận khác nhau
- log, thống kê và báo cáo sau giải sẽ sai nghĩa

### Mức ưu tiên

- **P0**

### Cần sửa gì

1. Rule engine phải trả về:
   - `WinType.KIKEN`
   - `WinType.HANSOKU`
   - `WinType.SHIKKAKU`
   hoặc mô hình tương đương
2. FE phải cho thư ký xác nhận đúng loại thắng, không rơi về `MANUAL`.
3. Audit trail phải lưu được lý do chuẩn.

## 3.6 Hantei và Hikiwake

### Luật WKF

WKF 2024:

- `HANTEI` chỉ dùng trong các tình huống nhất định
- `HIKIWAKE` tồn tại ở một số format như round-robin / team resolution

### Hệ thống hiện tại

Hiện có:

- trạng thái `HANTEI`
- FE có nút bật `Hantei`

Nhưng thiếu:

- khái niệm `HIKIWAKE`
- confirm flow cho draw
- phân biệt khi nào được Hantei, khi nào phải Hikiwake

### Đánh giá

- **Đáp ứng một phần**

### Mức ưu tiên

- **P1**

### Cần sửa gì

1. Bổ sung mô hình `draw / hikiwake`.
2. Tách rule theo:
   - individual elimination
   - round-robin
   - team kumite
   - extra bout

## 3.7 Video Review

### Luật WKF

WKF có hẳn Điều 14:

- coach có thẻ VR
- VR dùng để xin xem lại score
- VR supervisor có thể award score
- VR có thể dẫn tới hủy `SENSHU`
- không phải lỗi nào cũng VR được

### Hệ thống hiện tại

Dấu hiệu hiện tại:

- enum `ScoreEventType` có `VR`
- UI có nút `VR / Accept / Deny`

Nhưng thực tế:

- backend chưa xử lý flow VR
- UI đang disable
- không có state cho card, request, accept/deny, source clip, result

### Đánh giá

- **Thiếu hoàn toàn workflow**

### Mức ưu tiên

- **P0**

### Cần sửa gì

1. Thiết kế domain riêng cho Video Review:
   - ai yêu cầu
   - lúc nào yêu cầu
   - tình huống gì
   - kết quả gì
2. Thêm event/state:
   - `VR_REQUESTED`
   - `VR_ACCEPTED`
   - `VR_DENIED`
   - `VR_SCORE_AWARDED`
   - `VR_SENSHU_CANCELLED`
3. Gắn VR vào control flow và scoreboard.

## 3.8 10-second rule và y tế

### Luật WKF

WKF quy định:

- nếu VĐV ngã / bị knock down / bị ném mà không đứng dậy hoàn toàn trong 10 giây
- sẽ bị rút khỏi toàn bộ kumite của tournament
- referee phải gọi bác sĩ và đếm 10 giây

### Hệ thống hiện tại

Hiện không có mô hình nghiệp vụ cho:

- bắt đầu 10-second count
- kết luận fail 10-second rule
- ghi nhận quyết định y tế
- auto-withdraw khỏi các kumite event còn lại

### Đánh giá

- **Thiếu hoàn toàn**

### Mức ưu tiên

- **P0**

### Cần sửa gì

1. Thêm event:
   - `MEDICAL_STOP`
   - `TEN_SECOND_COUNT_START`
   - `TEN_SECOND_RULE_LOSS`
2. Gắn quyết định này vào trạng thái athlete trong tournament.
3. Bổ sung màn control cho medical outcome.

## 3.9 Thời lượng trận theo nhóm tuổi

### Luật WKF

WKF 2024:

- Senior và U21: `3 phút`
- Cadet và Junior: `2 phút`
- Under 14: `1.5 phút`

### Hệ thống hiện tại

Hiện trạng:

- category có `matchDurationSeconds`
- category có thể lưu giá trị `120`, `180`, ...
- nhưng khi draw trận, `KumiteMatchState.durationMs` vẫn khởi tạo mặc định `180000`
- tức là duration thực tế trong state trận **không tự động đi theo category**

### Đánh giá

- **Có dữ liệu cấu hình nhưng chưa áp dụng nhất quán**

### Mức ưu tiên

- **P0**

### Cần sửa gì

1. Khi tạo `KumiteMatchState`, set:
   - `durationMs = category.matchDurationSeconds * 1000`
   - `remainingMs = durationMs`
2. Bổ sung test cho:
   - Senior 180s
   - Cadet/Junior 120s
   - U14 90s

## 3.10 RulesetVersion

### Luật/Kỳ vọng

Nếu hệ thống đã khai báo `RulesetVersion.WKF_2026`, thì kỳ vọng hợp lý là:

- ruleset version phải ảnh hưởng đến rule behavior
- hoặc tối thiểu phải ảnh hưởng tới preset duration / tie-break / review / penalty semantics

### Hệ thống hiện tại

Hiện tại `RulesetVersion`:

- được lưu ở `Tournament` và `Category`
- được trả ra API
- nhưng **không được dùng để rẽ nhánh logic rule engine hoặc match flow**

### Đánh giá

- **Mới là metadata, chưa phải ruleset engine**

### Mức ưu tiên

- **P1**

### Cần sửa gì

1. Tạo lớp cấu hình luật theo version:
   - `KumiteRulesProfile`
2. Resolve profile từ:
   - tournament.rulesetVersion
   - category.rulesetVersion
3. Mọi logic rule phải đọc profile thay vì hard-code.

## 3.11 Round-robin và team kumite

### Luật WKF

WKF kumite hiện không chỉ có single elimination. Rulebook 2024 có:

- round-robin individual
- team kumite
- extra bout
- Hikiwake / Hantei theo ngữ cảnh

### Hệ thống hiện tại

Hiện trạng trong repo:

- enum có `ROUND_ROBIN`, `POOL`, `TEAM_KUMITE`
- nhưng draw hiện chỉ generate:
  - `SINGLE_ELIMINATION`
  - `REPECHAGE`

### Đánh giá

- **Thiếu support format quốc tế quan trọng**

### Mức ưu tiên

- **P1**

### Cần sửa gì

1. Implement round-robin/phase group logic.
2. Implement team kumite match resolution theo bout victories / points / extra bout.
3. Bổ sung `HIKIWAKE` ở cấp bout.

## 3.12 Contact tolerance và age-specific adjudication

### Luật WKF

WKF 2024 / exam 2024 nhấn mạnh:

- Senior: light touch với JODAN punches; tolerance lớn hơn cho JODAN kicks
- Cadet/Junior: JODAN kicks cho phép skin touch nếu không gây injury
- U14 có yêu cầu riêng về protective helmet

### Hệ thống hiện tại

Hệ thống hiện chỉ ghi nhận:

- score
- penalty

Hệ thống **không có rule support theo age/category** cho:

- skin touch
- excessive contact threshold
- protective-equipment compliance

### Đánh giá

- **Không phải bug nếu hệ thống chỉ là scoreboard thủ công**
- nhưng là **gap lớn** nếu mục tiêu là trợ lý điều hành chuẩn luật quốc tế

### Mức ưu tiên

- **P2**

### Cần sửa gì

1. Xác định rõ phạm vi sản phẩm:
   - chỉ là recording tool
   - hay là rules-assist tool
2. Nếu là rules-assist:
   - bổ sung profile theo age group
   - bổ sung checklists / prompts / foul subtype UI

## 4. Bảng tổng hợp gap

| Hạng mục | Mức hiện tại | Mức gap | Ưu tiên |
|---|---|---|---|
| Scoring 1/2/3 và chênh 8 điểm | Có | Nhỏ | P1 |
| Tie-break theo IPPON/WAZA-ARI | Thiếu | Lớn | P0 |
| Senshu chuẩn WKF | Thiếu nhiều | Lớn | P0 |
| Annulment/TORIMASEN | Thiếu | Lớn | P0 |
| Hikiwake | Thiếu | Lớn | P1 |
| Video Review | UI placeholder | Lớn | P0 |
| 10-second rule | Thiếu | Lớn | P0 |
| Phân biệt KIKEN/HANSOKU/SHIKKAKU | Sai nghĩa | Lớn | P0 |
| Duration theo age group | Có cấu hình nhưng chưa apply đúng | Lớn | P0 |
| RulesetVersion điều khiển logic | Chưa có | Lớn | P1 |
| Round-robin / team kumite | Thiếu | Lớn | P1 |
| Contact tolerance theo nhóm tuổi | Chưa hỗ trợ | Trung bình | P2 |

## 5. Đề xuất lộ trình sửa

## Phase 1 — Bắt buộc nếu muốn gần chuẩn WKF

1. Sửa propagation thời lượng trận từ category vào `KumiteMatchState`.
2. Sửa `KumiteRuleEngine`:
   - tách `KIKEN`, `HANSOKU`, `SHIKKAKU`
   - thêm tie-break `IPPON -> WAZA-ARI -> HANTEI/HIKIWAKE`
3. Thiết kế lại `SENSHU`:
   - auto-award
   - auto-cancel
   - no re-award after late annulment
4. Thêm foul taxonomy chuẩn hơn.
5. Bổ sung test cho các case luật trọng yếu.

## Phase 2 — Hoàn thiện vận hành quốc tế

1. Implement Video Review flow.
2. Implement 10-second rule + medical workflow.
3. Bổ sung `HIKIWAKE` và các rule theo format.

## Phase 3 — Mở rộng ruleset thật sự

1. Biến `RulesetVersion` thành rule profile thật.
2. Bổ sung age-specific behavior và rule prompts.
3. Implement round-robin / team kumite resolution đầy đủ.

## 6. Kết luận cuối

KarateOps hiện **đủ cho tatami scoring cơ bản**, nhưng **chưa đủ để khẳng định compliant với luật kumite quốc tế WKF** nếu dùng cho vận hành ở mức chính thức/quốc tế.

Nếu phải chốt ngắn gọn:

- phần **hiển thị và thao tác cơ bản**: dùng được
- phần **rule enforcement chuẩn WKF**: còn thiếu khá nhiều
- phần **quyết định kết quả theo luật**: hiện mới đúng một phần

Ba việc nên sửa đầu tiên là:

1. sửa `durationMs` theo category
2. sửa `SENSHU + tie-break + KIKEN/HANSOKU/SHIKKAKU`
3. bổ sung `Video Review` và `10-second rule`
