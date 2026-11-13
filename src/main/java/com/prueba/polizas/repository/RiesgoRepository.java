package com.prueba.polizas.repository;

import com.prueba.polizas.domain.EstadoRiesgo;
import com.prueba.polizas.domain.Riesgo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {

    List<Riesgo> findByPolizaId(Long polizaId);

    long countByPolizaIdAndEstado(Long polizaId, EstadoRiesgo estado);
}
