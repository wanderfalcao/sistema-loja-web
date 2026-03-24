# sistema-loja-web

[![CI](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/ci.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/ci.yml)
[![Code Quality](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/code-quality.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/code-quality.yml)
[![Security](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/security.yml/badge.svg)](https://github.com/wanderfalcao/sistema-loja-web/actions/workflows/security.yml)

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

O `DataLoader` popula produtos e pedidos de exemplo automaticamente no perfil `dev`.

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

**ci.yml** tem dois jobs. O primeiro — *Testes e Cobertura* — compila o projeto, executa todos os testes unitários e de integração dos dois serviços (incluindo WireMock e jqwik), gera o relatório JaCoCo e publica os resultados no painel do Actions. Se algum teste falhar ou a cobertura ficar abaixo de 90%, o job fecha com erro. O segundo job — *Selenium E2E* — só começa após o primeiro passar. Executa os testes Selenium com `@SpringBootTest`, que sobem o contexto Spring completo com Testcontainers provisionando o banco automaticamente — sem necessidade de Docker externo no runner. Nenhum PR deve ser mergeado com esse workflow em vermelho.

**code-quality.yml** roda o Checkstyle com as regras do Google Style Guide. Qualquer violação de severidade `error` quebra o build. Isso garante que o estilo de código seja consistente independente de qual IDE cada desenvolvedor usa.

**security.yml** roda análise estática com GitHub CodeQL para detectar vulnerabilidades em Java (SQL injection, XSS, gerenciamento incorreto de recursos, etc.). Executa em todo push para master e semanalmente às segundas. Os resultados aparecem na aba **Security → Code scanning alerts** do repositório.

**sonarqube.yml** sobe um SonarQube Community Edition como container efêmero dentro do próprio job do GitHub Actions — sem precisar de servidor externo. Após o SonarQube inicializar, o workflow gera um token de análise, executa `mvn verify` nos dois serviços para gerar os relatórios JaCoCo e em seguida `mvn sonar:sonar` a partir da raiz do projeto para enviar a análise. O resultado do Quality Gate é exibido no log. Para ver o dashboard completo seria necessário um servidor SonarQube persistente; para uso local, veja a seção abaixo.

**deploy.yml** é acionado automaticamente quando o ci.yml passa com sucesso. Constrói as imagens Docker de cada serviço usando buildx e as publica no GitHub Container Registry (`ghcr.io`) com duas tags: o SHA do commit e `latest`. O deploy em `dev` e o deploy em `prod` ficam bloqueados por environments de proteção no GitHub — ambos exigem aprovação manual de `wanderfalcao` antes de prosseguir. O ambiente `prod` tem adicionalmente um wait timer de 5 minutos após a aprovação, dando uma janela de segurança para cancelar. Deploys só são permitidos a partir da branch `master`.

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

Nos TPs anteriores os dois serviços foram desenvolvidos de forma independente, sem um padrão compartilhado entre eles. A refatoração do TP4 teve três focos: organizar o código existente, conectar os dois serviços de forma consistente e preparar a esteira de CI/CD.

**Organização do código.** Cada serviço foi reestruturado em camadas bem definidas — domínio, serviço, controlador, repositório e configuração. A lógica de criação de objetos de domínio foi centralizada em fábricas dedicadas, eliminando construtores espalhados pelo código. O mapeamento entre entidades e DTOs passou a ser feito por geração de código em tempo de compilação (MapStruct), substituindo conversões manuais sujeitas a erros. A geração de SKU para produtos virou um utilitário independente, cobrível por testes unitários isolados.

**Reutilização entre serviços.** Uma interface genérica de operações CRUD foi extraída e passou a ser implementada nos dois serviços, garantindo contrato comum para listagem, busca e remoção. O tratamento de exceções foi centralizado em handlers separados por protocolo: um para requisições MVC (com redirecionamento e mensagem amigável) e outro para a API REST (seguindo RFC 7807).

**Integração.** A comunicação do pedido-service com o produto-service passou a ser feita via interface dedicada, substituindo chamadas HTTP diretas espalhadas pelo código. O cliente HTTP foi trocado por Apache HttpClient 5 — necessário para suporte ao método PATCH — com timeouts configuráveis (3s para conexão, 5s para leitura). Em ambiente Docker toda comunicação entre serviços passa pelo API Gateway, garantindo o roteamento centralizado e load balancing via Eureka.

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
