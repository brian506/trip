package com.trip.stay.dataaccess.entity;

import com.trip.stay.vo.Supplier;
import com.trip.support.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
public class Stay extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "stay_id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier", nullable = false, length = 16)
    private Supplier supplier;

    @Column(name = "stay_code", nullable = false, length = 64)
    private String stayCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    private Stay(Supplier supplier, String stayCode, String name) {
        this.supplier = supplier;
        this.stayCode = stayCode;
        this.name = name;
        this.active = true;
    }

    public boolean applyLatestInfo(String name) {
        if (this.name.equals(name) && this.active) {
            return false;
        }
        this.name = name;
        this.active = true;
        return true;
    }

    public void markAsNonExist() {
        this.active = false;
    }
}
