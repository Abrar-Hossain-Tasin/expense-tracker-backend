package com.poshhouse.backend.repository;

import com.poshhouse.backend.entity.ChangeRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    List<ChangeRequest> findAllByOrderByRequestedAtDesc();
}
