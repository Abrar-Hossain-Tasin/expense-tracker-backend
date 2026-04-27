package com.poshhouse.backend.repository;

import com.poshhouse.backend.entity.GroceryPurchase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroceryPurchaseRepository extends JpaRepository<GroceryPurchase, Long> {

    List<GroceryPurchase> findAllByMonthKeyOrderByPurchaseDateDesc(String monthKey);
}
