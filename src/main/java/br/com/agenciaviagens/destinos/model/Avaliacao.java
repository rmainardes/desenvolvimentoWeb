package br.com.agenciaviagens.destinos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Avaliacao registrada por um viajante para um destino.
 *
 * Era um record na versao em memoria; virou entidade porque records nao podem
 * ser entidades JPA (o provedor precisa de construtor sem argumentos e de
 * campos mutaveis para materializar o objeto). Os acessores mantem o estilo de
 * record de proposito, para nao quebrar quem ja consumia a classe.
 *
 * O destino e referenciado pelo identificador, e nao por um @ManyToOne. A
 * avaliacao so existe dentro do contexto de um destino e sempre e carregada a
 * partir dele, entao nao ha ganho em navegar de volta -- e o mapeamento por id
 * evita carregar o destino inteiro para gravar uma nota. A integridade fica
 * garantida pela foreign key declarada na migration.
 */
@Entity
@Table(name = "avaliacao")
public class Avaliacao implements Persistable<UUID> {

    public static final int NOTA_MINIMA = 1;
    public static final int NOTA_MAXIMA = 5;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "destino_id", nullable = false, updatable = false)
    private UUID destinoId;

    @Column(name = "autor", nullable = false, length = 80)
    private String autor;

    @Column(name = "nota", nullable = false)
    private int nota;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Transient
    private boolean novo = true;

    /** Exigido pelo JPA. Nao usar no codigo de dominio. */
    protected Avaliacao() {
    }

    private Avaliacao(UUID id, UUID destinoId, String autor, int nota, String comentario, Instant criadaEm) {
        this.id = id;
        this.destinoId = destinoId;
        this.autor = autor;
        this.nota = nota;
        this.comentario = comentario;
        this.criadaEm = criadaEm;
    }

    public static Avaliacao nova(UUID destinoId, String autor, int nota, String comentario) {
        return new Avaliacao(UUID.randomUUID(), destinoId, autor, nota, comentario, Instant.now());
    }

    @PostPersist
    @PostLoad
    void marcarComoPersistida() {
        this.novo = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    public UUID id() {
        return id;
    }

    public UUID destinoId() {
        return destinoId;
    }

    public String autor() {
        return autor;
    }

    public int nota() {
        return nota;
    }

    public String comentario() {
        return comentario;
    }

    public Instant criadaEm() {
        return criadaEm;
    }
}
