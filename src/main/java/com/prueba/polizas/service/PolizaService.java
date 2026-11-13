package com.prueba.polizas.service;

import com.prueba.polizas.domain.EstadoPoliza;
import com.prueba.polizas.domain.EstadoRiesgo;
import com.prueba.polizas.domain.Poliza;
import com.prueba.polizas.domain.Riesgo;
import com.prueba.polizas.domain.TipoPoliza;
import com.prueba.polizas.dto.CrearRiesgoRequest;
import com.prueba.polizas.dto.PolizaResponse;
import com.prueba.polizas.dto.RiesgoResponse;
import com.prueba.polizas.exception.BusinessException;
import com.prueba.polizas.exception.ResourceNotFoundException;
import com.prueba.polizas.repository.PolizaRepository;
import com.prueba.polizas.repository.RiesgoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final CoreIntegrationService coreIntegrationService;
    private final BigDecimal ipcIncrement;

    public PolizaService(PolizaRepository polizaRepository,
                         RiesgoRepository riesgoRepository,
                         CoreIntegrationService coreIntegrationService,
                         @Value("${app.ipc-increment}") BigDecimal ipcIncrement) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.coreIntegrationService = coreIntegrationService;
        this.ipcIncrement = ipcIncrement;
    }

    @Transactional(readOnly = true)
    public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
        List<Poliza> polizas;
        if (tipo != null && estado != null) {
            polizas = polizaRepository.findByTipoAndEstado(tipo, estado);
        } else if (tipo != null) {
            polizas = polizaRepository.findByTipo(tipo);
        } else if (estado != null) {
            polizas = polizaRepository.findByEstado(estado);
        } else {
            polizas = polizaRepository.findAll();
        }
        return polizas.stream().map(PolizaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarRiesgos(Long polizaId) {
        obtenerPoliza(polizaId);
        return riesgoRepository.findByPolizaId(polizaId).stream()
                .map(RiesgoResponse::from)
                .toList();
    }

    @Transactional
    public PolizaResponse renovar(Long polizaId) {
        Poliza poliza = obtenerPoliza(polizaId);
        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessException("No se puede renovar una póliza cancelada");
        }

        BigDecimal factor = BigDecimal.ONE.add(ipcIncrement);
        BigDecimal nuevoCanon = poliza.getCanonMensual()
                .multiply(factor)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevaPrima = nuevoCanon
                .multiply(BigDecimal.valueOf(poliza.getMesesVigencia()))
                .setScale(2, RoundingMode.HALF_UP);

        poliza.setCanonMensual(nuevoCanon);
        poliza.setPrima(nuevaPrima);
        poliza.setFechaInicio(poliza.getFechaFin());
        poliza.setFechaFin(poliza.getFechaInicio().plusMonths(poliza.getMesesVigencia()));
        poliza.setEstado(EstadoPoliza.RENOVADA);

        Poliza guardada = polizaRepository.save(poliza);
        coreIntegrationService.registrarEvento("ACTUALIZACION", guardada.getId());
        return PolizaResponse.from(guardada);
    }

    @Transactional
    public PolizaResponse cancelar(Long polizaId) {
        Poliza poliza = obtenerPoliza(polizaId);
        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessException("La póliza ya se encuentra cancelada");
        }

        poliza.setEstado(EstadoPoliza.CANCELADA);
        for (Riesgo riesgo : poliza.getRiesgos()) {
            riesgo.setEstado(EstadoRiesgo.CANCELADO);
        }

        Poliza guardada = polizaRepository.save(poliza);
        coreIntegrationService.registrarEvento("ACTUALIZACION", guardada.getId());
        return PolizaResponse.from(guardada);
    }

    @Transactional
    public RiesgoResponse agregarRiesgo(Long polizaId, CrearRiesgoRequest request) {
        Poliza poliza = obtenerPoliza(polizaId);

        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessException("No se pueden agregar riesgos a una póliza cancelada");
        }
        if (poliza.getTipo() != TipoPoliza.COLECTIVA) {
            throw new BusinessException("Solo las pólizas colectivas permiten agregar riesgos. Una individual solo puede tener 1 riesgo");
        }

        Riesgo riesgo = new Riesgo();
        riesgo.setDescripcion(request.descripcion());
        riesgo.setEstado(EstadoRiesgo.ACTIVO);
        poliza.addRiesgo(riesgo);

        Poliza guardada = polizaRepository.save(poliza);
        Riesgo creado = guardada.getRiesgos().get(guardada.getRiesgos().size() - 1);
        coreIntegrationService.registrarEvento("ACTUALIZACION", guardada.getId());
        return RiesgoResponse.from(creado);
    }

    @Transactional
    public RiesgoResponse cancelarRiesgo(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new ResourceNotFoundException("Riesgo no encontrado: " + riesgoId));

        if (riesgo.getEstado() == EstadoRiesgo.CANCELADO) {
            throw new BusinessException("El riesgo ya se encuentra cancelado");
        }

        Poliza poliza = riesgo.getPoliza();
        if (poliza.getTipo() == TipoPoliza.INDIVIDUAL) {
            long activos = riesgoRepository.countByPolizaIdAndEstado(poliza.getId(), EstadoRiesgo.ACTIVO);
            if (activos <= 1) {
                throw new BusinessException("Una póliza individual debe conservar su único riesgo activo; cancele la póliza completa");
            }
        }

        riesgo.setEstado(EstadoRiesgo.CANCELADO);
        Riesgo guardado = riesgoRepository.save(riesgo);
        coreIntegrationService.registrarEvento("ACTUALIZACION", poliza.getId());
        return RiesgoResponse.from(guardado);
    }

    private Poliza obtenerPoliza(Long polizaId) {
        return polizaRepository.findById(polizaId)
                .orElseThrow(() -> new ResourceNotFoundException("Póliza no encontrada: " + polizaId));
    }
}
