package com.nabta.backend.repository;

import com.nabta.backend.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByMerchantReference(String merchantReference);
    Optional<PaymentTransaction> findByProviderCodeAndProviderPaymentId(String providerCode, String providerPaymentId);
}
