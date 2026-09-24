package com.pedidos360.backend.service;

import com.pedidos360.backend.client.CatalogoClient;
import com.pedidos360.backend.dto.ProductoDto;
import com.pedidos360.backend.model.ItemPedido;
import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private CatalogoClient catalogoClient;

    @InjectMocks
    private PedidoService pedidoService;

    private Pedido pedidoEn(String estado) {
        List<ItemPedido> items = new ArrayList<>();
        items.add(new ItemPedido(1L, "Teclado", 2, 45000.0));
        return Pedido.builder().id(5L).cliente("Ana").creadoPor("ana@x.cl").estado(estado).items(items).build();
    }

    @Test
    void listar_clienteSoloVeLosSuyos() {
        when(pedidoRepository.findByCreadoPor("ana@x.cl")).thenReturn(List.of(pedidoEn("CREADO")));

        assertEquals(1, pedidoService.listar("ana@x.cl", false).size());
        verify(pedidoRepository, never()).findAll();
    }

    @Test
    void listar_staffVeTodos() {
        when(pedidoRepository.findAll()).thenReturn(List.of(pedidoEn("CREADO"), pedidoEn("ACEPTADO")));

        assertEquals(2, pedidoService.listar("op@x.cl", true).size());
    }

    @Test
    void obtener_lanzaNoEncontrado() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> pedidoService.obtener(99L, "ana@x.cl", true));
    }

    @Test
    void obtener_clienteNoPuedeVerPedidoAjeno() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedidoEn("CREADO")));

        assertThrows(AccessDeniedException.class, () -> pedidoService.obtener(5L, "otro@x.cl", false));
    }

    @Test
    void crear_tomaPrecioYNombreDelCatalogo() {
        when(catalogoClient.obtenerProducto(1L)).thenReturn(new ProductoDto(1L, "PROD-002", "Teclado", 30, 45000.0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido entrada = Pedido.builder().items(List.of(new ItemPedido(1L, "hack", 2, 1.0))).build();
        Pedido creado = pedidoService.crear(entrada, "ana@x.cl", "Ana");

        assertEquals("CREADO", creado.getEstado());
        assertEquals("Teclado", creado.getItems().get(0).getNombreProducto());
        assertEquals(45000.0, creado.getItems().get(0).getPrecioUnitario());
    }

    @Test
    void crear_sinItemsFalla() {
        Pedido vacio = Pedido.builder().build();
        assertThrows(IllegalArgumentException.class, () -> pedidoService.crear(vacio, "ana@x.cl", "Ana"));
    }

    @Test
    void cambiarEstado_noSePuedeDespacharSinAceptar() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedidoEn("CREADO")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pedidoService.cambiarEstado(5L, "DESPACHADO"));
        assertTrue(ex.getMessage().contains("ACEPTADO"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_aceptarDescuentaStock() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedidoEn("CREADO")));
        when(catalogoClient.obtenerProducto(1L)).thenReturn(new ProductoDto(1L, "PROD-002", "Teclado", 30, 45000.0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = pedidoService.cambiarEstado(5L, "ACEPTADO");

        assertEquals("ACEPTADO", resultado.getEstado());
        verify(catalogoClient).descontarStock(1L, 2);
    }

    @Test
    void cambiarEstado_aceptarSinStockNoCambiaNada() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedidoEn("CREADO")));
        when(catalogoClient.obtenerProducto(1L)).thenReturn(new ProductoDto(1L, "PROD-002", "Teclado", 1, 45000.0));

        assertThrows(IllegalStateException.class, () -> pedidoService.cambiarEstado(5L, "ACEPTADO"));
        verify(catalogoClient, never()).descontarStock(anyLong(), anyInt());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_flujoCompletoValido() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedidoEn("ACEPTADO")));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("EN_PREPARACION", pedidoService.cambiarEstado(5L, "EN_PREPARACION").getEstado());
    }
}
