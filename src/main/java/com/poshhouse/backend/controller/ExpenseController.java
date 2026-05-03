package com.poshhouse.backend.controller;

import com.poshhouse.backend.dto.change.ChangeRequestResponse;
import com.poshhouse.backend.dto.expense.BulkDeleteExpenseRequest;
import com.poshhouse.backend.dto.expense.ExpenseRequest;
import com.poshhouse.backend.dto.expense.ExpenseResponse;
import com.poshhouse.backend.security.UserPrincipal;
import com.poshhouse.backend.service.ChangeRequestService;
import com.poshhouse.backend.service.ExpenseService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ChangeRequestService changeRequestService;

    @GetMapping
    public List<ExpenseResponse> listExpenses(@RequestParam(required = false) String month) {
        return expenseService.listExpenses(month);
    }

    @PostMapping
    public ChangeRequestResponse createExpense(
        @Valid @RequestBody ExpenseRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return changeRequestService.submitExpenseCreateRequest(request, principal);
    }

    @PostMapping("/bulk-delete")
    public ChangeRequestResponse bulkDelete(
        @Valid @RequestBody BulkDeleteExpenseRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return changeRequestService.submitExpenseDeleteRequest(request, principal);
    }
}
