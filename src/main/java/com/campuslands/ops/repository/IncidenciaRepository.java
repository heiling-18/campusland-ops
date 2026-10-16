package com.campuslands.ops.repository;

import com.campuslands.ops.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    List<Incidencia> findByEspacioNombreIgnoreCaseAndEstadoAndPrioridadIa(String espacioNombre, String estado, String prioridadIa);
}
