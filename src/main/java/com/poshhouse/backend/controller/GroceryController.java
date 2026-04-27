package com.poshhouse.backend.controller;

import com.poshhouse.backend.dto.grocery.GroceryPurchaseRequest;
import com.poshhouse.backend.dto.grocery.GroceryPurchaseResponse;
import com.poshhouse.backend.security.UserPrincipal;
import com.poshhouse.backend.service.GroceryService;
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
@RequestMapping("/api/groceries")
@RequiredArgsConstructor
public class GroceryController {

    private final GroceryService groceryService;

    @GetMapping
    public List<GroceryPurchaseResponse> listPurchases(@RequestParam(required = false) String month) {
        return groceryService.listPurchases(month);
    }

    @PostMapping
    public GroceryPurchaseResponse createPurchase(
        @Valid @RequestBody GroceryPurchaseRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return groceryService.createPurchase(request, principal);
    }
}
