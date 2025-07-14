package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.carts.domain.Carts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;


public interface CartRepository extends JpaRepository <Carts, String> {


    @Query("SELECT c FROM Carts c WHERE c.buyers.userId = :buyerId")
    Optional<Carts> findByBuyerId(String buyerId);


    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Carts c WHERE c.buyers.userId = :buyerId")
    boolean existsByBuyerId(String buyerId);
}
