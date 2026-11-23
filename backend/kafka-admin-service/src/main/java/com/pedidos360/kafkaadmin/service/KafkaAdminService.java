package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.dto.*;
import com.pedidos360.kafkaadmin.exception.KafkaConflictException;
import com.pedidos360.kafkaadmin.exception.KafkaNoDisponibleException;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Administracion de Kafka con AdminClient: topicos, configuraciones, consumer groups, lag y cluster.
 * Traduce los errores de Kafka a excepciones del dominio (404, 409, 400, 503).
 */
@Service
public class KafkaAdminService {

    /** Configuraciones de topico que la API permite crear, modificar y mostrar. */
    static final Set<String> CONFIGS_PERMITIDAS = Set.of(
            "retention.ms", "retention.bytes", "cleanup.policy", "min.insync.replicas",
            "segment.ms", "segment.bytes", "max.message.bytes", "delete.retention.ms",
            "min.cleanable.dirty.ratio");

    private static final Pattern NOMBRE_TOPICO = Pattern.compile("^[a-zA-Z0-9._-]{1,249}$");

    private final Admin admin;
    private final int timeoutSegundos;
    private final Set<String> topicosProtegidos;

    public KafkaAdminService(
            Admin admin,
            @Value("${app.kafka.operation-timeout-seconds:15}") int timeoutSegundos,
            @Value("${app.kafka.protected-topics:}") List<String> topicosProtegidos) {
        this.admin = admin;
        this.timeoutSegundos = timeoutSegundos;
        this.topicosProtegidos = topicosProtegidos.stream()
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------ cluster

    public ClusterInfoResponse infoCluster() {
        DescribeClusterResult r = admin.describeCluster();
        String clusterId = esperar(r.clusterId());
        Node controlador = esperar(r.controller());
        List<BrokerInfo> brokers = esperar(r.nodes()).stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new BrokerInfo(n.id(), n.host(), n.port(), n.rack()))
                .toList();
        return new ClusterInfoResponse(clusterId, controlador == null ? null : controlador.id(), brokers);
    }

    // ------------------------------------------------------------------ topicos

    public List<String> listarTopicos() {
        Set<String> nombres = esperar(admin.listTopics().names());
        return nombres.stream().sorted().toList();
    }

    public TopicInfoResponse describirTopico(String nombre) {
        TopicDescription d = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        if (d == null) {
            throw new NoSuchElementException("El topico no existe: " + nombre);
        }
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all()).get(recurso);

        Map<String, String> configs = new TreeMap<>();
        if (config != null) {
            config.entries().stream()
                    .filter(e -> CONFIGS_PERMITIDAS.contains(e.name()))
                    .forEach(e -> configs.put(e.name(), e.value()));
        }

        List<ParticionInfo> particiones = d.partitions().stream()
                .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                .map(p -> new ParticionInfo(
                        p.partition(),
                        p.leader() == null ? null : p.leader().id(),
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .toList();
        int rf = particiones.isEmpty() ? 0 : particiones.get(0).replicas().size();
        return new TopicInfoResponse(nombre, particiones.size(), rf, particiones, configs);
    }

    public Map<String, Object> crearTopico(TopicRequest req) {
        validarNombre(req.name());
        Map<String, String> configs = validarConfigs(req.configs());

        NewTopic nuevo = new NewTopic(req.name(), req.partitions(), req.replicationFactor().shortValue())
                .configs(configs);
        esperar(admin.createTopics(List.of(nuevo)).all());

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("name", req.name());
        respuesta.put("partitions", req.partitions());
        respuesta.put("replicationFactor", req.replicationFactor());
        respuesta.put("configs", configs);
        return respuesta;
    }

    public TopicInfoResponse actualizarConfig(String nombre, Map<String, String> cambios) {
        validarNombre(nombre);
        Map<String, String> configs = validarConfigs(cambios);
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("Debes indicar al menos una configuracion");
        }

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Collection<AlterConfigOp> operaciones = configs.entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .collect(Collectors.toList());
        Map<ConfigResource, Collection<AlterConfigOp>> peticion = Map.of(recurso, operaciones);

        esperar(admin.incrementalAlterConfigs(peticion).all());
        return describirTopico(nombre);
    }

    public Map<String, Object> aumentarParticiones(String nombre, int total) {
        validarNombre(nombre);
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("name", nombre);
        respuesta.put("partitions", total);
        return respuesta;
    }

    public void eliminarTopico(String nombre) {
        validarNombre(nombre);
        if (topicosProtegidos.contains(nombre)) {
            throw new KafkaConflictException("El topico esta protegido y no se puede eliminar: " + nombre);
        }
        esperar(admin.deleteTopics(List.of(nombre)).all());
    }

    // ------------------------------------------------------------------ consumer groups

