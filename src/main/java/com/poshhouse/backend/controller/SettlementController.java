package com.poshhouse.backend.controller;

import com.poshhouse.backend.dto.settlement.SettlementResponse;
import com.poshhouse.backend.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public SettlementResponse getSettlement(@RequestParam(required = false) String month) {
        return settlementService.getSettlement(month);
    }
}
