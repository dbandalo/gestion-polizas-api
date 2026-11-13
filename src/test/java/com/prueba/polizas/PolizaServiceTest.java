package com.prueba.polizas;

import com.prueba.polizas.domain.EstadoPoliza;
import com.prueba.polizas.domain.EstadoRiesgo;
import com.prueba.polizas.domain.Poliza;
import com.prueba.polizas.domain.Riesgo;
import com.prueba.polizas.domain.TipoPoliza;
import com.prueba.polizas.dto.CrearRiesgoRequest;
import com.prueba.polizas.exception.BusinessException;
import com.prueba.polizas.repository.PolizaRepository;
import com.prueba.polizas.repository.RiesgoRepository;
import com.prueba.polizas.service.CoreIntegrationService;
import com.prueba.polizas.service.PolizaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolizaServiceTest {

    @Mock
    private PolizaRepository polizaRepository;
    @Mock
    private RiesgoRepository riesgoRepository;
    @Mock
    private CoreIntegrationService coreIntegrationService;

    private PolizaService polizaService;

    @BeforeEach
    void setUp() {
        polizaService = new PolizaService(
                polizaRepository,
                riesgoRepository,
                coreIntegrationService,
                new BigDecimal("0.05")
        );
    }

    @Test
    void noRenuevaPolizaCancelada() {
        Poliza poliza = basePoliza(TipoPoliza.INDIVIDUAL);
        poliza.setEstado(EstadoPoliza.CANCELADA);
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza));

        assertThrows(BusinessException.class, () -> polizaService.renovar(1L));
    }

    @Test
    void renovarIncrementaPorIpcYNotificaCore() {
        Poliza poliza = basePoliza(TipoPoliza.INDIVIDUAL);
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza));
        when(polizaRepository.save(any(Poliza.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = polizaService.renovar(1L);

        assertEquals(EstadoPoliza.RENOVADA, response.estado());
        assertEquals(new BigDecimal("1050000.00"), response.canonMensual());
        assertEquals(new BigDecimal("12600000.00"), response.prima());
        verify(coreIntegrationService).registrarEvento("ACTUALIZACION", 1L);
    }

    @Test
    void noAgregaRiesgoEnIndividual() {
        Poliza poliza = basePoliza(TipoPoliza.INDIVIDUAL);
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza));

        assertThrows(BusinessException.class,
                () -> polizaService.agregarRiesgo(1L, new CrearRiesgoRequest("Nuevo")));
    }

    @Test
    void cancelarPolizaCancelaRiesgos() {
        Poliza poliza = basePoliza(TipoPoliza.COLECTIVA);
        Riesgo riesgo = new Riesgo();
        riesgo.setDescripcion("R1");
        riesgo.setEstado(EstadoRiesgo.ACTIVO);
        poliza.addRiesgo(riesgo);

        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza));
        when(polizaRepository.save(any(Poliza.class))).thenAnswer(inv -> inv.getArgument(0));

        polizaService.cancelar(1L);

        ArgumentCaptor<Poliza> captor = ArgumentCaptor.forClass(Poliza.class);
        verify(polizaRepository).save(captor.capture());
        assertEquals(EstadoPoliza.CANCELADA, captor.getValue().getEstado());
        assertEquals(EstadoRiesgo.CANCELADO, captor.getValue().getRiesgos().get(0).getEstado());
    }

    private Poliza basePoliza(TipoPoliza tipo) {
        Poliza poliza = new Poliza();
        poliza.setId(1L);
        poliza.setTipo(tipo);
        poliza.setEstado(EstadoPoliza.ACTIVA);
        poliza.setCanonMensual(new BigDecimal("1000000.00"));
        poliza.setPrima(new BigDecimal("12000000.00"));
        poliza.setMesesVigencia(12);
        poliza.setFechaInicio(LocalDate.of(2026, 1, 1));
        poliza.setFechaFin(LocalDate.of(2027, 1, 1));
        return poliza;
    }
}
