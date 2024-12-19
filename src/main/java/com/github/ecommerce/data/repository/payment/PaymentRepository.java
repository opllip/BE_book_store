package com.github.ecommerce.data.repository.payment;

import com.github.ecommerce.data.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    @Query("SELECT p From Payment p JOIN FETCH p.paymentProducts pp WHERE p.user.userId = :userId")
    List<Payment> findAllByUser_UserId(@Param("userId") Integer userId);

    @Query("SELECT p From Payment p JOIN FETCH p.paymentProducts pp WHERE p.paymentId = :paymentId")
    Optional<Payment> findByIdJoinPaymentProduct(@Param("paymentId") Integer paymentId);
}