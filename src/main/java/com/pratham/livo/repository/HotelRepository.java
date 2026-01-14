package com.pratham.livo.repository;

import com.pratham.livo.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel,Long> {
    long countByOwnerIdAndDeletedFalse(Long id);
}
