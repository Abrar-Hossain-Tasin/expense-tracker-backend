package com.poshhouse.backend.repository;

import com.poshhouse.backend.entity.HouseExpense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseExpenseRepository extends JpaRepository<HouseExpense, Long> {

    @EntityGraph(attributePaths = {"createdBy", "splits", "splits.user"})
    List<HouseExpense> findAllByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate start, LocalDate end);
}
