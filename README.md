# API de Destinos Turísticos — Agência de Viagens

API REST para o gerenciamento de destinos de viagem, construída com **Java 17** e **Spring Boot 4.1.1**.
Esta é a primeira versão funcional da solução: armazenamento em memória, sem banco de dados e sem
mecanismos de autenticação, com foco em arquitetura, organização do projeto e nos endpoints principais.

---

## 1. Visão geral do problema

A agência já possui site institucional e sistema interno de reservas, e quer expor seus dados de
destinos para aplicativos de turismo, parceiros comerciais e plataformas futuras. Uma API REST é o
ponto de integração adequado: é independente de linguagem, usa HTTP/JSON (aceito por qualquer
parceiro) e permite versionar o contrato sem quebrar quem já consome.

O domínio informado pela agência envolve destinos, localização, descrição, atividades turísticas,
disponibilidade de hotéis, pacotes e avaliações. Nesta etapa o recurso central é o **destino**, com
as **avaliações** modeladas como sub-recurso, já que só existem vinculadas a um destino e alteram
sua nota média.

### Escopo desta versão

| Incluído | Fora do escopo (próximas etapas) |
| --- | --- |
| CRUD de destinos | Persistência em banco de dados |
| Pesquisa por nome e localização | Autenticação/autorização |
| Registro de avaliações com recálculo de média | Reservas e pagamentos |
| Validação de entrada e tratamento de erros | Paginação e cache |
| Testes automatizados | Deploy/containerização |

---

## 2. Arquitetura proposta

A aplicação segue uma **arquitetura em camadas** (monolito modular), organizada por
responsabilidade. Cada camada só conhece a camada imediatamente abaixo:

```
   HTTP (JSON)
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
        |
        v
+---------------------+
|     repository      |   Contrato de persistência (interface)
|  (impl. em memória) |   + implementação com ConcurrentHashMap.
+---------------------+
        |
        v
+---------------------+
|    model/entity     |   Destino, Avaliacao, Localizacao — o domínio puro,
+---------------------+   sem anotações de framework.

   exception/  -> tratamento global de erros (@RestControllerAdvice)
   dto/        -> contratos de entrada e saída da API
   config/     -> carga de dados de exemplo
   util/       -> normalização de texto usada na pesquisa
```

### Por que essa arquitetura

- **Porte adequado.** O sistema tem um agregado principal (destino). Uma arquitetura hexagonal
  completa ou microsserviços adicionariam complexidade sem benefício nesta fase.
- **Manutenção.** Regras de negócio ficam em um único lugar (`service`), então mudanças de regra não
  se espalham por controllers.
- **Evolução.** `DestinoRepository` é uma interface. Trocar a implementação em memória por Spring
  Data JPA exige criar uma nova implementação — a camada de serviço não muda.
- **Isolamento do contrato.** Entidades de domínio nunca são serializadas diretamente; DTOs separam
  o que a API expõe do que o domínio guarda, evitando que uma mudança interna quebre integrações.
- **Testabilidade.** O serviço não depende de HTTP nem de Spring, então é testado com JUnit puro.

### Estrutura de pacotes

```
src/main/java/br/com/agenciaviagens/destinos/
├── AgenciaViagensApplication.java
├── config/      DadosIniciaisConfig
├── controller/  DestinoController, AvaliacaoController
├── dto/         DestinoRequest, DestinoPatchRequest, AvaliacaoRequest,
│                DestinoResponse, AvaliacaoResponse, ErroResponse
├── exception/   RecursoNaoEncontradoException, ConflitoDeDadosException,
│                RequisicaoInvalidaException, TratadorGlobalDeExcecoes
├── model/       Destino, Avaliacao, Localizacao
├── repository/  DestinoRepository, AvaliacaoRepository (+ impl. em memória)
├── service/     DestinoService
└── util/        Textos
```

---

## 3. Tecnologias e justificativas

