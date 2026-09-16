package com.trip.stay.dataaccess.repository;

import com.trip.stay.dataaccess.entity.RoomType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    List<RoomType> findAllByStayIdIn(Set<UUID> stayIds);

    @Query("""
            SELECT rt
            FROM RoomType rt
            WHERE rt.stayId IN :stayIds
              AND rt.maxOccupancy >= :guestCount
            """)
    List<RoomType> findAccommodatable(@Param("stayIds") Set<UUID> stayIds,
                                      @Param("guestCount") int guestCount);
}
