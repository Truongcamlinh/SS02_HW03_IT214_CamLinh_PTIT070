package vn.ptit.librax.borrowings.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class OverdueNotificationController {

    @PostMapping("/overdue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OverdueNotificationResponse notifyOverdue(@RequestBody OverdueNotificationRequest request) {
        return new OverdueNotificationResponse(true, "Notification accepted");
    }
}

record OverdueNotificationRequest(
        Long borrowingId,
        Long memberId,
        String memberEmail,
        String bookTitle,
        Integer overdueDays
) {
}

record OverdueNotificationResponse(
        boolean success,
        String message
) {
}