| Tecnologia | Versão | Por quê |
| --- | --- | --- |
| Java | 17 (LTS) | Base mínima do Spring Boot 4; `record` e text blocks reduzem código repetitivo em DTOs. Linguagem madura, tipada e amplamente adotada no mercado. |
| Spring Boot | 4.1.1 | Servidor Tomcat embarcado, autoconfiguração, injeção de dependências e serialização JSON prontos. Sem ele, seria necessário configurar servlet container, mapeamento de rotas e conversores manualmente. |
| Spring Web MVC | via starter | Modelo de anotações (`@RestController`, `@GetMapping`) que expressa rotas REST de forma declarativa e legível. |
| Bean Validation | via starter | Validação declarativa (`@NotBlank`, `@Min`) no limite da aplicação, sem `if` espalhado pelos controllers. |
| Maven | 3.9+ | Gerenciamento de dependências e ciclo de build padrão do ecossistema Java. |
| JUnit 5 + MockMvc | via starter de teste | Testes de unidade do serviço e testes de integração HTTP sem subir servidor real. |

Nenhuma biblioteca externa ao ecossistema Spring foi adicionada (sem Lombok, sem MapStruct): o
projeto compila apenas com os starters oficiais, o que reduz a superfície de manutenção.

---

## 4. Modelo de dados

**Destino**

| Campo | Tipo | Observação |
| --- | --- | --- |
| `id` | UUID | Gerado pela aplicação |
| `nome` | String | Obrigatório, até 120 caracteres |
| `descricao` | String | Obrigatório, até 2000 caracteres |
| `localizacao` | Objeto | `cidade` e `pais` obrigatórios, `estado` opcional |
| `atividades` | Lista de String | Até 20 itens |
| `hoteisDisponiveis` | Inteiro | Zero ou positivo |
| `precoPacote` | BigDecimal | Zero ou positivo, 2 casas decimais |
| `notaMedia` | BigDecimal | **Somente leitura** — derivado das avaliações |
| `totalAvaliacoes` | Inteiro | **Somente leitura** |
| `criadoEm` / `atualizadoEm` | Instant (UTC) | Controlados pela aplicação |

**Avaliacao**: `id`, `destinoId`, `autor`, `nota` (1 a 5), `comentario` (opcional), `criadaEm`.

`BigDecimal` é usado em preço e nota média para evitar os erros de arredondamento de `double`, que
em valores monetários e médias acumulam distorção.

---

## 5. Endpoints

Base: `http://localhost:8080/api/v1/destinos`

| Operação | Método | Rota | Sucesso | Erros |
| --- | --- | --- | --- | --- |
| Cadastrar destino | POST | `/api/v1/destinos` | 201 + header `Location` | 400, 409 |
| Listar todos | GET | `/api/v1/destinos` | 200 | — |
| Pesquisar por nome/localização | GET | `/api/v1/destinos?nome=&localizacao=` | 200 | 400 |
| Detalhar destino | GET | `/api/v1/destinos/{id}` | 200 | 400, 404 |
| Atualizar por completo | PUT | `/api/v1/destinos/{id}` | 200 | 400, 404, 409 |
| Atualizar parcialmente | PATCH | `/api/v1/destinos/{id}` | 200 | 400, 404, 409 |
| Excluir destino | DELETE | `/api/v1/destinos/{id}` | 204 | 400, 404 |
| Registrar avaliação | POST | `/api/v1/destinos/{id}/avaliacoes` | 201 + `Location` | 400, 404 |
| Listar avaliações | GET | `/api/v1/destinos/{id}/avaliacoes` | 200 | 400, 404 |
| Detalhar avaliação | GET | `/api/v1/destinos/{id}/avaliacoes/{avaliacaoId}` | 200 | 400, 404 |

### Escolha dos métodos HTTP

- **POST** cria recurso e devolve 201 com `Location` apontando para o recurso criado.
- **GET** é seguro e não altera estado; a pesquisa é feita por filtros na própria coleção
  (`?nome=&localizacao=`) em vez de uma rota `/buscar`, porque em REST a coleção é o recurso e os
  parâmetros de consulta a filtram. Os dois filtros são opcionais e combináveis (E lógico).
- **PUT** substitui o recurso inteiro; todos os campos são obrigatórios.
- **PATCH** aplica apenas os campos enviados — o método correto quando só a disponibilidade de
  hotéis muda, por exemplo.
