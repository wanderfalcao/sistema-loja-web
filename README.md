# sistema-loja-web — Microsserviços

[![CI](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/ci.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/ci.yml)
[![Code Quality](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/code-quality.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/code-quality.yml)

Sistema de loja online construído com arquitetura de microsserviços em Java 21 / Spring Boot 3.2.3, composto por dois serviços independentes que se comunicam via REST.

**Disciplina:** Engenharia Disciplinada de Software — INFNET

---

## Sumário

1. [Visão geral](#visão-geral)
2. [Arquitetura](#arquitetura)
3. [Pré-requisitos](#pré-requisitos)
4. [Iniciar a stack completa](#iniciar-a-stack-completa)
5. [Desenvolvimento local (sem Docker)](#desenvolvimento-local-sem-docker)
6. [Acessar os serviços](#acessar-os-serviços)
7. [Executar os testes](#executar-os-testes)
8. [Estrutura do projeto](#estrutura-do-projeto)
9. [Comunicação entre serviços](#comunicação-entre-serviços)
10. [Máquina de estados do pedido](#máquina-de-estados-do-pedido)
11. [API REST — produto-service](#api-rest--produto-service)
12. [API REST — pedido-service](#api-rest--pedido-service)
13. [Decisões de design](#decisões-de-design)

---

## Visão geral

| Serviço | Responsabilidade | Porta |
|---------|-----------------|-------|
| **produto-service** | Catálogo de produtos, estoque e promoções | `8081` |
| **pedido-service** | Pedidos, itens e ciclo de vida (status) | `8082` |

Cada serviço possui banco de dados PostgreSQL próprio, interface web Thymeleaf, API REST e Swagger UI independentes.

---

## Arquitetura

```
┌──────────────────────────────┐      ┌──────────────────────────────┐
│       produto-service        │      │       pedido-service         │
│           :8081              │      │           :8082              │
│                              │      │                              │
│  ProdutoController (MVC)     │      │  PedidoController  (MVC)     │
│  ProdutoRestController (REST)│◄─────│  PedidoRestController (REST) │
│  ProdutoService              │ HTTP │  PedidoService               │
│  ProdutoRepository           │      │  PedidoRepository            │
│         │                    │      │  ProdutoServiceClient        │
│         ▼                    │      │         │                    │
│   PostgreSQL (produto_db)    │      │   PostgreSQL (pedido_db)     │
│        :5433                 │      │        :5434                 │
└──────────────────────────────┘      └──────────────────────────────┘
```

O `pedido-service` chama o `produto-service` via `RestTemplate` em dois momentos:
- **PENDENTE → PROCESSANDO**: debita estoque (SAÍDA) para cada item com `produtoId`
- **PROCESSANDO → CANCELADO**: devolve estoque (ENTRADA)

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| Java (JDK) | 21+ |
| Maven      | 3.9+ |
| Docker Desktop | 24+ |

---

## Iniciar a stack completa

```bash
# Dentro de sistema-loja-web/
cd sistema-loja-web

# Build e inicialização de todos os containers (5 no total)
docker compose up --build

# Acompanhar logs de um serviço específico
docker compose logs -f produto-service
docker compose logs -f pedido-service
```

O `docker-compose.yml` sobe:

| Container | Imagem | Porta host |
|-----------|--------|-----------|
| `produto-db` | postgres:16-alpine | `5433` |
| `pedido-db` | postgres:16-alpine | `5434` |
| `produto-service` | build local | `8081` |
| `pedido-service` | build local | `8082` |
| `pgadmin` | dpage/pgadmin4 | `5050` |

> O `pedido-service` aguarda o `produto-service` estar healthy antes de iniciar (via `depends_on: condition: service_healthy`).

### Parar tudo

```bash
docker compose stop

# Parar e remover volumes (apaga dados)
docker compose down -v
```

---

## Desenvolvimento local (sem Docker)

Para rodar os serviços localmente, suba apenas os bancos via Docker e rode cada aplicação separadamente.

### 1. Subir os bancos

```bash
docker compose up produto-db pedido-db -d
docker compose ps    # aguardar status healthy
```

### 2. Rodar produto-service (porta 8081)

```bash
cd produto-service
mvn spring-boot:run
```

### 3. Rodar pedido-service (porta 8082)

```bash
# Em outro terminal
cd pedido-service
mvn spring-boot:run
```

> Perfil `dev` é ativado por padrão. O `DataLoader` popula produtos e pedidos de exemplo automaticamente.

---

## Acessar os serviços

### produto-service (:8081)

| Recurso | URL |
|---------|-----|
| Interface web | http://localhost:8081/produtos |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| API REST | http://localhost:8081/api/v1/produtos |
| Health check | http://localhost:8081/actuator/health |

### pedido-service (:8082)

| Recurso | URL |
|---------|-----|
| Interface web | http://localhost:8082/pedidos |
| Swagger UI | http://localhost:8082/swagger-ui.html |
| API REST | http://localhost:8082/api/v1/pedidos |
| Health check | http://localhost:8082/actuator/health |

### pgAdmin

| Recurso | Valor |
|---------|-------|
| URL | http://localhost:5050 |
| Email | `admin@loja.com` |
| Senha | `admin` |
| Banco produto | host `produto-db`, porta `5432`, db `produto_db` |
| Banco pedido | host `pedido-db`, porta `5432`, db `pedido_db` |

---

## Executar os testes

Os testes usam H2 in-memory e não dependem dos containers.

### Todos os módulos

```bash
cd sistema-loja-web
mvn verify -B
```

### Por módulo

```bash
# produto-service
mvn test -pl produto-service -B

# pedido-service
mvn test -pl pedido-service -B

# Classe específica
mvn test -pl pedido-service -Dtest=PedidoServiceTest -B
```

### Suite de testes — produto-service (~95 testes unitários + 15 E2E)

| Classe | Tipo | Testes |
|--------|------|--------|
| `ProdutoServiceTest` | Mockito — service | 30 |
| `ProdutoControllerTest` | MockMvc — MVC | 16 |
| `ProdutoRestControllerTest` | MockMvc — REST | 15 |
| `ProdutoTest` | Domínio (entidade) | 20 |
| `SkuGeneratorTest` | Utilitário | 11 |
| `CategoriaProdutoTest` | Enum | 3 |
| `ProdutoSeleniumTest` | Selenium E2E (`@Tag("selenium")`) | 15 |

### Suite de testes — pedido-service (~165 testes unitários + 18 E2E)

| Classe | Tipo | Testes |
|--------|------|--------|
| `PedidoServiceTest` | Mockito — service | 36 |
| `PedidoControllerTest` | MockMvc — MVC | 22 |
| `PedidoRestControllerTest` | MockMvc — REST | 21 |
| `PedidoParametrizadoTest` | @ParameterizedTest | ~40 |
| `PedidoFalhaInfraTest` | Falhas de infraestrutura | 9 |
| `PedidoServiceDTOTest` | Service — camada DTO | 12 |
| `PedidoMapperTest` | MapStruct | 9 |
| `ItemPedidoTest` | Domínio (item) | 9 |
| `PedidoFuzzTest` | jqwik property-based | 6 |
| `PedidoSeleniumTest` | Selenium E2E (`@Tag("selenium")`) | 18 |

### Selenium E2E

Os testes Selenium usam Chrome headless e são excluídos do ciclo `mvn verify` padrão para evitar dependência de browser no CI de build. Executar separadamente:

```bash
# Requer Chrome instalado na máquina
mvn test -pl produto-service -Dgroups=selenium -B
mvn test -pl pedido-service  -Dgroups=selenium -B
```

Os testes do `PedidoSeleniumTest` isolam o `ProdutoServiceClient` via `@MockBean` para não depender do produto-service em execução.

### Cobertura JaCoCo

```bash
# Gerar relatório
mvn verify -pl produto-service -B
mvn verify -pl pedido-service -B

# Abrir (Windows)
start produto-service\target\site\jacoco\index.html
start pedido-service\target\site\jacoco\index.html
```

| Módulo | Mínimo configurado | Cobertura obtida |
|--------|-------------------|-----------------|
| produto-service | 90% LINE | ≥ 90% |
| pedido-service | 90% LINE | ≥ 90% |

---

## Estrutura do projeto

```
sistema-loja-web/
├── pom.xml                          Parent Maven (módulos produto-service, pedido-service)
├── docker-compose.yml               Orquestra 5 containers
│
├── produto-service/                 Microsserviço de catálogo (:8081)
│   ├── src/main/java/br/com/infnet/
│   │   ├── config/
│   │   │   ├── DataLoader.java           Seed de produtos de exemplo (perfil dev)
│   │   │   └── RestTemplateConfig.java   (não utilizado neste serviço)
│   │   ├── produto/
│   │   │   ├── domain/
│   │   │   │   ├── Produto.java          Entidade rica com comandos de domínio
│   │   │   │   ├── Promocao.java         Value object @Embedded (desconto + datas)
│   │   │   │   ├── CategoriaProduto.java Enum: MONITORES, PERIFERICOS, ARMAZENAMENTO,
│   │   │   │   │                               COMPONENTES, AUDIO_VIDEO, GERAL
│   │   │   │   ├── TipoOperacaoEstoque.java  Enum: ENTRADA / SAIDA
│   │   │   │   └── SkuGenerator.java     Gera SKU único a partir do nome
│   │   │   ├── dto/
│   │   │   │   ├── ProdutoRequest.java
│   │   │   │   ├── ProdutoResponse.java
│   │   │   │   ├── AjusteEstoqueRequest.java
│   │   │   │   └── PromocaoRequest.java
│   │   │   ├── factory/ProdutoFactory.java
│   │   │   ├── mapper/ProdutoMapper.java  MapStruct
│   │   │   ├── repository/ProdutoRepository.java
│   │   │   ├── service/ProdutoService.java
│   │   │   └── controller/
│   │   │       ├── ProdutoController.java      MVC /produtos
│   │   │       └── ProdutoRestController.java  REST /api/v1/produtos
│   │   ├── controller/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── RestExceptionHandler.java
│   │   └── shared/
│   │       ├── config/JpaAuditingConfig.java
│   │       └── exception/
│   │           ├── DomainException.java
│   │           └── RecursoNaoEncontradoException.java
│   └── src/main/resources/
│       ├── application.properties
│       ├── application-dev.properties       PostgreSQL localhost:5433
│       ├── application-docker.properties    PostgreSQL produto-db:5432
│       └── application-prod.properties      Variáveis de ambiente
│
└── pedido-service/                  Microsserviço de pedidos (:8082)
    ├── src/main/java/br/com/infnet/
    │   ├── client/
    │   │   ├── ProdutoServiceClient.java      Interface do cliente HTTP
    │   │   ├── ProdutoServiceClientImpl.java  RestTemplate (timeout 3s/5s)
    │   │   ├── ProdutoInfo.java               DTO da resposta do produto-service
    │   │   └── EstoqueAjusteRequest.java      DTO de ajuste de estoque
    │   ├── config/
    │   │   ├── DataLoader.java                Seed de pedidos de exemplo (perfil dev)
    │   │   └── RestTemplateConfig.java        Timeout: 3s connect / 5s read
    │   ├── pedido/
    │   │   ├── domain/
    │   │   │   ├── Pedido.java                Entidade principal
    │   │   │   ├── ItemPedido.java            Item com snapshot de produto
    │   │   │   ├── StatusPedido.java          Enum: PENDENTE, PROCESSANDO,
    │   │   │   │                                    CONCLUIDO, CONTESTADO, CANCELADO
    │   │   │   └── StatusHistorico.java       Audit trail de transições
    │   │   ├── dto/
    │   │   │   ├── PedidoRequest.java
    │   │   │   ├── PedidoResponse.java
    │   │   │   ├── ItemPedidoRequest.java
    │   │   │   ├── ItemPedidoResponse.java
    │   │   │   └── ContestarRequest.java
    │   │   ├── factory/
    │   │   │   ├── PedidoFactory.java
    │   │   │   └── ItemPedidoFactory.java
    │   │   ├── mapper/PedidoMapper.java        MapStruct
    │   │   ├── repository/
    │   │   │   ├── PedidoRepository.java
    │   │   │   └── StatusHistoricoRepository.java
    │   │   ├── service/PedidoService.java
    │   │   └── controller/
    │   │       ├── PedidoController.java       MVC /pedidos
    │   │       └── PedidoRestController.java   REST /api/v1/pedidos
    │   ├── controller/
    │   │   ├── GlobalExceptionHandler.java
    │   │   └── RestExceptionHandler.java       Inclui handler 502 para falha HTTP
    │   └── shared/
    │       ├── config/JpaAuditingConfig.java
    │       └── exception/
    └── src/main/resources/
        ├── application.properties
        ├── application-dev.properties          PostgreSQL localhost:5434
        ├── application-docker.properties       PostgreSQL pedido-db:5432
        └── application-prod.properties         Variáveis de ambiente
```

---

## Comunicação entre serviços

O `pedido-service` chama o `produto-service` através de `ProdutoServiceClientImpl` (RestTemplate com timeout).

### URL configurável

```properties
# application.properties (pedido-service)
produto.service.url=http://localhost:8081        # perfil dev
# application-docker.properties
produto.service.url=http://produto-service:8081  # perfil docker
```

### Fluxo de integração

```
PedidoService.avancarStatus()
       │
       ├── PENDENTE → PROCESSANDO
       │        └── para cada item com produtoId:
       │               PATCH /api/v1/produtos/{id}/estoque  {"operacao":"SAIDA",  "quantidade": N}
       │
       └── PROCESSANDO → CANCELADO
                └── para cada item com produtoId:
                       PATCH /api/v1/produtos/{id}/estoque  {"operacao":"ENTRADA","quantidade": N}

PedidoService.adicionarItem()
       └── se produtoId fornecido:
              GET /api/v1/produtos/{id}  →  auto-preenche nome, SKU e preço
```

### Resiliência

- Timeout de conexão: **3 segundos**; timeout de leitura: **5 segundos**
- Falha HTTP 4xx do produto-service → 502 Bad Gateway no pedido-service
- Produto inativo → `DomainException` antes de adicionar item ao pedido

---

## Máquina de estados do pedido

```
PENDENTE ──────► PROCESSANDO ──────► CONCLUIDO
    │                  │                  │
    │                  │                  ▼
    └──► CANCELADO ◄───┘         CONTESTADO ──► PROCESSANDO
                                       │
                                       └──► CANCELADO
```

| Estado Atual | Próximos estados permitidos |
|--------------|-----------------------------|
| PENDENTE | PROCESSANDO ¹, CANCELADO |
| PROCESSANDO | CONCLUIDO, CANCELADO ² |
| CONCLUIDO | CONTESTADO |
| CONTESTADO | PROCESSANDO, CANCELADO |
| CANCELADO | *(terminal — nenhum)* |

¹ Exige pelo menos um item no pedido. Debita estoque no produto-service.
² Devolve estoque ao produto-service para itens com `produtoId`.

---

## API REST — produto-service

Base path: `/api/v1/produtos`

| Método | Endpoint | Descrição | Status |
|--------|----------|-----------|--------|
| `GET` | `/` | Listar ativos (paginado) | `200` |
| `GET` | `/{id}` | Buscar por ID | `200` / `404` |
| `GET` | `/sku/{sku}` | Buscar por SKU | `200` / `422` |
| `POST` | `/` | Criar produto | `201` / `400` / `422` |
| `PUT` | `/{id}` | Atualizar produto | `200` / `404` / `422` |
| `DELETE` | `/{id}` | Remover produto | `204` / `422` |
| `PATCH` | `/{id}/estoque` | Ajustar estoque (ENTRADA/SAIDA) | `200` / `422` |
| `PATCH` | `/{id}/promocao` | Ativar promoção | `200` / `422` |
| `DELETE` | `/{id}/promocao` | Encerrar promoção | `200` |

Erros retornam **RFC 7807 ProblemDetail**: `400` (validação), `404` (não encontrado), `422` (regra de negócio).

---

## API REST — pedido-service

Base path: `/api/v1/pedidos`

| Método | Endpoint | Descrição | Status |
|--------|----------|-----------|--------|
| `GET` | `/` | Listar (paginado) | `200` |
| `GET` | `/{id}` | Buscar por ID | `200` / `404` |
| `POST` | `/` | Criar pedido | `201` / `400` / `422` |
| `PUT` | `/{id}` | Atualizar pedido | `200` / `404` / `422` |
| `POST` | `/{id}/status` | Avançar status | `200` / `422` / `502` |
| `POST` | `/{id}/contestar` | Contestar pedido | `200` / `422` |
| `DELETE` | `/{id}` | Deletar pedido | `204` / `404` |
| `POST` | `/{id}/itens` | Adicionar item | `200` / `422` / `502` |
| `DELETE` | `/{id}/itens/{itemId}` | Remover item | `200` / `422` |
| `GET` | `/{id}/itens` | Listar itens | `200` / `404` |

---

## Decisões de design

### Dois bancos de dados independentes
Cada microsserviço possui seu próprio PostgreSQL. Não há joins entre bancos. A consistência entre estoques e pedidos é mantida via chamada REST síncrona em cada transição de status.

### Snapshot de produto no item
Quando um item é adicionado ao pedido, `nomeProduto`, `skuProduto` e `precoUnitario` são copiados para `ItemPedido`. Isso garante que alterações futuras no produto não afetam pedidos já criados.

### Auto-enriquecimento opcional
Se o campo `produtoId` for informado ao adicionar um item (via REST ou formulário web), o pedido-service busca automaticamente os dados atuais do produto-service e sobrescreve os campos do snapshot. Isso elimina erros de digitação e garante consistência com o catálogo.

### Histórico de status (audit trail)
Cada transição de status gera um registro em `pedido_status_historico` com status anterior, novo e timestamp. A timeline é exibida na tela de detalhe do pedido.

### Dois handlers de exceção por serviço
Cada serviço mantém dois `@ControllerAdvice` separados:
- **`GlobalExceptionHandler`** — erros MVC → redirect com flash message amigável
- **`RestExceptionHandler`** — erros REST → RFC 7807 `ProblemDetail` com tipo, título e detalhe

### RestTemplate com timeout
O `pedido-service` configura `SimpleClientHttpRequestFactory` com connect timeout de 3 s e read timeout de 5 s. Sem isso, uma falha no produto-service travaria o pedido-service indefinidamente.

### Spring Profiles
| Perfil | Banco | Uso |
|--------|-------|-----|
| `dev` | PostgreSQL localhost | Desenvolvimento local |
| `docker` | PostgreSQL hostname Docker | Container |
| `prod` | Variáveis de ambiente | Produção |
| `test` | H2 in-memory | Testes automatizados |
