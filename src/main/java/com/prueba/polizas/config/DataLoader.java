package com.prueba.polizas.config;

import com.prueba.polizas.domain.EstadoPoliza;
import com.prueba.polizas.domain.EstadoRiesgo;
import com.prueba.polizas.domain.Poliza;
import com.prueba.polizas.domain.Riesgo;
import com.prueba.polizas.domain.TipoPoliza;
import com.prueba.polizas.repository.PolizaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner seed(PolizaRepository polizaRepository) {
        return args -> {
            if (polizaRepository.count() > 0) {
                return;
            }

            Poliza individual = new Poliza();
            individual.setTipo(TipoPoliza.INDIVIDUAL);
            individual.setEstado(EstadoPoliza.ACTIVA);
            individual.setCanonMensual(new BigDecimal("1500000.00"));
            individual.setMesesVigencia(12);
            individual.setPrima(new BigDecimal("18000000.00"));
            individual.setFechaInicio(LocalDate.now().minusMonths(2));
            individual.setFechaFin(individual.getFechaInicio().plusMonths(12));

            Riesgo riesgoIndividual = new Riesgo();
            riesgoIndividual.setDescripcion("Inmueble Calle 100 #15-20, Bogotá");
            riesgoIndividual.setEstado(EstadoRiesgo.ACTIVO);
            individual.addRiesgo(riesgoIndividual);

            Poliza colectiva = new Poliza();
            colectiva.setTipo(TipoPoliza.COLECTIVA);
            colectiva.setEstado(EstadoPoliza.ACTIVA);
            colectiva.setCanonMensual(new BigDecimal("2500000.00"));
            colectiva.setMesesVigencia(12);
            colectiva.setPrima(new BigDecimal("30000000.00"));
            colectiva.setFechaInicio(LocalDate.now().minusMonths(1));
            colectiva.setFechaFin(colectiva.getFechaInicio().plusMonths(12));

            Riesgo r1 = new Riesgo();
            r1.setDescripcion("Apt 101 - Torre Norte");
            r1.setEstado(EstadoRiesgo.ACTIVO);
            colectiva.addRiesgo(r1);

            Riesgo r2 = new Riesgo();
            r2.setDescripcion("Apt 202 - Torre Sur");
            r2.setEstado(EstadoRiesgo.ACTIVO);
            colectiva.addRiesgo(r2);

            polizaRepository.save(individual);
            polizaRepository.save(colectiva);
        };
    }
}
