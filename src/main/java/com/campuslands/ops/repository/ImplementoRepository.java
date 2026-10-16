package com.campuslands.ops.repository;

import com.campuslands.ops.model.Implemento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImplementoRepository extends JpaRepository<Implemento, Long> {
    List<Implemento> findByEspacioIdOrderByPuestoNumeroAsc(Long espacioId);
    List<Implemento> findByEspacioNombreIgnoreCaseOrderByPuestoNumeroAsc(String espacioNombre);
    List<Implemento> findByTipoOrderByIdAsc(String tipo);
}
