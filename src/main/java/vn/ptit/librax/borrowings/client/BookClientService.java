package vn.ptit.librax.borrowings.client;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class BookClientService {

    private final RestTemplate restTemplate;

    public BookClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getBookTitle(Long bookId) {
        try {
            String title = restTemplate.getForObject(
                    "http://book-service/api/books/{bookId}/title",
                    String.class,
                    bookId
            );
            return title == null ? "N/A" : title;
        } catch (RestClientException exception) {
            return "N/A";
        }
    }
}