- **DELETE** devolve 204 sem corpo.
- Avaliação é **POST em um sub-recurso**, não PUT no destino: registrar avaliação cria um novo
  registro; a alteração da média é consequência, não a operação em si.

### Exemplos

**Cadastro**

```http
POST /api/v1/destinos
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
  "criadoEm": "2026-08-30T12:00:00Z",
  "atualizadoEm": "2026-08-30T12:00:00Z"
}
```

**Avaliação**

```http
POST /api/v1/destinos/9f1c...c4/avaliacoes
Content-Type: application/json

{ "autor": "Ana", "nota": 5, "comentario": "Inesquecível" }
```

```http
HTTP/1.1 201 Created

{
  "id": "3b7e...9a",
  "destinoId": "9f1c...c4",
  "autor": "Ana",
  "nota": 5,
  "comentario": "Inesquecível",
  "criadaEm": "2026-08-30T12:05:00Z",
  "resumoDestino": { "notaMedia": 5.00, "totalAvaliacoes": 1 }
}
```

**Erro de validação**

```http
HTTP/1.1 400 Bad Request

{
  "timestamp": "2026-08-30T12:06:00Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Existem campos inválidos na requisição",
  "caminho": "/api/v1/destinos",
  "camposInvalidos": [
    { "campo": "nome", "mensagem": "O nome do destino é obrigatório" },
    { "campo": "precoPacote", "mensagem": "O preço do pacote é obrigatório" }
  ]
}
```

---

## 6. Regras de negócio

1. **Recálculo da média.** A cada avaliação registrada, o serviço soma todas as notas do destino e
   divide pelo total, com 2 casas decimais e arredondamento `HALF_UP`. A média é recalculada a
   partir da lista completa (e não incrementada) para que o valor continue correto caso avaliações
   passem a ser removidas ou editadas no futuro.
2. **Concorrência.** O registro de avaliação usa uma trava por destino, de modo que duas requisições
   simultâneas para o mesmo destino não calculem a média sobre um estado intermediário. Com banco de
   dados, esse papel passaria para a transação.
3. **Unicidade.** Não é permitido cadastrar dois destinos com o mesmo nome na mesma cidade e país
   (comparação sem acentos e sem diferenciar maiúsculas) — retorna **409 Conflict**.
4. **Pesquisa tolerante.** "sao paulo", "São Paulo" e "SAO PAULO" encontram o mesmo destino: o texto
   é normalizado (NFD, remoção de acentos, minúsculas) antes da comparação.
5. **Exclusão em cascata.** Excluir um destino remove também suas avaliações, evitando registros órfãos.
6. **PATCH vazio é rejeitado** com 400, por não descrever nenhuma intenção de alteração.

---

## 7. Tratamento de erros

Um `@RestControllerAdvice` centraliza a tradução de exceções em respostas HTTP. Todas seguem o mesmo
formato (`ErroResponse`):

| Situação | Status |
| --- | --- |
| Payload com campos inválidos | 400 |
| JSON malformado ou ausente | 400 |
| UUID inválido na rota | 400 |
| PATCH sem nenhum campo | 400 |
| Destino ou avaliação inexistente | 404 |
| Nome duplicado na mesma cidade/país | 409 |
| Falha não prevista | 500 |

Nenhuma resposta expõe stack trace, nome de classe ou detalhe interno: o erro completo vai para o
log da aplicação e o cliente recebe uma mensagem genérica. Essa separação evita vazamento de
informação sobre a implementação.

---

## 8. Como executar

**Pré-requisitos:** JDK 17 ou superior e Maven 3.9+ (`java -version`, `mvn -v`).

```bash
git clone <url-do-repositorio>
cd destinos-api

mvn spring-boot:run          # sobe em http://localhost:8080
```

Empacotamento e execução do jar:

```bash
mvn clean package
java -jar target/destinos-api-1.0.0.jar
```

A aplicação sobe com três destinos de exemplo já cadastrados. Para começar com a base vazia:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--app.dados-iniciais=false
```

### Testando rapidamente

```bash
# listar
curl http://localhost:8080/api/v1/destinos

