package com.poshhouse.backend.controller;

import com.poshhouse.backend.dto.change.ChangeRequestResponse;
import com.poshhouse.backend.dto.change.ChangeRequestReviewRequest;
import com.poshhouse.backend.entity.ChangeRequestStatus;
import com.poshhouse.backend.security.UserPrincipal;
import com.poshhouse.backend.service.ChangeRequestService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @GetMapping
    public List<ChangeRequestResponse> listRequests(
        @RequestParam(required = false) String month,
        @RequestParam(required = false) ChangeRequestStatus status
    ) {
        return changeRequestService.listRequests(month, status);
    }

    @PostMapping("/{requestId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ChangeRequestResponse reviewRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody ChangeRequestReviewRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return changeRequestService.reviewRequest(requestId, request, principal);
    }
}
