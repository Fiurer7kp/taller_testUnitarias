package com.hotel.Hotel.service;

import com.hotel.Hotel.domain.Cliente;
import com.hotel.Hotel.domain.EstadoReserva;
import com.hotel.Hotel.domain.Habitacion;
import com.hotel.Hotel.domain.HabitacionEstandar;
import com.hotel.Hotel.domain.RangoFechas;
import com.hotel.Hotel.domain.Reserva;
import com.hotel.Hotel.dto.request.CrearReservaRequest;
import com.hotel.Hotel.dto.response.ReservaResponse;
import com.hotel.Hotel.exception.RecursoNoEncontradoException;
import com.hotel.Hotel.exception.ReglaNegocioException;
import com.hotel.Hotel.mapper.ReservaMapper;
import com.hotel.Hotel.repository.ClienteRepository;
import com.hotel.Hotel.repository.HabitacionRepository;
import com.hotel.Hotel.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private HabitacionRepository habitacionRepository;

    @Mock
    private ReservaMapper reservaMapper;

    @InjectMocks
    private ReservaService reservaService;

    @Test
    void crear_debeCrearReservaCuandoLosDatosSonValidos() {
        UUID clienteId = UUID.randomUUID();
        UUID habitacionId = UUID.randomUUID();
        Cliente cliente = new Cliente("Ana Gómez", "ana@hotel.com");
        Habitacion habitacion = new HabitacionEstandar("101", 2, 150.0, 1);
        LocalDateTime inicio = LocalDateTime.now().plusDays(2);
        LocalDateTime fin = inicio.plusDays(3);
        CrearReservaRequest request = new CrearReservaRequest(clienteId, habitacionId, inicio, fin);

        ReservaResponse esperado = new ReservaResponse(
                UUID.randomUUID(),
                "Ana Gómez",
                "101",
                inicio,
                fin,
                450.0,
                "PENDIENTE"
        );

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(habitacionRepository.findById(habitacionId)).thenReturn(Optional.of(habitacion));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservaMapper.toResponse(any(Reserva.class))).thenReturn(esperado);

        ReservaResponse resultado = reservaService.crear(request);

        assertNotNull(resultado);
        assertEquals("PENDIENTE", resultado.estado());
        assertEquals(450.0, resultado.costoTotal());
        verify(clienteRepository).findById(clienteId);
        verify(habitacionRepository).findById(habitacionId);
        verify(reservaRepository).save(any(Reserva.class));
        verify(reservaMapper).toResponse(any(Reserva.class));
    }

    @Test
    void crear_debeLanzarReglaNegocioCuandoLaFechaFinEsAnteriorAFechaInicio() {
        UUID clienteId = UUID.randomUUID();
        UUID habitacionId = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.now().plusDays(5);
        LocalDateTime fin = inicio.minusDays(1);
        CrearReservaRequest request = new CrearReservaRequest(clienteId, habitacionId, inicio, fin);

        ReglaNegocioException exception = assertThrows(ReglaNegocioException.class,
                () -> reservaService.crear(request));

        assertEquals("La fecha de salida no puede ser anterior a la fecha de entrada.", exception.getMessage());
        verifyNoInteractions(clienteRepository);
        verifyNoInteractions(habitacionRepository);
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void crear_debeLanzarExcepcionCuandoElClienteNoExiste() {
        UUID clienteId = UUID.randomUUID();
        UUID habitacionId = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = inicio.plusDays(2);
        CrearReservaRequest request = new CrearReservaRequest(clienteId, habitacionId, inicio, fin);

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        RecursoNoEncontradoException exception = assertThrows(RecursoNoEncontradoException.class,
                () -> reservaService.crear(request));

        assertTrue(exception.getMessage().contains("Cliente"));
        verify(clienteRepository).findById(clienteId);
        verifyNoInteractions(habitacionRepository);
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void confirmar_debeActualizarEstadoYGuardarReserva() {
        UUID reservaId = UUID.randomUUID();
        Cliente cliente = new Cliente("Luis Pérez", "luis@hotel.com");
        Habitacion habitacion = new HabitacionEstandar("202", 2, 180.0, 2);
        RangoFechas fechas = new RangoFechas(LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(7));
        Reserva reserva = new Reserva(cliente, habitacion, fechas);
        ReservaResponse esperado = new ReservaResponse(
                reservaId,
                "Luis Pérez",
                "202",
                fechas.fechaInicio(),
                fechas.fechaFin(),
                360.0,
                "CONFIRMADA"
        );

        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservaMapper.toResponse(any(Reserva.class))).thenReturn(esperado);

        ReservaResponse resultado = reservaService.confirmar(reservaId);

        assertNotNull(resultado);
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        assertEquals("CONFIRMADA", resultado.estado());
        verify(reservaRepository).save(reserva);
        verify(reservaMapper).toResponse(reserva);
    }

    @Test
    void cancelar_debeActualizarEstadoYGuardarReserva() {
        UUID reservaId = UUID.randomUUID();
        Cliente cliente = new Cliente("Marta Silva", "marta@hotel.com");
        Habitacion habitacion = new HabitacionEstandar("303", 3, 220.0, 2);
        RangoFechas fechas = new RangoFechas(LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(12));
        Reserva reserva = new Reserva(cliente, habitacion, fechas);
        ReservaResponse esperado = new ReservaResponse(
                reservaId,
                "Marta Silva",
                "303",
                fechas.fechaInicio(),
                fechas.fechaFin(),
                440.0,
                "CANCELADA"
        );

        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservaMapper.toResponse(any(Reserva.class))).thenReturn(esperado);

        ReservaResponse resultado = reservaService.cancelar(reservaId);

        assertNotNull(resultado);
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertEquals("CANCELADA", resultado.estado());
        verify(reservaRepository).save(reserva);
        verify(reservaMapper).toResponse(reserva);
    }
}