    public List<GrupoResumen> listarGrupos() {
        Collection<ConsumerGroupListing> grupos = esperar(admin.listConsumerGroups().all());
        return grupos.stream()
                .map(g -> new GrupoResumen(g.groupId(), g.state().map(Object::toString).orElse("UNKNOWN")))
                .sorted(Comparator.comparing(GrupoResumen::groupId))
                .toList();
    }

    public GrupoInfoResponse describirGrupo(String groupId) {
        ConsumerGroupDescription d = esperar(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        if (d == null || "DEAD".equals(String.valueOf(d.state()))) {
            throw new NoSuchElementException("El consumer group no existe: " + groupId);
        }
        List<MiembroInfo> miembros = d.members().stream()
                .map(m -> new MiembroInfo(m.consumerId(), m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream()
                                .sorted(Comparator.comparing(TopicPartition::topic)
                                        .thenComparingInt(TopicPartition::partition))
                                .map(tp -> tp.topic() + "-" + tp.partition())
                                .toList()))
                .toList();
        Node coordinador = d.coordinator();
        return new GrupoInfoResponse(groupId, String.valueOf(d.state()),
                coordinador == null ? null : coordinador.id(), miembros);
    }

    public GrupoLagResponse lagGrupo(String groupId) {
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        Map<TopicPartition, OffsetAndMetadata> offsets = confirmados.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (offsets.isEmpty()) {
            throw new NoSuchElementException(
                    "El grupo no existe o aun no ha confirmado offsets: " + groupId);
        }

        Map<TopicPartition, OffsetSpec> consulta = new HashMap<>();
        offsets.keySet().forEach(tp -> consulta.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales =
                esperar(admin.listOffsets(consulta).all());

        List<ParticionLag> filas = offsets.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<TopicPartition, OffsetAndMetadata> e) -> e.getKey().topic())
                        .thenComparingInt(e -> e.getKey().partition()))
                .map(e -> {
                    long actual = e.getValue().offset();
                    long fin = finales.get(e.getKey()).offset();
                    return new ParticionLag(e.getKey().topic(), e.getKey().partition(),
                            actual, fin, Math.max(0, fin - actual));
                })
                .toList();
        long total = filas.stream().mapToLong(ParticionLag::lag).sum();
        return new GrupoLagResponse(groupId, total, filas);
    }

    public void eliminarGrupo(String groupId) {
        esperar(admin.deleteConsumerGroups(List.of(groupId)).all());
    }

    // ------------------------------------------------------------------ utilidades

    private void validarNombre(String nombre) {
        if (nombre == null || !NOMBRE_TOPICO.matcher(nombre).matches()) {
            throw new IllegalArgumentException(
                    "Nombre de topico invalido (use letras, numeros, punto, guion o guion bajo)");
        }
        if (nombre.startsWith("__")) {
            throw new IllegalArgumentException("Los nombres que empiezan con '__' estan reservados por Kafka");
        }
    }

    private Map<String, String> validarConfigs(Map<String, String> configs) {
        Map<String, String> limpias = new LinkedHashMap<>();
        if (configs == null) {
            return limpias;
        }
        for (Map.Entry<String, String> e : configs.entrySet()) {
            if (!CONFIGS_PERMITIDAS.contains(e.getKey())) {
                throw new IllegalArgumentException("Configuracion no permitida: " + e.getKey()
                        + ". Permitidas: " + new TreeSet<>(CONFIGS_PERMITIDAS));
            }
            if (e.getValue() == null || e.getValue().isBlank()) {
                throw new IllegalArgumentException("La configuracion " + e.getKey() + " no puede estar vacia");
            }
            limpias.put(e.getKey(), e.getValue().trim());
        }
        return limpias;
    }

    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(timeoutSegundos, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("La operacion fue interrumpida");
        } catch (TimeoutException e) {
            throw new KafkaNoDisponibleException("Kafka no respondio en " + timeoutSegundos + " segundos");
        } catch (ExecutionException e) {
            throw traducir(e.getCause());
        }
    }

    private RuntimeException traducir(Throwable causa) {
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new NoSuchElementException("El topico no existe");
        }
        if (causa instanceof GroupIdNotFoundException) {
            return new NoSuchElementException("El consumer group no existe");
        }
        if (causa instanceof TopicExistsException) {
            return new KafkaConflictException("El topico ya existe");
        }
        if (causa instanceof GroupNotEmptyException) {
            return new KafkaConflictException("El consumer group tiene miembros activos; detenga sus consumidores primero");
        }
        if (causa instanceof InvalidReplicationFactorException || causa instanceof InvalidPartitionsException
                || causa instanceof InvalidConfigurationException || causa instanceof InvalidTopicException
                || causa instanceof InvalidRequestException) {
            return new IllegalArgumentException(causa.getMessage());
        }
        if (causa instanceof org.apache.kafka.common.errors.TimeoutException) {
            return new KafkaNoDisponibleException("Kafka no respondio: " + causa.getMessage());
        }
        return new IllegalStateException(causa == null ? "Error de Kafka" : causa.getMessage(), causa);
    }
}
