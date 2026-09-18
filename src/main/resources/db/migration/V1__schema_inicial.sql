-- ---------------------------------------------------------------------------
-- Schema inicial da API de destinos turisticos.
--
-- O schema e a fonte da verdade: a aplicacao sobe com
-- spring.jpa.hibernate.ddl-auto=validate, entao o Hibernate apenas confere se
-- o mapeamento das entidades bate com estas tabelas. Nenhuma alteracao de
-- estrutura e feita em tempo de execucao.
--
-- Colunas *_normalizado(a) guardam o texto sem acentos e em minusculas. Elas
-- existem por dois motivos:
--   1. permitir pesquisa e checagem de duplicidade sem depender da extensao
--      unaccent, mantendo a aplicacao portavel entre PostgreSQL gerenciado
--      (Neon) e o container local;
--   2. permitir que a regra de unicidade do negocio seja garantida pelo banco
--      (uk_destino_nome_local), e nao apenas pela camada de servico.
-- ---------------------------------------------------------------------------

create table destino (
    id                       uuid          not null,
    nome                     varchar(120)  not null,
    nome_normalizado         varchar(120)  not null,
    descricao                varchar(2000) not null,
    cidade                   varchar(120)  not null,
    estado                   varchar(120),
    pais                     varchar(120)  not null,
    cidade_normalizada       varchar(120)  not null,
    pais_normalizado         varchar(120)  not null,
    localizacao_normalizada  varchar(400)  not null,
    hoteis_disponiveis       integer       not null,
    preco_pacote             numeric(11,2) not null,
    nota_media               numeric(3,2)  not null,
    total_avaliacoes         integer       not null,
    criado_em                timestamp(6) with time zone not null,
    atualizado_em            timestamp(6) with time zone not null,

    constraint pk_destino primary key (id),
    constraint uk_destino_nome_local unique (nome_normalizado, cidade_normalizada, pais_normalizado),
    constraint ck_destino_hoteis check (hoteis_disponiveis >= 0),
    constraint ck_destino_preco  check (preco_pacote >= 0),
    constraint ck_destino_nota   check (nota_media >= 0 and nota_media <= 5),
    constraint ck_destino_total  check (total_avaliacoes >= 0)
);

comment on table destino is 'Destino turistico ofertado pela agencia';
comment on column destino.nota_media is 'Resumo derivado das avaliacoes, recalculado pela camada de servico';

-- Atividades e uma colecao de valores simples, nao uma entidade: nao tem
-- identidade propria nem ciclo de vida fora do destino.
create table destino_atividade (
    destino_id uuid        not null,
    ordem      integer     not null,
    atividade  varchar(80) not null,

    constraint pk_destino_atividade primary key (destino_id, ordem),
    constraint fk_destino_atividade_destino foreign key (destino_id)
        references destino (id) on delete cascade
);

create table avaliacao (
    id         uuid         not null,
    destino_id uuid         not null,
    autor      varchar(80)  not null,
    nota       integer      not null,
    comentario varchar(500),
    criada_em  timestamp(6) with time zone not null,

    constraint pk_avaliacao primary key (id),
    constraint fk_avaliacao_destino foreign key (destino_id)
        references destino (id) on delete cascade,
    constraint ck_avaliacao_nota check (nota between 1 and 5)
);

create index ix_avaliacao_destino on avaliacao (destino_id);

-- ---------------------------------------------------------------------------
-- Usuarios e perfis de acesso.
--
-- senha_hash guarda o hash no formato do DelegatingPasswordEncoder, com o
-- prefixo do algoritmo embutido (ex.: {argon2}$argon2id$v=19$m=19456,t=2,p=1$...).
-- Guardar o prefixo permite trocar de algoritmo no futuro sem forcar a
-- redefinicao de todas as senhas. Senha em claro nunca e persistida.
-- 255 caracteres cobrem com folga o hash Argon2id com salt de 16 bytes.
-- ---------------------------------------------------------------------------

create table usuario (
    id          uuid         not null,
    username    varchar(60)  not null,
    senha_hash  varchar(255) not null,
    ativo       boolean      not null,
    criado_em   timestamp(6) with time zone not null,

    constraint pk_usuario primary key (id),
    constraint uk_usuario_username unique (username)
);

create table usuario_perfil (
    usuario_id uuid        not null,
    perfil     varchar(20) not null,

    constraint pk_usuario_perfil primary key (usuario_id, perfil),
    constraint fk_usuario_perfil_usuario foreign key (usuario_id)
        references usuario (id) on delete cascade,
    constraint ck_usuario_perfil check (perfil in ('ADMIN', 'USER'))
);
