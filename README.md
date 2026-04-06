# sistema-loja-web

[![CI](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/ci.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/ci.yml)
[![Code Quality](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/code-quality.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/code-quality.yml)
[![Security](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/security.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/security.yml)
[![Deploy](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/deploy.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/deploy.yml)

Sistema de loja construído com microsserviços Spring Boot. O `produto-service` gerencia o catálogo e o estoque; o `pedido-service` controla o ciclo de vida dos pedidos e chama o produto-service para ajustar estoque nas transições de status. Tudo passa pelo `api-gateway`, que roteia as requisições e serve o Swagger unificado dos dois serviços.

## Arquitetura

```
                      ┌───────────────────┐
                      │    api-gateway    │  :8080
                      │  Spring Cloud GW  │  /swagger-ui.html
                      └─────────┬─────────┘
              lb://              │               lb://
    ┌─────────────────────┬──────┴──────┬─────────────────────┐
    ▼                                                         ▼
┌───────────────┐                               ┌───────────────────┐
│produto-service│  :8081                        │  pedido-service   │  :8082
│ produtos, SKU │◄────── REST (via gateway) ────│  pedidos, status  │
│ estoque, promo│                               │  audit trail      │
└───────┬───────┘                               └────────┬──────────┘
        │                                                │
   produto-db :5433                               pedido-db :5434

         Eureka Server :8761  —  service discovery
         SonarQube     :9000  —  qualidade de código (local)
```

## Como subir

```bash
docker-compose up --build
```

Aguarda todos os health checks (~2 min na primeira vez). Ao finalizar, o terminal exibe os endereços:

| Serviço     | URL                                       |
|-------------|-------------------------------------------|
| API Gateway | http://localhost:8080                     |
| Pedidos     | http://localhost:8080/pedidos             |
| Produtos    | http://localhost:8080/produtos            |
| Swagger UI  | http://localhost:8080/swagger-ui.html     |
| Eureka      | http://localhost:8761                     |
| SonarQube   | http://localhost:9000  (admin / admin)    |
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

| Método | Caminho                              | Descrição                     |
|--------|--------------------------------------|-------------------------------|
| GET    | /api/v1/produtos                     | Lista produtos paginado        |
| POST   | /api/v1/produtos                     | Cadastra produto               |
| GET    | /api/v1/produtos/{id}                | Busca por ID                   |
| PUT    | /api/v1/produtos/{id}                | Atualiza produto               |
| PATCH  | /api/v1/produtos/{id}/estoque        | Ajusta estoque (ENTRADA/SAIDA) |
| GET    | /api/v1/pedidos                      | Lista pedidos paginado         |
| POST   | /api/v1/pedidos                      | Cria pedido                    |
| GET    | /api/v1/pedidos/{id}                 | Busca por ID                   |
| PATCH  | /api/v1/pedidos/{id}/avancar         | Avança status                  |
| PATCH  | /api/v1/pedidos/{id}/cancelar        | Cancela pedido                 |

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

Os testes Selenium usam `@Tag("selenium")` e ficam fora do ciclo padrão. Para rodá-los é preciso ter um servidor subindo e Chrome instalado:

```bash
mvn test -pl pedido-service -Dgroups=selenium
mvn test -pl produto-service -Dgroups=selenium
```

## Workflows GitHub Actions

Os arquivos ficam em `.github/workflows/`. Para verificar execuções passadas: aba **Actions** do repositório. Para disparar manualmente qualquer workflow: **Actions → selecione o workflow → Run workflow**.

**ci.yml** roda em dois jobs. O primeiro compila e testa os dois serviços — unitários, WireMock e jqwik — e exige cobertura mínima de 90% no JaCoCo para passar. O segundo roda os Selenium só depois do primeiro verde, usando Testcontainers para subir o banco dentro do próprio runner. PR com esse workflow vermelho não entra.

**code-quality.yml** roda Checkstyle no estilo Google. Violação de severidade `error` quebra o build.

**security.yml** roda CodeQL para análise estática de segurança em Java. Executa em todo push para master e toda segunda-feira. Resultados ficam em **Security → Code scanning alerts**.

**sonarqube.yml** sobe um SonarQube Community Edition temporário direto no runner do Actions, sem precisar de servidor dedicado. Roda o `mvn sonar:sonar` nos dois serviços e exibe o resultado do Quality Gate no log. Para rodar local veja a seção abaixo.

**deploy.yml** dispara quando o ci.yml passa. Autentica no GCP via OIDC — sem chave JSON, só um JWT que o GCP valida pelo Workload Identity Federation. Publica as imagens no Artifact Registry e faz deploy no Cloud Run em três etapas: `dev` → `test` → `prod`. O `prod` trava até alguém aprovar manualmente no GitHub Environments.

## Banco de dados em produção

Localmente os bancos sobem via Docker Compose (`produto-db :5433` e `pedido-db :5434`). No Cloud Run cada serviço usa uma instância separada no [Neon.tech](https://neon.tech) — PostgreSQL gerenciado com free tier.

| Ambiente        | Banco                          |
|-----------------|--------------------------------|
| local / dev     | Docker Compose (PostgreSQL 16) |
| Cloud Run       | Neon.tech PostgreSQL           |

A connection string fica nos secrets `PRODUTO_DB_URL` e `PEDIDO_DB_URL` no formato `jdbc:postgresql://...?sslmode=require`.

## Análise de qualidade com SonarQube

Para rodar localmente:

```bash
# Sobe o SonarQube junto com o restante da stack
docker-compose up sonarqube -d

# Aguarda o health check ficar UP, depois:
mvn verify -pl produto-service,pedido-service

mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin \
  -Dsonar.projectKey=sistema-loja-web
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
|---------------|-----------------|
| MONITORES     | 30%             |
| PERIFERICOS   | 40%             |
| ARMAZENAMENTO | 25%             |
| COMPONENTES   | 20%             |
| AUDIO_VIDEO   | 35%             |
| GERAL         | 50%             |

Quando `ativarPromocao()` é chamado, a validação do desconto usa o método da categoria — sem nenhum switch ou if.

### Regras de negócio adicionadas

- Promoção com datas precisa durar pelo menos 1 hora (`Promocao.criar`).
- Um pedido aceita no máximo 20 itens (`PedidoService.adicionarItem`).

### CI/CD

Os cinco workflows publicam resumo no painel do Actions via `$GITHUB_STEP_SUMMARY`. O deploy usa OIDC com GCP Workload Identity Federation — sem chave JSON nos secrets, o GitHub emite um token temporário que o GCP valida. Os serviços rodam no Cloud Run e o ambiente `prod` exige aprovação manual antes do deploy.

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

| De | Para | Observação |
|----|------|------------|
| PENDENTE | PROCESSANDO | Debita estoque |
| PENDENTE | CANCELADO | Sem impacto em estoque |
| PROCESSANDO | CONCLUIDO | — |
| PROCESSANDO | CANCELADO | Devolve estoque |
| CONCLUIDO | CONTESTADO | Abre contestação |
| CONTESTADO | PROCESSANDO | Reinicia processamento |
| CONTESTADO | CANCELADO | — |

---

## Requisitos

- Java 21
- Maven 3.9+
- Docker 24+ com Compose v2
