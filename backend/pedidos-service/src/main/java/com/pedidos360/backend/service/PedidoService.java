package com.pedidos360.backend.service;

import com.pedidos360.backend.client.CatalogoClient;
import com.pedidos360.backend.dto.ProductoDto;
import com.pedidos360.backend.model.EstadoPedido;
import com.pedidos360.backend.model.ItemPedido;
import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.repository.PedidoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CatalogoClient catalogoClient;

    public PedidoService(PedidoRepository pedidoRepository, CatalogoClient catalogoClient) {
        this.pedidoRepository = pedidoRepository;
        this.catalogoClient = catalogoClient;
    }

    /** ADMIN/OPERADOR ven todos; CLIENTE solo los que el mismo creo. */
    public List<Pedido> listar(String usuario, boolean veTodos) {
        return veTodos ? pedidoRepository.findAll() : pedidoRepository.findByCreadoPor(usuario);
    }

    public Pedido obtener(Long id, String usuario, boolean veTodos) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado: " + id));
        if (!veTodos && !usuario.equalsIgnoreCase(pedido.getCreadoPor())) {
            throw new AccessDeniedException("No puedes ver pedidos de otros usuarios");
        }
        return pedido;
    }

    public Pedido crear(Pedido entrada, String usuario, String cliente) {
        if (entrada.getItems() == null || entrada.getItems().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un producto");
        }
        List<ItemPedido> items = new ArrayList<>();
        for (ItemPedido it : entrada.getItems()) {
            if (it.getProductoId() == null || it.getCantidad() == null || it.getCantidad() <= 0) {
                throw new IllegalArgumentException("Cada item necesita productoId y cantidad mayor a 0");
            }
            // Nombre y precio se toman del catalogo (no se confia en lo que manda el navegador)
            ProductoDto prod = catalogoClient.obtenerProducto(it.getProductoId());
            items.add(new ItemPedido(prod.id(), prod.nombre(), it.getCantidad(), prod.precio()));
        }
        Pedido nuevo = Pedido.builder()
                .cliente(cliente)
                .creadoPor(usuario)
                .estado(EstadoPedido.CREADO.name())
                .items(items)
                .build();
        return pedidoRepository.save(nuevo);
    }

    @Transactional
    public Pedido cambiarEstado(Long id, String estadoNuevo) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado: " + id));

        EstadoPedido actual = parsear(pedido.getEstado());
        EstadoPedido destino = parsear(estadoNuevo);

        // Regla de negocio minima: no se despacha lo que no fue aceptado
        if (destino == EstadoPedido.DESPACHADO && actual == EstadoPedido.CREADO) {
            throw new IllegalStateException("No se puede DESPACHAR un pedido que no fue ACEPTADO");
        }
        if (!actual.puedePasarA(destino)) {
            throw new IllegalStateException("Transicion no permitida: " + actual + " -> " + destino);
        }
        if (destino == EstadoPedido.ACEPTADO) {
            descontarStock(pedido);
        }
        pedido.setEstado(destino.name());
        return pedidoRepository.save(pedido);
    }

    // Regla de negocio: al ACEPTAR un pedido baja el stock en el catalogo.
    // Primero se valida todo y despues se descuenta, para no dejar descuentos a medias
    // en el caso mas comun (falta de stock).
    private void descontarStock(Pedido pedido) {
        for (ItemPedido it : pedido.getItems()) {
            ProductoDto prod = catalogoClient.obtenerProducto(it.getProductoId());
            if (prod.stockDisponible() < it.getCantidad()) {
                throw new IllegalStateException("Stock insuficiente para " + prod.nombre()
                        + " (disponible: " + prod.stockDisponible() + ", pedido: " + it.getCantidad() + ")");
            }
        }
        for (ItemPedido it : pedido.getItems()) {
            catalogoClient.descontarStock(it.getProductoId(), it.getCantidad());
        }
    }

    private EstadoPedido parsear(String valor) {
        try {
            return EstadoPedido.valueOf(valor.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Estado desconocido: " + valor);
        }
    }
}
