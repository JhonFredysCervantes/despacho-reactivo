package com.despachoreactivo.project.service;

import com.despachoreactivo.project.model.Despacho;
import com.despachoreactivo.project.model.Paquete;
import com.despachoreactivo.project.repository.DespachoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private DespachoRepository despachoRepository;

    @InjectMocks
    private ReportService reportService;

    private Despacho despacho(String ciudad, String estado, int pesoKg, double tarifa) {
        Despacho despacho = new Despacho();
        despacho.setCiudad(ciudad);
        despacho.setEstado(estado);
        despacho.setTarifaTotal(tarifa);

        Paquete paquete = new Paquete();
        paquete.setPesoKg(pesoKg);
        despacho.setPaquetes(List.of(paquete));

        return despacho;
    }

    @Test
    void obtenerReportePorCiudad_soloConsideraDespachosEnRutaOEntregados() {
        Despacho enRuta = despacho("BOG", "EN_RUTA", 100, 1000.0);
        Despacho entregado = despacho("BOG", "ENTREGADO", 50, 500.0);
        Despacho recibido = despacho("BOG", "RECIBIDO", 999, 9999.0);
        Despacho cancelado = despacho("MDE", "CANCELADO", 999, 9999.0);

        when(despachoRepository.findAll())
                .thenReturn(Flux.just(enRuta, entregado, recibido, cancelado));

        StepVerifier.create(reportService.obtenerReportePorCiudad())
                .assertNext(reporte -> {
                    assertThat(reporte).containsKey("BOG");
                    assertThat(reporte).doesNotContainKey("MDE");

                    ReportService.ReporteCiudad reporteBogota = reporte.get("BOG");
                    assertThat(reporteBogota.pesoKgTotal).isEqualTo(150);
                    assertThat(reporteBogota.valorTotal).isEqualTo(1500.0);
                })
                .verifyComplete();
    }

    @Test
    void obtenerReportePorCiudad_manejaDespachosSinPaquetesNiTarifa() {
        Despacho sinDatos = new Despacho();
        sinDatos.setCiudad("CLO");
        sinDatos.setEstado("ENTREGADO");
        sinDatos.setPaquetes(null);
        sinDatos.setTarifaTotal(null);

        when(despachoRepository.findAll()).thenReturn(Flux.just(sinDatos));

        StepVerifier.create(reportService.obtenerReportePorCiudad())
                .assertNext(reporte -> {
                    ReportService.ReporteCiudad reporteClo = reporte.get("CLO");
                    assertThat(reporteClo.pesoKgTotal).isEqualTo(0);
                    assertThat(reporteClo.valorTotal).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    void obtenerReportePorCiudad_acumulaVariosDespachosDeLaMismaCiudad() {
        Despacho primero = despacho("BOG", "EN_RUTA", 100, 1000.0);
        Despacho segundo = despacho("BOG", "EN_RUTA", 200, 2000.0);

        when(despachoRepository.findAll()).thenReturn(Flux.just(primero, segundo));

        StepVerifier.create(reportService.obtenerReportePorCiudad())
                .assertNext(reporte -> {
                    ReportService.ReporteCiudad reporteBogota = reporte.get("BOG");
                    assertThat(reporteBogota.pesoKgTotal).isEqualTo(300);
                    assertThat(reporteBogota.valorTotal).isEqualTo(3000.0);
                })
                .verifyComplete();
    }

    @Test
    void obtenerReportePorCiudadStream_emiteUnaActualizacionPorCadaDespachoRelevante() {
        Despacho primero = despacho("BOG", "EN_RUTA", 100, 1000.0);
        Despacho segundo = despacho("MDE", "ENTREGADO", 50, 500.0);
        Despacho irrelevante = despacho("CLO", "RECIBIDO", 999, 9999.0);

        when(despachoRepository.findAll()).thenReturn(Flux.just(primero, segundo, irrelevante));

        StepVerifier.create(reportService.obtenerReportePorCiudadStream())
                .assertNext(reporte -> {
                    assertThat(reporte.ciudad).isEqualTo("BOG");
                    assertThat(reporte.pesoKgTotal).isEqualTo(100);
                })
                .assertNext(reporte -> {
                    assertThat(reporte.ciudad).isEqualTo("MDE");
                    assertThat(reporte.pesoKgTotal).isEqualTo(50);
                })
                .verifyComplete();
    }
}
