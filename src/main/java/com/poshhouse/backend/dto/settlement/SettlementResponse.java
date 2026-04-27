package com.poshhouse.backend.dto.settlement;

import java.util.List;

public record SettlementResponse(
    String month,
    SettlementSummaryDto summary,
    List<SettlementUserDetailDto> userDetails
) {
}
