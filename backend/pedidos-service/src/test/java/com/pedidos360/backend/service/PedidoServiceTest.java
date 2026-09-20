package com.pedidos360.backend.service;

import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PedidoService pedidoService;

    @Test
    void listar_debeRetornarTodosLosPedidos() {
        Pedido p1 = Pedido.builder().id(1L).cliente("Juan Perez").estado("PENDIENTE").build();
        Pedido p2 = Pedido.builder().id(2L).cliente("Maria Soto").estado("DESPACHADO").build();
        when(pedidoRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Pedido> resultado = pedidoService.listar();

        assertEquals(2, resultado.size());
        verify(pedidoRepository).findAll();
    }

    @Test
    void crear_debeGuardarYRetornarElPedido() {
        Pedido nuevo = Pedido.builder().cliente("Ana Diaz").build();
        Pedido guardado = Pedido.builder().id(10L).cliente("Ana Diaz").estado("PENDIENTE").build();
        when(pedidoRepository.save(nuevo)).thenReturn(guardado);

        Pedido resultado = pedidoService.crear(nuevo);

        assertEquals(10L, resultado.getId());
        assertEquals("PENDIENTE", resultado.getEstado());
    }

    @Test
    void obtener_debeLanzarExcepcion_siNoExiste() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        try {
            pedidoService.obtener(99L);
            assert false : "deberia haber lanzado una excepcion";
        } catch (RuntimeException e) {
            assertEquals("Pedido no encontrado: 99", e.getMessage());
        }
    }
}
