# Bài 3: Chuyển đổi từ SOA sang Microservice Architecture bằng REST API
## 1. Tình huống

Trong LibraX, `borrowing-service` cần lấy tên sách từ `book-service`. Code cũ gọi thẳng vào một địa chỉ IP:

```java
String url = "http://192.168.1.15:8082/api/books/" + bookId;
return restTemplate.getForObject(url, String.class);
```

Đoạn này chạy được khi máy dev chỉ có một `book-service`. Nhưng khi deploy thật, `book-service` có thể chạy 2, 3 hoặc nhiều instance. IP của instance cũng có thể thay đổi khi restart container. Vì thế gọi IP cứng là không ổn.

## 2. Lỗi của cách gọi IP cứng

- Chỉ gọi được một instance, không chia tải cho các instance còn lại.
- Khi instance `192.168.1.15:8082` tắt, request bị lỗi dù các instance khác vẫn còn sống.
- Code phụ thuộc vào hạ tầng thật, khi đổi môi trường phải sửa lại.
- Không tận dụng được Eureka/Service Discovery.

Cách đúng hơn trong MSA là gọi bằng tên service:

```text
http://book-service/api/books/{bookId}/title
```

## 3. Sửa bằng @LoadBalanced RestTemplate

Cần tạo bean:

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

Sau đó inject `RestTemplate` vào client. Khi gọi URL bắt đầu bằng `http://book-service`, LoadBalancer sẽ tìm instance của `book-service` trong Service Discovery.

## 4. Xử lý lỗi khi gọi book-service

Trong hệ thống microservice, service khác có thể bị timeout hoặc trả lỗi. Vì vậy `borrowing-service` không nên để exception làm crash toàn bộ request.

Code sửa:

```java
public String getBookTitle(Long bookId) {
    try {
        String title = restTemplate.getForObject(
                "http://book-service/api/books/{bookId}/title",
                String.class,
                bookId
        );
        return title == null ? "N/A" : title;
    } catch (RestClientException ex) {
        return "N/A";
    }
}
```

## 5. REST API notifyOverdue

Thao tác `notifyOverdue` nên chuyển thành REST API nhẹ:

```http
POST /api/notifications/overdue
```

Request:

```json
{
  "borrowingId": 2001,
  "memberId": 18,
  "memberEmail": "student@example.com",
  "bookTitle": "Domain Driven Design",
  "overdueDays": 4
}
```

Response:

```json
{
  "success": true,
  "message": "Notification accepted"
}
```

## 6. Phân tích SOA và MSA trong LibraX

Khi còn dùng SOA, LibraX có thể để các service giao tiếp qua ESB. Cách này có ưu điểm là tập trung, dễ quản lý luồng tích hợp và có thể chuẩn hóa message giữa các hệ thống. Nếu nghiệp vụ chưa thay đổi nhiều, ESB giúp đội kỹ thuật kiểm soát giao tiếp ở một nơi. Nhưng nhược điểm là mọi service phụ thuộc vào tầng trung gian. Khi muốn sửa luồng mượn sách hoặc gửi thông báo quá hạn, team phải sửa cả phần tích hợp trên ESB nên tốc độ phát triển chậm hơn.

Với MSA, `borrowing-service` gọi trực tiếp `book-service` bằng REST API. Cách này đơn giản hơn về luồng nghiệp vụ, dễ đọc code và giúp từng service tự chủ hơn. Nếu `book-service` cần scale lên nhiều instance, `borrowing-service` không cần biết IP thật, chỉ gọi tên `book-service`. Điều này giúp hệ thống linh hoạt hơn khi deploy bằng container hoặc cloud.

Tuy nhiên MSA không tự động tốt hơn SOA trong mọi trường hợp. Nó làm vận hành khó hơn vì có nhiều service nhỏ, nhiều request qua mạng và lỗi có thể xảy ra ở nhiều điểm. Nếu `book-service` chậm, `borrowing-service` cũng bị ảnh hưởng. Vì vậy cần timeout, try-catch, fallback và log lỗi rõ ràng. Với LibraX, MSA phù hợp nếu hệ thống cần scale độc lập và nhiều team cùng phát triển, nhưng phải đi kèm service discovery và load balancing.

## 7. Kết luận

Địa chỉ IP cố định là nguyên nhân chính khiến code không phù hợp khi `book-service` scale. Cần dùng `@LoadBalanced RestTemplate`, gọi bằng tên logic `book-service`, và có xử lý lỗi để `borrowing-service` không bị lỗi dây chuyền.
