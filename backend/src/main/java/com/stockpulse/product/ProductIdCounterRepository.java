package com.stockpulse.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ProductIdCounterRepository extends JpaRepository<ProductIdCounter, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductIdCounter> findById(String name);
}