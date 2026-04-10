# sistema-loja-web

[![CI](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/ci.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/ci.yml)
[![Code Quality](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/code-quality.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/code-quality.yml)
[![Security](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/security.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/security.yml)
[![Deploy](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/deploy.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/deploy.yml)

Sistema de loja construído com microsserviços Spring Boot. O `produto-service` gerencia o catálogo e o estoque; o `pedido-service` controla o ciclo de vida dos pedidos e chama o produto-service para ajustar estoque nas transições de status. Tudo passa pelo `api-gateway`, que roteia as requisições e serve o Swagger unificado dos dois serviços.

## Arquitetura

### Docker Compose (local)

```
  Browser
     │  HTTP :8080
     ▼
┌─────────────────────────────────────────────────────┐
│              API Gateway  :8080                     │
│  Spring Cloud Gateway + Eureka LoadBalancer         │
│  /produtos/** → produto-service  (lb://)            │
│  /pedidos/**  → pedido-service   (lb://)            │
└──────────────┬──────────────────┬───────────────────┘
               │                  │
   ┌───────────▼──────┐   ┌───────▼───────────┐
   │  produto-service │   │  pedido-service    │
   │  :8081           │◄──│  :8082             │
   │                  │   │  (via api-gateway) │
   └────────┬─────────┘   └────────┬───────────┘
            │                      │
       produto-db              pedido-db
        :5433                   :5434

  Eureka Server :8761  —  service discovery
  SonarQube     :9000  —  qualidade de código
```

### Cloud Run (prod/dev)

```
  Browser
     │  HTTPS
     ▼
┌─────────────────────────────────────────────────────┐
│              API Gateway (Cloud Run)                │
│  /produtos/** → produto-service-dev                 │
│  /pedidos/**  → pedido-service-dev                  │
└──────────────┬──────────────────┬───────────────────┘
               │                  │
   ┌───────────▼──────┐   ┌───────▼───────────┐
   │  produto-service │   │  pedido-service    │
   │  Cloud Run       │◄──│  Cloud Run         │
   │                  │   │  (sem Eureka —     │
   │                  │   │   URL direta)      │
   └────────┬─────────┘   └────────┬───────────┘
            │                      │
       Neon (produto-db)      Neon (pedido-db)
       PostgreSQL 16          PostgreSQL 16
```

## Como subir

```bash
docker-compose up --build
```

Aguarda todos os health checks (~2 min na primeira vez). Ao finalizar, o terminal exibe os endereços:

| Serviço     | URL                                             |
| ----------- | ----------------------------------------------- |
| API Gateway | http://localhost:8080                           |
| Pedidos     | http://localhost:8080/pedidos                   |
| Produtos    | http://localhost:8080/produtos                  |
| Swagger UI  | http://localhost:8080/swagger-ui.html           |
| Eureka      | http://localhost:8761                           |
| SonarQube   | http://localhost:9000  (admin / admin)          |
| PgAdmin     | http://localhost:5050  (admin@loja.com / admin) |

Para subir só o essencial sem SonarQube e PgAdmin:

```bash
docker-compose up eureka-server produto-service pedido-service api-gateway
```

Para derrubar tudo e limpar os volumes:

```bash
docker-compose down -v
```

## Desenvolvimento local (sem Docker)

Suba apenas os bancos e o Eureka, depois rode cada serviço individualmente:

```bash
docker-compose up produto-db pedido-db eureka-server -d

# terminal 1
cd produto-service && mvn spring-boot:run

# terminal 2
cd pedido-service && mvn spring-boot:run

# terminal 3
cd api-gateway && mvn spring-boot:run
```

O `DataLoader` popula produtos e pedidos de exemplo automaticamente em qualquer perfil exceto `prod`.

## Endpoints (via Gateway — :8080)

### Produtos

| Método | Caminho                        | Descrição                             |
| ------ | ------------------------------ | ------------------------------------- |
| GET    | /api/v1/produtos               | Lista produtos ativos (paginado)      |
| POST   | /api/v1/produtos               | Cadastra produto                      |
| GET    | /api/v1/produtos/{id}          | Busca por ID                          |
| GET    | /api/v1/produtos/sku/{sku}     | Busca por SKU                         |
| PUT    | /api/v1/produtos/{id}          | Atualiza produto                      |
| DELETE | /api/v1/produtos/{id}          | Remove produto (exige estoque zerado) |
| PATCH  | /api/v1/produtos/{id}/estoque  | Ajusta estoque (ENTRADA ou SAIDA)     |
| PATCH  | /api/v1/produtos/{id}/promocao | Ativa promoção com percentual e datas |
| DELETE | /api/v1/produtos/{id}/promocao | Encerra promoção ativa                |

### Pedidos

| Método | Caminho                             | Descrição                                    |
| ------ | ----------------------------------- | -------------------------------------------- |
| GET    | /api/v1/pedidos                     | Lista pedidos (paginado)                     |
| POST   | /api/v1/pedidos                     | Cria pedido                                  |
| GET    | /api/v1/pedidos/{id}                | Busca por ID                                 |
| PUT    | /api/v1/pedidos/{id}                | Atualiza pedido                              |
| DELETE | /api/v1/pedidos/{id}                | Remove pedido                                |
| POST   | /api/v1/pedidos/{id}/status         | Muda status (`?novoStatus=PROCESSANDO` etc.) |
| POST   | /api/v1/pedidos/{id}/contestar      | Contesta pedido concluído com motivo         |
| POST   | /api/v1/pedidos/{id}/itens          | Adiciona item ao pedido (apenas PENDENTE)    |
| DELETE | /api/v1/pedidos/{id}/itens/{itemId} | Remove item do pedido (apenas PENDENTE)      |
| GET    | /api/v1/pedidos/{id}/itens          | Lista itens do pedido                        |

Documentação interativa: http://localhost:8080/swagger-ui.html (dropdown para alternar entre os serviços)

## Testes

Os testes não dependem dos containers Docker — usam banco em memória ou Testcontainers para PostgreSQL.

```bash
# Roda todos os testes (unitários + integração, exclui Selenium)
mvn test -pl produto-service,pedido-service

# Testes + relatório de cobertura (build falha se < 90% de linhas)
mvn verify -pl produto-service,pedido-service

# Abre o relatório HTML após o verify (Windows)
start produto-service\target\site\jacoco\index.html
start pedido-service\target\site\jacoco\index.html
```

**O que está coberto nos testes:**

- *Unitários com Mockito*: validações de regras de negócio, transições de status, comportamentos de borda
- *MockMvc*: controladores REST e MVC, validação de payloads, códigos HTTP e estrutura da resposta
- *Integração com WireMock*: simula o produto-service em cenários de timeout, produto não encontrado e serviço indisponível — o pedido-service não sabe que está falando com um mock
- *Property-based (jqwik)*: executa centenas de combinações geradas automaticamente para validar invariantes, incluindo tentativas de SQL injection e XSS nos campos de texto
- *Selenium E2E*: navega pelas interfaces Thymeleaf como um usuário real, valida formulários, listagens e detalhes

Os testes Selenium usam `@Tag("selenium")` e ficam fora do ciclo padrão. O banco de teste sobe e desce automaticamente via Docker — basta ter o Docker rodando e o Chrome instalado:

```bash
# Headless (padrão — mesmo comportamento do CI)
mvn verify -pl produto-service -Dgroups=selenium
mvn verify -pl pedido-service -Dgroups=selenium

# Com browser visível (útil para depurar falhas localmente)
mvn verify -pl produto-service -Dgroups=selenium -Dselenium.headless=false
mvn verify -pl pedido-service -Dgroups=selenium -Dselenium.headless=false
```

A extensão `SeleniumExtension` implementa `TestWatcher` e captura screenshot automaticamente em `target/screenshots/{Classe}/{metodo}.png` quando um teste falha — sem necessidade de configuração adicional.

## Workflows GitHub Actions

Os arquivos ficam em `.github/workflows/`. Para verificar execuções passadas: aba **Actions** do repositório. Para disparar manualmente qualquer workflow: **Actions → selecione o workflow → Run workflow**.

**ci.yml** roda em dois jobs. O primeiro compila e testa os dois serviços — unitários, WireMock e jqwik — e exige cobertura mínima de 90% no JaCoCo para passar. O segundo roda os Selenium só depois do primeiro verde, usando Testcontainers para subir o banco dentro do próprio runner. PR com esse workflow vermelho não entra.

**code-quality.yml** roda Checkstyle no estilo Google. Violação de severidade `error` quebra o build.

**security.yml** roda CodeQL para análise estática de segurança em Java. Executa em todo push para master e toda segunda-feira. Resultados ficam em **Security → Code scanning alerts**.

**sonarqube.yml** sobe um SonarQube Community Edition temporário direto no runner do Actions, sem precisar de servidor dedicado. Roda o `mvn sonar:sonar` nos dois serviços e exibe o resultado do Quality Gate no log. Para rodar local veja a seção abaixo.

**deploy.yml** dispara quando o ci.yml passa. Autentica no GCP via OIDC — sem chave JSON, só um JWT que o GCP valida pelo Workload Identity Federation. Publica as imagens no Artifact Registry e faz deploy no Cloud Run em três etapas: `dev` → `test` → `prod`. O `prod` trava até alguém aprovar manualmente no GitHub Environments.

## Banco de dados em produção

Localmente os bancos sobem via Docker Compose (`produto-db :5433` e `pedido-db :5434`). No Cloud Run cada serviço usa uma instância separada no [Neon.tech](https://neon.tech) — PostgreSQL gerenciado com free tier.

| Ambiente    | Banco                          |
| ----------- | ------------------------------ |
| local / dev | Docker Compose (PostgreSQL 16) |
| Cloud Run   | Neon.tech PostgreSQL           |

A connection string fica nos secrets `PRODUTO_DB_URL` e `PEDIDO_DB_URL` no formato `jdbc:postgresql://...?sslmode=require`.

## Análise de qualidade com SonarQube

Para rodar localmente:

```bash
# Sobe o SonarQube (profile analysis — não inicia com docker compose up normal)
docker compose --profile analysis up -d sonarqube

# Aguarda UP (~90s). Acesse http://localhost:9000 (admin / admin),
# gere um token em My Account → Security → Generate Token e use abaixo:
mvn -B clean verify -pl .,produto-service,pedido-service -am -Dspring.profiles.active=test

mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<SEU_TOKEN> \
  -Dsonar.projectKey=sistema-loja-web \
  "-Dsonar.exclusions=**/DataLoader.java,**/*Application.java,**/dto/**,**/mapper/**,api-gateway/**"
```

Dashboard disponível em http://localhost:9000/dashboard?id=sistema-loja-web

## O que mudou na refatoração

No TP4 os serviços foram reorganizados para terem uma estrutura comum: domínio, serviço, controlador, repositório e configuração bem separados. Antes disso o código estava espalhado sem padrão definido.

O mapeamento entre entidades e DTOs passou para MapStruct — gerado em tempo de compilação, sem conversão manual. As fábricas centralizaram a criação dos objetos de domínio. O tratamento de erros foi separado por protocolo: um handler para MVC (redireciona com mensagem) e outro para a API REST (formato RFC 7807).

A comunicação entre os serviços foi encapsulada em uma interface dedicada usando Apache HttpClient 5, necessário para o PATCH funcionar. Em Docker toda chamada passa pelo gateway, que também faz o balanceamento via Eureka.

---

## Decisões de projeto (TP5)

### Imutabilidade

As entidades de domínio não têm setters públicos. Para mudar o estado de um objeto, existe um método com nome que diz o que está acontecendo: `ajustarEstoque`, `avancarStatus`, `contestar`. Fica mais fácil de rastrear o que muda e de onde vem a mudança.

`ProdutoRequest` trocou o `@Data` por `@Getter @Builder` para não gerar setters. A deserialização do Jackson continua funcionando pelo `@AllArgsConstructor`.

### Polimorfismo nos enums

`TipoOperacaoEstoque` tem um método abstrato `validarAntesDeAplicar(boolean ativo)` implementado por cada valor do enum. `SAIDA` lança exceção se o produto estiver inativo; `ENTRADA` não tem restrição. O `Produto.ajustarEstoque()` só chama o enum — não compara `if (operacao == SAIDA)` em nenhum momento.

`CategoriaProduto` tem `descontoMaximoPermitido()` abstrato. Cada categoria define seu próprio teto:

| Categoria     | Desconto máximo |
| ------------- | --------------- |
| MONITORES     | 30%             |
| PERIFERICOS   | 40%             |
| ARMAZENAMENTO | 25%             |
| COMPONENTES   | 20%             |
| AUDIO_VIDEO   | 35%             |
| GERAL         | 50%             |

Quando `ativarPromocao()` é chamado, a validação do desconto usa o método da categoria — sem nenhum switch ou if.

### motivoContestacao separado de observacao

Um pedido tem dois campos de texto distintos: `observacao` (preenchida na criação, editável enquanto pendente) e `motivoContestacao` (preenchido apenas ao contestar). Antes, o motivo sobrescrevia a observação original, perdendo a informação do cliente. Agora `contestar(String motivo)` grava em `motivoContestacao` e nunca toca em `observacao`. O `PedidoResponse` expõe os dois campos. A tela de detalhe exibe o motivo em um bloco destacado apenas quando o pedido está CONTESTADO.

### Resiliência na comunicação entre serviços

`ProdutoServiceClientImpl` combina retry e circuit breaker. O `@Retryable` tenta `ajustarEstoque` até 3 vezes com backoff (500 ms → 1 s → 2 s) em erros de rede e 5xx; o `@Recover` converte a falha final em `DomainException`. Erros 4xx não são retentados — indicam problema de negócio, não de infraestrutura. O circuit breaker (Resilience4j) abre após 50% de falhas em 10 chamadas e fica fechado por 30 s. A ordem AOP garante: CB (externo) → Retry (interno) → método.

O `EstoqueOrquestrador` usa Saga compensatório — se um item falhar após outros já processados, a operação inversa é enviada ao produto-service. Falhas na compensação vão para o log e não mascaram o erro original.

### Interface de criação de pedidos

Layout em duas colunas: grade de cards com busca client-side (por nome e SKU) à esquerda e carrinho com total estimado à direita. Seleção e busca acontecem sem nenhuma requisição ao servidor.

### Regras de negócio adicionadas

- Promoção com datas precisa durar pelo menos 1 hora (`Promocao.criar`).
- Um pedido aceita no máximo 20 itens (`PedidoService.adicionarItem`).
- Produto ativo com estoque zero não pode ser cadastrado nem atualizado (`ProdutoService.validarAtivacaoComEstoque`).

### CI/CD

Deploy via OIDC + Workload Identity Federation — sem chave JSON nos secrets. Cada ambiente tem proteção própria; `prod` trava até aprovação manual no GitHub.

---

## Fluxo de status do pedido

Um pedido começa como PENDENTE. O avanço ao status PROCESSANDO é o único momento em que o estoque é debitado no produto-service — um item por vez, usando os IDs de produto registrados no pedido. Se o pedido for cancelado a partir de PROCESSANDO, o estoque é devolvido. Pedidos CONCLUIDOS podem ser contestados e retornar ao processamento; a partir do estado CANCELADO não há mais transições possíveis.

```
PENDENTE ──────► PROCESSANDO ──────► CONCLUIDO
    │                 │                  │
    └────────────────►│                  ▼
                      └───────► CANCELADO   CONTESTADO
                                    ▲         │   │
                                    └─────────┘   │
                                                  └──► PROCESSANDO
```

| De          | Para        | Observação             |
| ----------- | ----------- | ---------------------- |
| PENDENTE    | PROCESSANDO | Debita estoque         |
| PENDENTE    | CANCELADO   | Sem impacto em estoque |
| PROCESSANDO | CONCLUIDO   | —                      |
| PROCESSANDO | CANCELADO   | Devolve estoque        |
| CONCLUIDO   | CONTESTADO  | Abre contestação       |
| CONTESTADO  | PROCESSANDO | Reinicia processamento |
| CONTESTADO  | CANCELADO   | —                      |

---

---

## Entrega Final — TP5

### 1. Arquitetura Final do Sistema

O sistema é composto por quatro serviços Spring Boot que se comunicam via HTTP, orquestrados por um API Gateway central.

```
 Browser
    │  HTTP
    ▼
┌─────────────────────────────────────────────────────┐
│              API Gateway  :8080                     │
│  Spring Cloud Gateway + Eureka LoadBalancer         │
│  Rotas: /produtos/** → produto-service              │
│          /pedidos/**  → pedido-service              │
│          /swagger-ui  → agregação OpenAPI           │
└────────────────┬────────────────┬───────────────────┘
                 │ lb://          │ lb://
    ┌────────────▼──────┐   ┌────▼──────────────┐
    │  produto-service  │   │  pedido-service    │
    │  :8081            │◄──│  :8082             │
    │                   │   │                   │
    │  Domínio:         │   │  Domínio:         │
    │  Produto          │   │  Pedido           │
    │  Sku (VO)         │   │  ItemPedido       │
    │  Dinheiro (VO)    │   │  StatusHistorico  │
    │  Quantidade (VO)  │   │  Dinheiro (VO)    │
    │  CategoriaProduto │   │  Quantidade (VO)  │
    │  TipoOpEstoque    │   │  StatusPedido     │
    │  Promocao         │   │                   │
    └────────┬──────────┘   └────────┬──────────┘
             │                       │
        produto_db              pedido_db
       PostgreSQL 16           PostgreSQL 16
      (local :5433 /          (local :5434 /
       Neon em prod)           Neon em prod)

         Eureka Server :8761 — registro e descoberta
```

**Padrões aplicados:**
- *Database per Service*: cada serviço tem seu próprio banco, sem compartilhamento de schema
- *API Gateway*: ponto único de entrada para o browser e roteamento interno via Eureka
- *Value Objects imutáveis*: `Dinheiro`, `Quantidade`, `Sku` encapsulam validação e semântica
- *Factory Methods*: `Produto.novo()`, `Pedido.novo()`, `ItemPedido.criar()` centralizam criação
- *MapStruct*: mapeamento entidade ↔ DTO gerado em tempo de compilação, sem reflexão em runtime

**Comunicação entre serviços:**
O `pedido-service` chama o `produto-service` via `ProdutoServiceClientImpl` (RestTemplate + Apache HttpClient 5 para suporte a PATCH). Em Docker a chamada vai direto pela rede interna — o gateway é ponto de entrada exclusivo para clientes externos. No Cloud Run vai direto à URL do serviço (`eureka.client.enabled=false` no perfil `prod`).

**Rate Limiting no Gateway:**
Rate limiting por IP no `RateLimitFilter` (Bucket4j): 20 req/s por IP, `429` quando excedido. Implementação em memória, sem Redis.

---

### 2. Configuração e Funcionamento dos Workflows no GitHub Actions

O repositório possui cinco workflows em `.github/workflows/`:

#### `ci.yml` — Integração Contínua
Dispara em todo push para `master` e em pull requests.

| Job        | O que faz                                                                                                                                                       |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `testes`   | `mvn verify` nos dois serviços. JaCoCo exige ≥ 90% de cobertura de linhas — build falha se não atingir. Publica relatório JUnit e HTML do JaCoCo como artefato. |
| `selenium` | Roda após `testes`. Sobe os serviços com Testcontainers (PostgreSQL real no runner) e executa os testes `@Tag("selenium")` com ChromeDriver headless.           |

Resumo gerado em `$GITHUB_STEP_SUMMARY` com resultado de cada job, cobertura e artefatos.

#### `code-quality.yml` — Qualidade Estática
Roda Checkstyle com o estilo Google. Qualquer violação de severidade `error` quebra o build imediatamente.

#### `security.yml` — Segurança (SAST + DAST)
Dispara em push para `master` e toda segunda-feira às 02h UTC.

| Job      | Ferramenta         | O que analisa                                                                                                                             |
| -------- | ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `codeql` | GitHub CodeQL      | Análise estática do código Java. Queries `security-and-quality`. Resultados em **Security → Code scanning alerts**.                       |
| `dast`   | OWASP ZAP Baseline | Sobe o `produto-service` com PostgreSQL real no runner e executa scan dinâmico HTTP. Relatório HTML disponível como artefato por 14 dias. |

#### `sonarqube.yml` — Quality Gate
Sobe o SonarQube Community Edition diretamente no runner (sem servidor dedicado). Executa análise nos dois serviços com JaCoCo e exibe resultado do Quality Gate no log e no `$GITHUB_STEP_SUMMARY`.

#### `deploy.yml` — Deploy Automatizado
Dispara quando o `ci.yml` conclui com sucesso (`workflow_run`), em releases publicadas ou manualmente (`workflow_dispatch`).

```
CI verde
   │
   ▼
build-and-push ──► ghcr.io (cache)
                   Artifact Registry GCP (fonte de verdade)
   │
   ▼
deploy-dev ──────► Cloud Run (automático)
   │
   ▼
deploy-test ─────► Cloud Run (aguarda aprovação manual)
   │
   ▼
deploy-prod ─────► Cloud Run (aguarda aprovação manual)
```

Autenticação via **OIDC + Workload Identity Federation**: o GitHub emite um JWT que o GCP valida diretamente — nenhuma chave JSON nos secrets, só `GCP_WIF_PROVIDER` e `GCP_SERVICE_ACCOUNT`.

---

### 3. Ambientes de Deploy e suas Proteções

| Ambiente | Serviço Cloud Run                               | Trigger                  | Proteção                                                               |
| -------- | ----------------------------------------------- | ------------------------ | ---------------------------------------------------------------------- |
| **dev**  | `produto-service-dev`<br>`pedido-service-dev`   | Automático após CI verde | Nenhuma — deploy imediato                                              |
| **test** | `produto-service-test`<br>`pedido-service-test` | Automático após dev      | **Required reviewers**: aprovação manual obrigatória antes de executar |
| **prod** | `produto-service`<br>`pedido-service`           | Automático após test     | **Required reviewers**: aprovação manual obrigatória antes de executar |

**Configuração dos ambientes** (GitHub → Settings → Environments):
- `dev`: sem proteção, deploy imediato
- `test`: reviewer obrigatório — o job fica pausado e envia notificação até alguém aprovar no GitHub
- `prod`: reviewer obrigatório — mesmo fluxo, com a visibilidade de que é produção

**Variáveis por ambiente (Cloud Run env vars):**

| Variável                 | dev                       | test               | prod               |
| ------------------------ | ------------------------- | ------------------ | ------------------ |
| `SPRING_PROFILES_ACTIVE` | `prod`                    | `prod`             | `prod`             |
| `SPRING_DATASOURCE_URL`  | `PRODUTO_DB_URL` (secret) | idem               | idem               |
| `PRODUTO_SERVICE_URL`    | URL Cloud Run dev         | URL Cloud Run test | URL Cloud Run prod |
| `APP_PRODUTO_BASE_URL`   | URL Cloud Run dev         | URL Cloud Run test | URL Cloud Run prod |

Cada serviço escala para zero instâncias quando ocioso (dev/test: 0–2, prod: 1–10). Banco de dados gerenciado pelo [Neon.tech](https://neon.tech) com SSL obrigatório.

---

### 4. Estratégias de Testes

#### Testes locais (executam no CI — `ci.yml`)

| Tipo                      | Tecnologia                | O que valida                                                                                                                                                                                                                                                                                             |
| ------------------------- | ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Unitários**             | JUnit 5 + Mockito         | Regras de negócio isoladas: guard clauses, transições de status, cálculo de subtotal, validações de value objects                                                                                                                                                                                        |
| **MockMvc**               | Spring MVC Test           | Controladores MVC e REST: payloads, códigos HTTP, redirecionamentos, flash attributes                                                                                                                                                                                                                    |
| **Integração**            | WireMock + Testcontainers | `pedido-service` chamando `produto-service` simulado em cenários de falha (timeout, 404, serviço indisponível) com PostgreSQL real                                                                                                                                                                       |
| **Property-based (fuzz)** | jqwik                     | Centenas de entradas geradas automaticamente validam invariantes. Inclui tentativas de SQL injection e XSS nos campos de texto — nenhuma deve lançar NullPointerException ou vazar dados                                                                                                                 |
| **Selenium E2E**          | Selenium 4 + ChromeDriver | Navega pela UI como um usuário real. Inclui cenários de preservação de observação ao contestar, exibição do motivo de contestação no detalhe, e rejeição de produto ativo sem estoque. `SeleniumExtension` captura screenshot em falha. Headless por padrão, modo visual via `-Dselenium.headless=false` |
| **Cobertura mínima**      | JaCoCo                    | 90% de linhas cobertas. Build falha automaticamente se não atingir                                                                                                                                                                                                                                       |

#### Análise estática e dinâmica (executam no `security.yml`)

| Tipo     | Tecnologia         | Frequência                     |
| -------- | ------------------ | ------------------------------ |
| **SAST** | GitHub CodeQL      | Todo push + toda segunda-feira |
| **DAST** | OWASP ZAP Baseline | Todo push + toda segunda-feira |

O ZAP sobe o `produto-service` com PostgreSQL real no runner e executa varredura HTTP, identificando headers ausentes, endpoints expostos e configurações inseguras.

#### Testes pós-deploy (executam no `deploy.yml`)

Após cada deploy bem-sucedido (dev, test e prod), o pipeline:

1. Faz health check nos endpoints `/actuator/health` dos dois serviços — aguarda até 120s para o Cloud Run escalar a instância
2. Executa os testes `@Tag("selenium")` apontando para as URLs reais do Cloud Run via `-Dselenium.base.url.produtos` e `-Dselenium.base.url.pedidos`
3. Os testes Selenium navegam pela UI Thymeleaf como um usuário real: criam produtos, fazem pedidos, validam listagens e formulários
4. O resultado é enviado como artefato (`selenium-dev`, `selenium-test`, `selenium-prod`) — `continue-on-error: true` para não bloquear o pipeline em falha de E2E

---

## Requisitos

- Java 21
- Maven 3.9+
- Docker 24+ com Compose v2
