package com.trip.stay.dataaccess.entity;

import com.trip.support.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomType extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "room_type_id", nullable = false)
    private UUID id;

    @Column(name = "room_type_code", nullable = false, length = 64)
    private String roomTypeCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "max_occupancy", nullable = false)
    private int maxOccupancy;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "stay_id", nullable = false)
    private UUID stayId;

    @Builder
    private RoomType(UUID stayId, String roomTypeCode, String name, int maxOccupancy) {
        this.stayId = stayId;
        this.roomTypeCode = roomTypeCode;
        this.name = name;
        this.maxOccupancy = maxOccupancy;
        this.active = true;
    }

    public void sync(String name, int maxOccupancy, LocalDateTime syncedAt) {
        this.name = name;
        this.maxOccupancy = maxOccupancy;
        this.active = true;
        markSynced(syncedAt);
    }

    public void markAsNonExist() {
        this.active = false;
    }
}
