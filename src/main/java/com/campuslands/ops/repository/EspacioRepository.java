package com.campuslands.ops.repository;

import com.campuslands.ops.model.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {
    Optional<Espacio> findByNombreIgnoreCase(String nombre);
    List<Espacio> findByCapacidadLessThanEqualOrderByCapacidadAsc(Integer capacidadMaxima);
}
