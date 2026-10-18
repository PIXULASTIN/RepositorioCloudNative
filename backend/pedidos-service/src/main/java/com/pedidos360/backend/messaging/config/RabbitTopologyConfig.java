package com.pedidos360.backend.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitTopologyConfig {

    private final MensajeriaProperties props;

    public RabbitTopologyConfig(MensajeriaProperties props) {
        this.props = props;
    }

    @Bean
    public DirectExchange pedidosDirectExchange() {
        return ExchangeBuilder.directExchange(props.exchanges().direct()).durable(true).build();
    }

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return ExchangeBuilder.topicExchange(props.exchanges().topic()).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.exchanges().dlx()).durable(true).build();
    }

    // ---------------- Colas principales (con dead-letter hacia su DLQ) ----------------

    @Bean
    public Queue pedidosAceptadosQueue() {
        return colaConDlq(props.queues().aceptados());
    }

    @Bean
    public Queue pedidosDespachosQueue() {
        return colaConDlq(props.queues().despachos());
    }

    @Bean
    public Queue pedidosNotificacionesQueue() {
        return colaConDlq(props.queues().notificaciones());
    }

    // ---------------- DLQ (una por cola) ----------------

    @Bean
    public Queue pedidosAceptadosDlq() {
        return QueueBuilder.durable(props.dlq(props.queues().aceptados())).build();
    }

    @Bean
    public Queue pedidosDespachosDlq() {
        return QueueBuilder.durable(props.dlq(props.queues().despachos())).build();
    }

    @Bean
    public Queue pedidosNotificacionesDlq() {
        return QueueBuilder.durable(props.dlq(props.queues().notificaciones())).build();
    }

    @Bean
    public Binding bindingAceptados() {
        return BindingBuilder.bind(pedidosAceptadosQueue())
                .to(pedidosDirectExchange()).with(props.routingKeys().aceptado());
    }

    @Bean
    public Binding bindingDespachos() {
        return BindingBuilder.bind(pedidosDespachosQueue())
                .to(pedidosDirectExchange()).with(props.routingKeys().despachado());
    }

    @Bean
    public Binding bindingNotificaciones() {
        return BindingBuilder.bind(pedidosNotificacionesQueue())
                .to(pedidosTopicExchange()).with(props.routingKeys().notificaciones());
    }

    @Bean
    public Binding bindingDlqAceptados() {
        return bindDlq(pedidosAceptadosDlq());
    }

    @Bean
    public Binding bindingDlqDespachos() {
        return bindDlq(pedidosDespachosDlq());
    }

    @Bean
    public Binding bindingDlqNotificaciones() {
        return bindDlq(pedidosNotificacionesDlq());
    }


    private Queue colaConDlq(String nombre) {
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.dlq(nombre))
                .build();
    }

    private Binding bindDlq(Queue dlq) {
        return BindingBuilder.bind(dlq).to(deadLetterExchange()).with(dlq.getName());
    }
}
