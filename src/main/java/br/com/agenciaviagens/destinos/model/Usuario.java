package br.com.agenciaviagens.destinos.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Usuario que consome a API.
 *
 * Guarda apenas o hash da senha, nunca a senha em claro. O hash vem do
 * PasswordEncoder configurado em SecurityConfig e ja chega com o prefixo do
 * algoritmo (ex.: {argon2}...), o que permite trocar de algoritmo depois sem
 * invalidar as senhas existentes.
 *
 * Os perfis sao carregados junto com o usuario (EAGER) porque o
 * UserDetailsService precisa deles imediatamente, fora de qualquer transacao
 * aberta -- e a aplicacao roda com open-in-view desligado.
 */
@Entity
@Table(name = "usuario")
public class Usuario implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "usuario_perfil",
            joinColumns = @JoinColumn(
                    name = "usuario_id",
                    foreignKey = @ForeignKey(name = "fk_usuario_perfil_usuario")))
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private Set<Perfil> perfis = EnumSet.noneOf(Perfil.class);

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Transient
    private boolean novo = true;

    /** Exigido pelo JPA. Nao usar no codigo de dominio. */
    protected Usuario() {
    }

    public Usuario(String username, String senhaHash, Collection<Perfil> perfis) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.senhaHash = senhaHash;
        this.ativo = true;
        this.perfis.addAll(perfis);
        this.criadoEm = Instant.now();
    }

    /** Troca o hash da senha. Recebe o valor ja codificado, nunca a senha em claro. */
    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
    }

    public void desativar() {
        this.ativo = false;
    }

    @PostPersist
    @PostLoad
    void marcarComoPersistido() {
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

    public String getUsername() {
        return username;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Set<Perfil> getPerfis() {
        return Collections.unmodifiableSet(perfis);
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    /** Nao inclui o hash da senha: evita vazamento acidental em log. */
    @Override
    public String toString() {
        return "Usuario[username=" + username + ", perfis=" + perfis + ", ativo=" + ativo + "]";
    }
}