# pesquisar
curl "http://localhost:8080/api/v1/destinos?nome=gramado"
curl "http://localhost:8080/api/v1/destinos?localizacao=argentina"

# cadastrar
curl -X POST http://localhost:8080/api/v1/destinos \
  -H "Content-Type: application/json" \
  -d '{"nome":"Jericoacoara","descricao":"Vila de pescadores com dunas e lagoas.","localizacao":{"cidade":"Jijoca de Jericoacoara","estado":"CE","pais":"Brasil"},"atividades":["Kitesurf"],"hoteisDisponiveis":60,"precoPacote":3200.00}'

# avaliar (substitua {id} pelo id retornado acima)
curl -X POST http://localhost:8080/api/v1/destinos/{id}/avaliacoes \
  -H "Content-Type: application/json" \
  -d '{"autor":"Ana","nota":5,"comentario":"Inesquecível"}'
```

O arquivo [`docs/api.http`](docs/api.http) contém todas as chamadas prontas para execução no
IntelliJ IDEA ou na extensão REST Client do VS Code.

### Testes automatizados

```bash
mvn test
```

- `DestinoServiceTest` — unidade, sem Spring: cadastro, recálculo da média, duplicidade, pesquisa
  sem acento, PATCH parcial e exclusão em cascata.
- `DestinoControllerIT` — integração via MockMvc: fluxo completo (cadastro, consulta, pesquisa,
  avaliação, exclusão) e resposta de erro de validação.

---

## 9. Decisões técnicas e limitações conhecidas

| Decisão | Motivo | Consequência |
| --- | --- | --- |
| `Map` em memória | Escopo da etapa | Dados são perdidos ao reiniciar |
| `record` para DTOs | Imutabilidade e menos código | Exige Java 16+ |
| Mapeamento manual DTO ↔ entidade | Evita dependência externa | Mais código conforme o domínio crescer |
| Rota versionada `/api/v1` | Permite evoluir o contrato sem quebrar parceiros | — |
| Sem paginação | Volume pequeno nesta fase | `GET /destinos` cresce indefinidamente |
| PATCH ignora campos nulos | Simplicidade | Não distingue "campo ausente" de "definir como nulo" (JSON Merge Patch resolveria) |

**Sobre dados pessoais:** a avaliação armazena o nome do autor, que é dado pessoal. Antes de ir a
produção, o campo deve ser tratado conforme a LGPD — minimização (permitir apelido), base legal
definida, retenção limitada e log sem conteúdo identificável. O tratamento global de erros já evita
que dados da requisição sejam ecoados em mensagens de falha.

### Evolução futura

1. Persistência com Spring Data JPA (basta implementar `DestinoRepository`) e migrações com Flyway.
2. Autenticação com Spring Security (OAuth2/JWT) e autorização por perfil.
3. Documentação interativa com OpenAPI/Swagger UI.
4. Paginação e ordenação em `GET /destinos`.
5. Novos agregados: pacotes de viagem, hotéis e reservas.
6. Observabilidade com Spring Boot Actuator e pipeline de CI.

---

## 10. Onde cada requisito foi atendido

| Requisito | Implementação |
| --- | --- |
| Cadastrar destinos | `POST /api/v1/destinos` — `DestinoController.cadastrar` |
| Listar todos | `GET /api/v1/destinos` — `DestinoController.listar` |
| Pesquisar por nome ou localização | `GET /api/v1/destinos?nome=&localizacao=` — `DestinoRepositoryEmMemoria.listar` |
| Detalhar destino | `GET /api/v1/destinos/{id}` — `DestinoController.detalhar` |
| Atualizar destino | `PUT` e `PATCH /api/v1/destinos/{id}` |
| Registrar avaliação alterando a média | `POST /api/v1/destinos/{id}/avaliacoes` — `DestinoService.registrarAvaliacao` |
| Excluir destino | `DELETE /api/v1/destinos/{id}` |
| Separação em camadas | pacotes `controller`, `service`, `repository`, `model` |
| Armazenamento temporário em memória | `DestinoRepositoryEmMemoria`, `AvaliacaoRepositoryEmMemoria` |
| Documentação técnica | este README |
