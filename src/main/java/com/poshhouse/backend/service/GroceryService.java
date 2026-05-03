package com.poshhouse.backend.service;

import com.poshhouse.backend.dto.grocery.GroceryPurchaseRequest;
import com.poshhouse.backend.dto.grocery.GroceryPurchaseResponse;
import com.poshhouse.backend.entity.GroceryPurchase;
import com.poshhouse.backend.entity.User;
import com.poshhouse.backend.exception.ResourceNotFoundException;
import com.poshhouse.backend.repository.GroceryPurchaseRepository;
import com.poshhouse.backend.repository.UserRepository;
import com.poshhouse.backend.util.MonthWindow;
import com.poshhouse.backend.util.MoneyUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroceryService {

    private final GroceryPurchaseRepository groceryPurchaseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GroceryPurchaseResponse> listPurchases(String month) {
        MonthWindow monthWindow = MonthWindow.from(month);
        return groceryPurchaseRepository.findAllByMonthKeyOrderByPurchaseDateDesc(monthWindow.monthKey())
            .stream()
            .map(GroceryService::toResponse)
            .toList();
    }

    @Transactional
    public GroceryPurchaseResponse createPurchase(GroceryPurchaseRequest request, User requester) {
        Long payerId = request.payerId() != null ? request.payerId() : requester.getId();
        User payer = userRepository.findById(payerId)
            .orElseThrow(() -> new ResourceNotFoundException("Payer not found."));

        GroceryPurchase purchase = groceryPurchaseRepository.save(GroceryPurchase.builder()
            .payer(payer)
            .amount(MoneyUtils.scale(request.amount()))
            .purchaseDate(request.purchaseDate())
            .description(request.description())
            .build());

        return toResponse(purchase);
    }

    @Transactional(readOnly = true)
    public GroceryPurchase getPurchase(Long purchaseId) {
        return groceryPurchaseRepository.findById(purchaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Grocery purchase not found."));
    }

    @Transactional(readOnly = true)
    public List<GroceryPurchase> getPurchases(List<Long> purchaseIds) {
        List<GroceryPurchase> purchases = groceryPurchaseRepository.findAllById(purchaseIds);
        if (purchases.size() != purchaseIds.size()) {
            throw new ResourceNotFoundException("One or more grocery purchases could not be found.");
        }
        return purchases;
    }

    @Transactional
    public void deletePurchases(List<Long> purchaseIds) {
        groceryPurchaseRepository.deleteAllById(purchaseIds);
    }

    public static GroceryPurchaseResponse toResponse(GroceryPurchase purchase) {
        return new GroceryPurchaseResponse(
            purchase.getId(),
            purchase.getPayer().getId(),
            purchase.getPayer().getUsername(),
            MoneyUtils.scale(purchase.getAmount()),
            purchase.getPurchaseDate(),
            purchase.getDescription()
        );
    }
}
