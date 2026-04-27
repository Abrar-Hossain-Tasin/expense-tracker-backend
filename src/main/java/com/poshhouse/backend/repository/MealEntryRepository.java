package com.poshhouse.backend.repository;

import com.poshhouse.backend.entity.MealEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealEntryRepository extends JpaRepository<MealEntry, Long> {

    @Query("""
        select m
        from MealEntry m
        join fetch m.user
        where m.date between :start and :end
        order by m.date asc, m.user.username asc
        """)
    List<MealEntry> findAllWithUsersByDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    Optional<MealEntry> findByUserIdAndDate(Long userId, LocalDate date);
}
