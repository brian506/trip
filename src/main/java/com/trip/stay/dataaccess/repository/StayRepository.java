package com.trip.stay.dataaccess.repository;

import com.trip.stay.dataaccess.entity.Stay;
import com.trip.stay.vo.Supplier;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StayRepository extends JpaRepository<Stay, UUID> {

    List<Stay> findAllBySupplier(Supplier supplier);
}
