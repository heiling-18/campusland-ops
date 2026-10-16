package com.campuslands.ops.repository;

import com.campuslands.ops.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByEspacioIdAndFechaAndEstadoNot(Long espacioId, String fecha, String estadoExcluido);
    List<Reserva> findByEspacioId(Long espacioId);
    List<Reserva> findByUsuarioIdOrderByIdDesc(Long usuarioId);
    List<Reserva> findByEstadoOrderByIdDesc(String estado);
}
