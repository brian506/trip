package com.trip.stay.dataaccess.repository;

import com.trip.stay.dataaccess.entity.RoomType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    List<RoomType> findAllByStayIdIn(Collection<UUID> stayIds);
}
