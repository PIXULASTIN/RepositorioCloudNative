package com.pedidos360.catalogo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku; // codigo unico del producto, ej: "PROD-001"

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Integer stockDisponible;

    @Column(nullable = false)
    private Double precio;

    @PrePersist
    public void prePersist() {
        if (stockDisponible == null) {
            stockDisponible = 0;
        }
    }
}
