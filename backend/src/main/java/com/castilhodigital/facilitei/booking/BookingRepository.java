package com.castilhodigital.facilitei.booking;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findBySlotId(Long slotId);

    Optional<Booking> findByAsaasPaymentId(String asaasPaymentId);

}
