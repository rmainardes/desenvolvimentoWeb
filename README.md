# API de Destinos Turísticos — Agência de Viagens

API REST para gerenciamento de destinos de viagem, construída com **Java 17**, **Spring Boot 4.1.1**,
**PostgreSQL**, **Spring Data JPA** e **Spring Security**.

Esta é a segunda versão da solução. A primeira mantinha os dados em memória e não tinha controle de
acesso; agora os dados são persistidos em banco relacional, o schema é versionado com Flyway, os
usuários vivem no banco e cada endpoint exige o perfil adequado.

> **Início rápido:** `docker compose up -d` e depois `./mvnw spring-boot:run`.
> A API sobe em `http://localhost:8080` com três destinos de exemplo e dois usuários criados.
> Detalhes e configuração do banco online na [seção 9](#9-como-executar).

---

## 1. Visão geral do problema

A agência já possui site institucional e sistema interno de reservas, e quer expor seus dados de
destinos para aplicativos de turismo, parceiros comerciais e plataformas futuras. Uma API REST é o
ponto de integração adequado: é independente de linguagem, usa HTTP/JSON (aceito por qualquer
parceiro) e permite versionar o contrato sem quebrar quem já consome.

O domínio informado pela agência envolve destinos, localização, descrição, atividades turísticas,
disponibilidade de hotéis, pacotes e avaliações. O recurso central é o **destino**, com as
**avaliações** modeladas como sub-recurso, já que só existem vinculadas a um destino e alteram sua
nota média.

### Escopo desta versão

| Incluído | Fora do escopo (próximas etapas) |
| --- | --- |
| CRUD de destinos | Reservas e pagamentos |
| Pesquisa por nome e localização | Paginação e cache |
| Registro de avaliações com recálculo de média | Documentação OpenAPI/Swagger |
| Validação de entrada e tratamento de erros | Cadastro self-service de usuários |
| **Persistência em PostgreSQL com Spring Data JPA** | Refresh token / OAuth2 |
| **Schema versionado com Flyway** | Observabilidade e pipeline de CI |
| **Autenticação de usuários cadastrados no banco** | |
| **Autorização por perfil (ADMIN / USER)** | |
| Testes de unidade e de integração com banco real | |

---

## 2. Arquitetura

A aplicação segue uma **arquitetura em camadas** (monolito modular), organizada por
responsabilidade. Cada camada só conhece a camada imediatamente abaixo:

```
   HTTP (JSON)
        |
        v
+---------------------+   Spring Security: autenticação (Basic) e
|   filtros de        |   autorização por perfil acontecem ANTES do
|   segurança         |   controller. Respostas 401/403 padronizadas.
+---------------------+
        |
        v
+---------------------+   DTOs de entrada/saída (records + Bean Validation)
|     controller      |   Traduz HTTP <-> chamadas de negócio. Sem regra de negócio.
+---------------------+
        |
        v
+---------------------+
|      service        |   Regras de negócio: duplicidade, recálculo de média,
+---------------------+   atualização parcial, exclusão em cascata.
        |                 Cada operação pública é uma transação.
        v
+---------------------+   Porta: DestinoRepository, AvaliacaoRepository (interfaces)
|     repository      |   Adaptador: DestinoRepositoryJpa, AvaliacaoRepositoryJpa
|  (Spring Data JPA)  |   Consultas: DestinoJpaRepository, AvaliacaoJpaRepository
+---------------------+
        |
        v
+---------------------+
|   entidades JPA     |   Destino, Avaliacao, Localizacao, Usuario
+---------------------+
        |
        v
+---------------------+
|     PostgreSQL      |   schema versionado por Flyway
+---------------------+
```

### A troca de persistência não vazou para o negócio

`DestinoRepository` e `AvaliacaoRepository` já eram **interfaces** na primeira versão, com a
implementação em memória por trás. Foi a aposta feita lá, e ela se pagou aqui: para trocar
`ConcurrentHashMap` por PostgreSQL bastou escrever um adaptador novo. A camada de serviço mudou
apenas para ganhar anotações transacionais — nenhuma regra de negócio foi reescrita.

A melhor evidência disso é que **os sete testes de unidade do serviço continuam passando sem
alteração**, ainda usando as implementações em memória como dublês. Se a persistência tivesse
vazado para o negócio, eles teriam quebrado.

### Estrutura de pacotes

```
src/main/java/br/com/agenciaviagens/destinos/
├── AgenciaViagensApplication.java
├── config/      SecurityConfig, DadosIniciaisConfig
├── controller/  DestinoController, AvaliacaoController
├── dto/         DestinoRequest, DestinoPatchRequest, AvaliacaoRequest,
│                DestinoResponse, AvaliacaoResponse, ErroResponse
├── exception/   RecursoNaoEncontradoException, ConflitoDeDadosException,
│                RequisicaoInvalidaException, TratadorGlobalDeExcecoes
├── model/       Destino, Avaliacao, Localizacao, Usuario, Perfil   (entidades JPA)
├── repository/  DestinoRepository, AvaliacaoRepository             (portas)
│   └── jpa/     DestinoJpaRepository, AvaliacaoJpaRepository,      (Spring Data)
│                DestinoRepositoryJpa, AvaliacaoRepositoryJpa,      (adaptadores)
│                UsuarioRepository
├── security/    UsuarioDetailsService, TratadorDeErrosDeSeguranca
├── service/     DestinoService
└── util/        Textos

src/main/resources/
├── application.yml
└── db/migration/V1__schema_inicial.sql
```

> As classes `DestinoRepositoryEmMemoria` e `AvaliacaoRepositoryEmMemoria` continuam no pacote
> `repository`, mas **deixaram de ser beans Spring**: a anotação `@Repository` foi removida delas
> para que exista um único candidato à injeção. Hoje servem apenas como dublês nos testes de
> unidade — e como registro visível da diferença entre os dois modos de armazenamento.

---

## 3. Tecnologias e justificativas

| Tecnologia | Versão | Por quê |
| --- | --- | --- |
| Java | 17 (LTS) | Base mínima do Spring Boot 4; `record` e text blocks reduzem código repetitivo. |
| Spring Boot | 4.1.1 | Tomcat embarcado, autoconfiguração, injeção de dependências e JSON prontos. |
| Spring Web MVC | via starter | Rotas REST declarativas (`@RestController`, `@GetMapping`). |
| Bean Validation | via starter | Validação no limite da aplicação, sem `if` espalhado pelos controllers. |
| **Spring Data JPA** | via starter | Repositórios derivados de interface; menos SQL manual para CRUD, sem abrir mão de JPQL onde a consulta é específica. |
| **PostgreSQL** | 16 | Relacional maduro, com tipo `uuid` nativo, `numeric` exato para valores monetários e constraints declarativas. Disponível em free tier gerenciado (Neon). |
| **Flyway** | via starter | Schema versionado e reproduzível. O banco deixa de ser um efeito colateral do ORM e passa a ser um artefato revisável, com histórico. |
| **Spring Security** | via starter | Autenticação e autorização como filtros, antes do controller — regra de acesso não se mistura com regra de negócio. |
| **BouncyCastle** | 1.84 | Implementação do Argon2id usada pelo `Argon2PasswordEncoder`. |
| Maven | 3.9+ | Build padrão do ecossistema Java. |
| JUnit 5 + MockMvc | via starter | Testes de unidade e de integração HTTP sem subir servidor real. |
| **Testcontainers** | via BOM | PostgreSQL real e descartável no teste de integração. |

Nenhuma biblioteca fora do ecossistema Spring foi adicionada para o domínio (sem Lombok, sem
MapStruct). BouncyCastle entra por exigência técnica do Argon2id, não por conveniência.

---

## 4. Modelo de dados

### Entidades

**Destino** (`destino`)

| Campo | Tipo Java | Coluna | Observação |
| --- | --- | --- | --- |
| `id` | UUID | `id` (PK) | Atribuído pela aplicação no construtor |
| `nome` | String | `nome` | Obrigatório, até 120 caracteres |
| `descricao` | String | `descricao` | Obrigatório, até 2000 caracteres |
| `localizacao` | Localizacao | `cidade`, `estado`, `pais` | `@Embeddable` — não é tabela separada |
| `atividades` | List\<String\> | tabela `destino_atividade` | `@ElementCollection`, até 20 itens |
| `hoteisDisponiveis` | int | `hoteis_disponiveis` | Zero ou positivo |
| `precoPacote` | BigDecimal | `preco_pacote` `numeric(11,2)` | Zero ou positivo |
| `notaMedia` | BigDecimal | `nota_media` `numeric(3,2)` | **Somente leitura** — derivado das avaliações |
| `totalAvaliacoes` | int | `total_avaliacoes` | **Somente leitura** |
| `criadoEm` / `atualizadoEm` | Instant (UTC) | `timestamptz` | Controlados pela aplicação |
| — | — | `nome_normalizado`, `cidade_normalizada`, `pais_normalizado`, `localizacao_normalizada` | Internos, nunca expostos pela API |

**Avaliacao** (`avaliacao`): `id`, `destino_id` (FK), `autor`, `nota` (1 a 5), `comentario`
(opcional), `criada_em`.

**Usuario** (`usuario`): `id`, `username` (único), `senha_hash`, `ativo`, `criado_em`, e os perfis
em `usuario_perfil`.

### Decisões de mapeamento

- **`Localizacao` é `@Embeddable`, não entidade.** É um objeto de valor: não tem identidade própria
  nem existe fora de um destino. As três colunas vivem na tabela `destino`, o que evita um JOIN em
  toda consulta. Era um `record` na versão anterior e virou classe porque o JPA precisa de
  construtor sem argumentos; os acessores mantêm o estilo de record (`cidade()`, `pais()`) de
  propósito, para que nenhum consumidor precisasse mudar.

- **`Avaliacao` referencia o destino por id, não por `@ManyToOne`.** A avaliação sempre é carregada
  a partir de um destino e nunca precisa navegar de volta. Mapear por id evita carregar o destino
  inteiro para gravar uma nota. A integridade fica garantida pela foreign key declarada na
  migration, não pela ausência de relacionamento no Java.

- **Colunas normalizadas em vez da extensão `unaccent`.** A pesquisa e a regra de unicidade ignoram
  acentos e caixa. Isso poderia ser feito com `unaccent()` no SQL, mas exigiria uma extensão
  instalada no servidor — nem sempre disponível em banco gerenciado. Guardar o texto já normalizado
  resolve com portabilidade total e permite que a unicidade seja uma **constraint de banco**
  (`uk_destino_nome_local`), não apenas uma verificação na aplicação.

- **`BigDecimal` para preço e nota.** `double` acumula erro de arredondamento; em valor monetário
  e média isso vira divergência visível. No banco, `numeric` exato pelo mesmo motivo.

- **O schema pertence ao Flyway, não ao Hibernate.** A aplicação sobe com
  `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate apenas confere, na inicialização, se o
  mapeamento bate com as tabelas. Um erro de mapeamento aparece como falha de subida, com o nome da
  coluna divergente no log, em vez de virar um erro de SQL em produção. `update` nunca é usado — ele
  altera o banco sem deixar registro do que mudou.

---

## 5. Segurança

### Perfis de acesso

| Perfil | Quem é | O que pode |
| --- | --- | --- |
| `USER` | Viajante autenticado | Consultar o catálogo e registrar avaliações |
| `ADMIN` | Operação da agência | Tudo que o `USER` faz, mais cadastrar, alterar e excluir destinos |

### Matriz de autorização

| Método e rota | Acesso exigido | Por quê |
| --- | --- | --- |
| `GET /api/v1/destinos` | **Público** | O catálogo é a vitrine da agência; parceiros e aplicativos consultam sem credencial |
| `GET /api/v1/destinos/{id}` | **Público** | idem |
| `GET /api/v1/destinos/{id}/avaliacoes` | **Público** | Avaliação pública faz parte da vitrine |
| `POST /api/v1/destinos/{id}/avaliacoes` | Autenticado (`USER` ou `ADMIN`) | Nota anônima é vetor de manipulação da média |
| `POST /api/v1/destinos` | `ADMIN` | Manutenção do catálogo é operação interna |
| `PUT /api/v1/destinos/{id}` | `ADMIN` | idem |
| `PATCH /api/v1/destinos/{id}` | `ADMIN` | idem |
| `DELETE /api/v1/destinos/{id}` | `ADMIN` | Operação destrutiva e irreversível |

As regras ficam em `SecurityConfig`, declaradas por método HTTP e rota. A regra específica de
avaliações vem **antes** da regra genérica de escrita — em Spring Security a primeira que casa é a
que vale.

### Armazenamento de senhas: Argon2id

As senhas são gravadas com **Argon2id**, configurado com os parâmetros mínimos recomendados pela
[OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html):
**19 MiB de memória, 2 iterações, paralelismo 1**, salt de 16 bytes e hash de 32 bytes.

```java
new Argon2PasswordEncoder(16, 32, 1, 19456, 2)
```

Por que não bcrypt: a recomendação atual da OWASP é usar bcrypt apenas em sistemas legados onde
Argon2 e scrypt não estão disponíveis. Argon2id é resistente a ataque com hardware dedicado (GPU e
ASIC) justamente por exigir memória, não só CPU.

Por que os parâmetros explícitos e não `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`:
aquele preset usa 16 MiB, abaixo do mínimo recomendado.

O encoder é registrado dentro de um `DelegatingPasswordEncoder`, então o hash gravado carrega o
prefixo do algoritmo:

```
{argon2}$argon2id$v=19$m=19456,t=2,p=1$c29tZSBzYWx0...
```

Isso permite trocar de algoritmo no futuro sem invalidar as senhas existentes — hashes antigos
continuam sendo verificados pelo encoder correspondente, e só os novos usam o algoritmo novo.

**Custo a considerar:** cada verificação aloca 19 MiB. Com muitos logins simultâneos, isso
dimensiona junto com o pool de threads. Para dois usuários de teste é irrelevante; em produção é um
número a acompanhar.

### Outras práticas aplicadas

- **Senha em claro nunca é persistida nem registrada em log.** O `toString()` de `Usuario` omite o
  hash de propósito.
- **Nenhuma credencial no código ou no `application.yml`.** Tudo vem de variável de ambiente, com
  padrões que só servem para desenvolvimento — e a aplicação avisa no log enquanto estiverem em uso.
- **Falha de login não revela se o usuário existe.** A mensagem é sempre a mesma; mensagens
  distintas transformariam o login em um oráculo de enumeração de contas.
- **401 e 403 respondem no mesmo formato JSON do resto da API.** Erros de segurança acontecem na
  cadeia de filtros e não passam pelo `@RestControllerAdvice`; sem o `TratadorDeErrosDeSeguranca` o
  cliente receberia corpo vazio nesses dois casos.
- **Sessão desabilitada (`STATELESS`) e CSRF desligado.** A API não usa cookie de sessão; a
  credencial vai em cada requisição. Sem cookie automático não há o que um ataque CSRF sequestre.
- **HTTPS é pré-requisito fora da máquina do desenvolvedor.** Basic transmite a credencial em
  base64, que é codificação, não criptografia.

---

## 6. Endpoints

Base: `http://localhost:8080/api/v1/destinos`

| Operação | Método | Rota | Acesso | Sucesso | Erros |
| --- | --- | --- | --- | --- | --- |
| Listar todos | GET | `/api/v1/destinos` | público | 200 | — |
| Pesquisar | GET | `/api/v1/destinos?nome=&localizacao=` | público | 200 | 400 |
| Detalhar destino | GET | `/api/v1/destinos/{id}` | público | 200 | 400, 404 |
| Cadastrar destino | POST | `/api/v1/destinos` | **ADMIN** | 201 + `Location` | 400, 401, 403, 409 |
| Atualizar por completo | PUT | `/api/v1/destinos/{id}` | **ADMIN** | 200 | 400, 401, 403, 404, 409 |
| Atualizar parcialmente | PATCH | `/api/v1/destinos/{id}` | **ADMIN** | 200 | 400, 401, 403, 404, 409 |
| Excluir destino | DELETE | `/api/v1/destinos/{id}` | **ADMIN** | 204 | 400, 401, 403, 404 |
| Registrar avaliação | POST | `/api/v1/destinos/{id}/avaliacoes` | **autenticado** | 201 + `Location` | 400, 401, 404 |
| Listar avaliações | GET | `/api/v1/destinos/{id}/avaliacoes` | público | 200 | 400, 404 |
| Detalhar avaliação | GET | `/api/v1/destinos/{id}/avaliacoes/{avaliacaoId}` | público | 200 | 400, 404 |

### Escolha dos métodos HTTP

- **POST** cria recurso e devolve 201 com `Location` apontando para o recurso criado.
- **GET** é seguro e não altera estado; a pesquisa é feita por filtros na própria coleção
  (`?nome=&localizacao=`) em vez de uma rota `/buscar`, porque em REST a coleção é o recurso e os
  parâmetros de consulta a filtram. Os dois filtros são opcionais e combináveis (E lógico).
- **PUT** substitui o recurso inteiro; todos os campos são obrigatórios.
- **PATCH** aplica apenas os campos enviados.
- **DELETE** devolve 204 sem corpo.
- Avaliação é **POST em um sub-recurso**: registrar avaliação cria um novo registro; a alteração da
  média é consequência, não a operação em si.

### Exemplos

**Cadastro (exige ADMIN)**

```http
POST /api/v1/destinos
Authorization: Basic YWRtaW46c3VhLXNlbmhh
Content-Type: application/json

{
  "nome": "Jericoacoara",
  "descricao": "Vila de pescadores com dunas, lagoas e pôr do sol na Duna do Pôr do Sol.",
  "localizacao": { "cidade": "Jijoca de Jericoacoara", "estado": "CE", "pais": "Brasil" },
  "atividades": ["Kitesurf", "Passeio de buggy"],
  "hoteisDisponiveis": 60,
  "precoPacote": 3200.00
}
```

```http
HTTP/1.1 201 Created
Location: /api/v1/destinos/9f1c...c4

{
  "id": "9f1c...c4",
  "nome": "Jericoacoara",
  "localizacao": { "cidade": "Jijoca de Jericoacoara", "estado": "CE", "pais": "Brasil" },
  "atividades": ["Kitesurf", "Passeio de buggy"],
  "hoteisDisponiveis": 60,
  "precoPacote": 3200.00,
  "notaMedia": 0.00,
  "totalAvaliacoes": 0,
  "criadoEm": "2026-09-17T12:00:00Z",
  "atualizadoEm": "2026-09-17T12:00:00Z"
}
```

**Sem credencial**

```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Basic realm="destinos-api", charset="UTF-8"

{
  "timestamp": "2026-09-17T12:01:00Z",
  "status": 401,
  "erro": "Unauthorized",
  "mensagem": "Autenticacao obrigatoria para acessar este recurso",
  "caminho": "/api/v1/destinos",
  "camposInvalidos": []
}
```

**Autenticado, mas sem perfil suficiente**

```http
HTTP/1.1 403 Forbidden

{
  "timestamp": "2026-09-17T12:02:00Z",
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Seu perfil de acesso nao permite esta operacao",
  "caminho": "/api/v1/destinos",
  "camposInvalidos": []
}
```

**Erro de validação**

```http
HTTP/1.1 400 Bad Request

{
  "timestamp": "2026-09-17T12:06:00Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Existem campos invalidos na requisicao",
  "caminho": "/api/v1/destinos",
  "camposInvalidos": [
    { "campo": "nome", "mensagem": "O nome do destino e obrigatorio" },
    { "campo": "precoPacote", "mensagem": "O preco do pacote e obrigatorio" }
  ]
}
```

---

## 7. Regras de negócio

1. **Recálculo da média.** A cada avaliação registrada, o serviço soma todas as notas do destino e
   divide pelo total, com 2 casas decimais e arredondamento `HALF_UP`. A média é recalculada a
   partir da lista completa (e não incrementada) para que o valor continue correto caso avaliações
   passem a ser removidas ou editadas no futuro.
2. **Concorrência.** O registro de avaliação lê o destino com **lock pessimista**
   (`SELECT ... FOR UPDATE`) antes de recalcular. Duas requisições simultâneas para o mesmo destino
   são serializadas pelo banco. Na versão anterior isso era uma trava `synchronized`, que só
   funcionava dentro de um processo — com banco, esse controle pertence ao banco.
3. **Unicidade.** Não é permitido cadastrar dois destinos com o mesmo nome na mesma cidade e país
   (comparação sem acentos e sem diferenciar maiúsculas) — retorna **409 Conflict**. A regra é
   verificada na camada de serviço *e* garantida pela constraint `uk_destino_nome_local`. As duas
   coisas: a verificação dá a mensagem boa, a constraint fecha a janela entre ler e gravar.
4. **Pesquisa tolerante.** "sao paulo", "São Paulo" e "SAO PAULO" encontram o mesmo destino.
5. **Exclusão em cascata.** Excluir um destino remove suas avaliações. Explicitamente na camada de
   serviço, e com `ON DELETE CASCADE` na FK como garantia.
6. **PATCH vazio é rejeitado** com 400, por não descrever nenhuma intenção de alteração.
7. **Cada operação é uma transação.** Um erro no meio do caminho desfaz tudo — "avaliação gravada
   mas média não atualizada" deixou de ser um estado possível.

---

## 8. Tratamento de erros

Um `@RestControllerAdvice` centraliza a tradução de exceções em respostas HTTP, e o
`TratadorDeErrosDeSeguranca` cobre os dois casos que nascem antes dele. Todas as respostas seguem o
mesmo formato (`ErroResponse`):

| Situação | Status | Tratado por |
| --- | --- | --- |
| Payload com campos inválidos | 400 | `@RestControllerAdvice` |
| JSON malformado ou ausente | 400 | `@RestControllerAdvice` |
| UUID inválido na rota | 400 | `@RestControllerAdvice` |
| PATCH sem nenhum campo | 400 | `@RestControllerAdvice` |
| Sem credencial ou credencial inválida | 401 | `TratadorDeErrosDeSeguranca` |
| Perfil insuficiente | 403 | `TratadorDeErrosDeSeguranca` |
| Destino ou avaliação inexistente | 404 | `@RestControllerAdvice` |
| Nome duplicado na mesma cidade/país | 409 | `@RestControllerAdvice` |
| Violação de constraint no banco | 409 | `@RestControllerAdvice` |
| Falha não prevista | 500 | `@RestControllerAdvice` |

Nenhuma resposta expõe stack trace, nome de classe ou SQL: o erro completo vai para o log e o
cliente recebe uma mensagem genérica.

---

## 9. Como executar

### Pré-requisitos

| Requisito | Versão | Verificar com |
| --- | --- | --- |
| JDK | 17 ou superior | `java -version` |
| Maven | usa o wrapper do projeto | `./mvnw -v` |
| Docker | qualquer versão recente | `docker -v` |

Docker é necessário para o banco local e para o teste de integração. Se você for usar apenas o
banco online (Neon), ele é dispensável para rodar a aplicação — mas continua necessário para
`./mvnw verify`.

### Opção A — PostgreSQL local com Docker (recomendado para desenvolver)

```bash
docker compose up -d          # sobe o PostgreSQL 16 em localhost:5432
./mvnw spring-boot:run        # a aplicação sobe em http://localhost:8080
```

Os valores padrão do `application.yml` já apontam para esse container, então não é preciso
configurar nada. Na primeira execução o Flyway cria o schema e a aplicação insere os usuários e os
destinos de exemplo.

Para recomeçar do zero: `docker compose down -v && docker compose up -d`.

### Opção B — PostgreSQL online (Neon, free tier)

1. Crie uma conta em [neon.tech](https://neon.tech) e um projeto (a região `sa-east-1` é a mais
   próxima do Brasil).
2. Crie um banco chamado `destinos`.
3. Em **Connection Details**, copie host, usuário e senha.
4. Monte a URL no formato JDBC — o `sslmode=require` não é opcional, o Neon só aceita TLS:

```bash
cp .env.example .env
```

```dotenv
DB_URL=jdbc:postgresql://ep-seu-projeto-123456.sa-east-1.aws.neon.tech/destinos?sslmode=require
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha
APP_ADMIN_SENHA=uma-senha-forte
APP_USER_SENHA=outra-senha-forte
```

```bash
set -a && . ./.env && set +a
./mvnw spring-boot:run
```

O Flyway cria o schema na primeira conexão. **O arquivo `.env` está no `.gitignore` e não deve ser
versionado.**

> O Neon hiberna o compute depois de alguns minutos sem uso. A primeira requisição seguinte leva
> alguns segundos enquanto ele acorda — é o motivo do `connection-timeout` folgado no
> `application.yml`, não um defeito.

### Usuários criados na primeira inicialização

| Usuário | Senha | Perfis |
| --- | --- | --- |
| `admin` | valor de `APP_ADMIN_SENHA` (padrão: `trocar-esta-senha`) | `ADMIN`, `USER` |
| `viajante` | valor de `APP_USER_SENHA` (padrão: `trocar-esta-senha`) | `USER` |

A carga é idempotente: se o usuário já existe, nada é sobrescrito. Para trocar a senha de um usuário
existente, apague-o do banco e reinicie a aplicação.

Enquanto a senha padrão estiver em uso, a aplicação registra um `WARN` na subida.

### Empacotamento

```bash
./mvnw clean package
java -jar target/destinos-api-2.0.0.jar
```

### Subir com a base vazia

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.dados-iniciais=false
```

Isso desativa apenas os destinos de exemplo. Os usuários de acesso são sempre criados — sem eles a
API subiria sem ninguém capaz de administrá-la.

### Testando rapidamente

```bash
# consulta — pública, sem credencial
curl http://localhost:8080/api/v1/destinos
curl "http://localhost:8080/api/v1/destinos?nome=gramado"

# cadastrar — exige ADMIN
curl -X POST http://localhost:8080/api/v1/destinos \
  -u admin:trocar-esta-senha \
  -H "Content-Type: application/json" \
  -d '{"nome":"Jericoacoara","descricao":"Vila de pescadores com dunas e lagoas.","localizacao":{"cidade":"Jijoca de Jericoacoara","estado":"CE","pais":"Brasil"},"atividades":["Kitesurf"],"hoteisDisponiveis":60,"precoPacote":3200.00}'

# avaliar — basta estar autenticado (substitua {id})
curl -X POST http://localhost:8080/api/v1/destinos/{id}/avaliacoes \
  -u viajante:trocar-esta-senha \
  -H "Content-Type: application/json" \
  -d '{"autor":"Ana","nota":5,"comentario":"Inesquecivel"}'

# 401 — sem credencial
curl -i -X DELETE http://localhost:8080/api/v1/destinos/{id}

# 403 — autenticado, mas sem perfil
curl -i -X DELETE http://localhost:8080/api/v1/destinos/{id} -u viajante:trocar-esta-senha
```

O arquivo [`docs/api.http`](docs/api.http) traz todas as chamadas prontas, incluindo os cenários de
401 e 403, para execução no IntelliJ IDEA ou na extensão REST Client do VS Code.

---

## 10. Testes automatizados

```bash
./mvnw test      # unidade — segundos, não precisa de Docker nem de banco
./mvnw verify    # tudo, incluindo o teste de integração com PostgreSQL real
```

| Suíte | O que cobre |
| --- | --- |
| `DestinoServiceTest` | Unidade, sem Spring e sem banco: cadastro, recálculo da média, duplicidade, pesquisa sem acento, PATCH parcial e exclusão em cascata. Usa as implementações em memória como dublês. |
| `DestinoControllerIT` | Integração via MockMvc contra um PostgreSQL em container: fluxo completo, persistência efetiva, duplicidade (409), validação (400) e os casos de segurança — consulta pública, 401 sem credencial, 401 com senha errada e 403 com perfil insuficiente. |

### Um bug silencioso corrigido nesta entrega

`DestinoControllerIT` **existia desde a primeira versão mas nunca era executado**. O Surefire só
reconhece classes com nome `Test*`, `*Test`, `*Tests` ou `*TestCase`, e o sufixo `IT` não casa com
nenhum desses padrões. O relatório de testes mostrava apenas os 7 testes do serviço, e "não falhar"
era indistinguível de "não rodar".

A correção foi configurar o `maven-failsafe-plugin`, que é o plugin correto para testes de
integração: roda na fase `verify` e reconhece o sufixo `IT`. Como efeito colateral positivo, a
separação ficou explícita — `test` é rápido e não exige Docker, `verify` é completo.

O teste de integração usa **Testcontainers**, não banco em memória: as migrations usam tipos e
sintaxe específicos do PostgreSQL, e o mapeamento é validado contra elas na subida. Testar em outro
dialeto daria confiança falsa.

---

## 11. O que mudou da primeira para a segunda versão

| Área | Antes | Agora |
| --- | --- | --- |
| Armazenamento | `ConcurrentHashMap`, perdido no restart | PostgreSQL, com schema versionado por Flyway |
| Identidade dos dados | Só na memória do processo | Chave primária, FK e constraint de unicidade no banco |
| Concorrência | `synchronized` por destino, válido em um processo | Lock pessimista de linha, válido entre instâncias |
| Atomicidade | Nenhuma | Transação por operação de negócio |
| Carga inicial | Recriava tudo a cada boot | Idempotente — recriar quebraria na segunda subida |
| Acesso | Tudo aberto | Basic + perfis `ADMIN` / `USER`, com regra por método e rota |
| Usuários | Não existiam | Tabela `usuario`, senha em Argon2id |
| Erros 401/403 | Não existiam | Mesmo formato JSON do resto da API |
| Teste de integração | Escrito, mas nunca executado | Executado em `verify`, contra PostgreSQL real |
| Camada de serviço | — | **Inalterada nas regras de negócio** |

---

## 12. Decisões técnicas e limitações conhecidas

| Decisão | Motivo | Consequência |
| --- | --- | --- |
| HTTP Basic em vez de JWT | A API é consumida por sistemas, não por navegador; Basic resolve sem infraestrutura de token | Credencial vai em toda requisição; exige HTTPS obrigatoriamente |
| Argon2id em vez de bcrypt | Recomendação atual da OWASP | Dependência do BouncyCastle e 19 MiB por verificação |
| `ddl-auto=validate` + Flyway | Schema é artefato revisável, com histórico | Mudança de entidade exige migration nova — de propósito |
| Colunas normalizadas | Portabilidade e unicidade no banco | Quatro colunas redundantes por destino |
| `autor` da avaliação vem do corpo, não do usuário autenticado | Mantém o contrato da primeira versão | Um usuário pode avaliar em nome de outro. Correção natural: usar `Authentication.getName()` |
| Porta só para destinos e avaliações | Usuário é infraestrutura de autenticação, sem regra a proteger | Assimetria entre os pacotes, documentada |
| Sem paginação | Volume pequeno nesta fase | `GET /destinos` cresce indefinidamente |
| `LIKE '%termo%'` na pesquisa | Simplicidade | Não usa índice; com volume alto pediria `pg_trgm` |
| PATCH ignora campos nulos | Simplicidade | Não distingue "campo ausente" de "definir como nulo" |

**Sobre dados pessoais (LGPD):** a avaliação armazena o nome do autor e a tabela `usuario` armazena
credenciais — os dois são dados pessoais. O que já está aplicado: senha só como hash forte, nenhum
dado identificável em log de erro, mensagem de falha de login que não confirma existência de conta,
e nenhuma resposta ecoando o conteúdo da requisição. O que falta antes de um ambiente real:
minimização (permitir apelido em vez do nome), base legal definida, política de retenção e rotina de
exclusão a pedido do titular.

### Evolução futura

1. Documentação interativa com OpenAPI/Swagger UI.
2. `autor` da avaliação derivado do usuário autenticado, e uma avaliação por usuário por destino.
3. JWT com refresh token, se a API passar a ser consumida por front-end próprio.
4. Paginação e ordenação em `GET /destinos`.
5. Índice `pg_trgm` para a pesquisa textual, quando o volume justificar.
6. Novos agregados: pacotes de viagem, hotéis e reservas.
7. Actuator, métricas e pipeline de CI rodando `./mvnw verify`.

---

## 13. Onde cada requisito foi atendido

| Requisito do desafio | Implementação |
| --- | --- |
| Integração com PostgreSQL | `spring-boot-starter-data-jpa` + driver, `spring.datasource.*` em `application.yml` |
| Configuração no arquivo de propriedades | `application.yml`, com credenciais por variável de ambiente |
| Entidades JPA | `Destino`, `Avaliacao`, `Usuario` (`@Entity`), `Localizacao` (`@Embeddable`) |
| Repositories com Spring Data JPA | `DestinoJpaRepository`, `AvaliacaoJpaRepository`, `UsuarioRepository` |
| Camada de serviço com persistência | `DestinoService`, transacional, conversando com as portas |
| Controller sem acesso direto ao banco | Controllers só chamam `DestinoService` |
| Persistir, consultar, atualizar e excluir | CRUD completo — ver [seção 6](#6-endpoints) |
| Autenticação de usuários do banco | `UsuarioDetailsService` sobre a tabela `usuario` |
| Cadastro de usuários no banco | `DadosIniciaisConfig.criarUsuariosIniciais`, idempotente |
| Perfis ADMIN e USER | enum `Perfil`, tabela `usuario_perfil` |
| Autorização por perfil | `SecurityConfig.filterChain` — matriz na [seção 5](#5-segurança) |
| Proteção de operações sensíveis | POST, PUT, PATCH e DELETE restritos a `ADMIN` |
| Recursos de consulta acessíveis | Todos os `GET` são públicos |
| Armazenamento seguro de credenciais | Argon2id (19 MiB, t=2, p=1) via `DelegatingPasswordEncoder` |
| Organização em camadas mantida | `controller`, `service`, `repository`, `model`, `security` |
| Documentação de execução e configuração | [Seção 9](#9-como-executar) |
| Usuários e perfis de teste | [Seção 9](#usuários-criados-na-primeira-inicialização) |
| Endpoints e regras de acesso | [Seções 5 e 6](#5-segurança) |
| Repositório Git com o código-fonte | Este repositório |
