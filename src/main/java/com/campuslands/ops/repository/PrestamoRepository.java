package com.campuslands.ops.repository;

import com.campuslands.ops.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findByCamperIdAndEstado(Long camperId, String estado);
    List<Prestamo> findByCamperIdOrderByIdDesc(Long camperId);
}
