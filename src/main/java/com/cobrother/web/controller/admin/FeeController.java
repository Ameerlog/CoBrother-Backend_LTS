package com.cobrother.web.controller.admin;

import com.cobrother.web.service.admin.AdminService;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/fee")
public class FeeController {

    @Autowired private AdminService adminService;
    @Autowired private CurrentUserService currentUserService;

    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyFeeRequests() {
        return adminService.getMyFeeRequests(currentUserService.getCurrentUser());
    }

    @PostMapping("/requests/{requestId}/create-order")
    public ResponseEntity<?> createOrder(@PathVariable Long requestId) {
        return adminService.createFeeOrder(requestId, currentUserService.getCurrentUser());
    }

    @PostMapping("/requests/{requestId}/verify")
    public ResponseEntity<?> verify(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body) {
        return adminService.verifyFeePayment(
                requestId,
                body.get("razorpayPaymentId"),
                body.get("razorpayOrderId"),
                body.get("razorpaySignature"),
                currentUserService.getCurrentUser()
        );
    }

    @PostMapping("/requests/{requestId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long requestId) {
        return adminService.cancelFeeRequest(requestId, currentUserService.getCurrentUser());
    }
}