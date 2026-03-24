# Especificação de Testes — pedido-service

Rastreabilidade entre as regras de negócio do pedido-service e os casos de teste implementados. Cobre a máquina de estados de `StatusPedido`, integração com o produto-service, tabelas de decisão, diagramas de causa-efeito e propriedades verificadas com jqwik.

---

## Máquina de estados — `StatusPedido`

Os estados refletem o fluxo operacional: criação, confirmação de pagamento, separação, envio, entrega e cancelamento. Pedidos ENVIADOS e ENTREGUES não podem ser cancelados.

| Transição | Válida? |
|-----------|---------|
| `PENDENTE` → `PROCESSANDO` | ✓ |
| `PENDENTE` → `CANCELADO` | ✓ |
| `PROCESSANDO` → `CONCLUIDO` | ✓ |
| `PROCESSANDO` → `CANCELADO` | ✓ |
| `CONCLUIDO` → `CONTESTADO` | ✓ |
| `CONTESTADO` → `PROCESSANDO` | ✓ |
| `CONTESTADO` → `CANCELADO` | ✓ |
| `CONCLUIDO` → qualquer (exceto CONTESTADO) | ✗ — 422 |
| `CANCELADO` → qualquer | ✗ — 422 |
| Salto de estado | ✗ — 422 |

> Switch sobre `StatusPedido` sem `default` — novos estados forçam tratamento explícito em tempo de compilação.

---

## Tabelas de decisão

### POST /api/v1/pedidos

| Itens não vazio | produto-service disponível | quantidade > 0 | Resultado |
|-----------------|---------------------------|----------------|-----------|
| ✓ | ✓ | ✓ | 201 — status = PENDENTE |
| ✗ | — | — | 400 — itens obrigatórios |
| ✓ | ✗ | ✓ | 502 — produto-service indisponível |
| ✓ | ✓ | ✗ | 400 — quantidade inválida |

### PATCH /api/v1/pedidos/{id}/avancar

| ID existe | Transição válida | Resultado |
|-----------|-----------------|-----------|
| ✓ | ✓ | 200 + novo status |
| ✓ | ✗ | 422 |
| ✗ | — | 404 |

### Integração com produto-service

| produto-service disponível | produto existe | Resultado |
|----------------------------|----------------|-----------|
| ✓ | ✓ | Pedido criado, estoque debitado na transição PENDENTE → PROCESSANDO |
| ✓ | ✗ | 404 repassado ao cliente |
| ✗ | — | 502 Bad Gateway |

---

## Diagramas de causa-efeito

### Criação de pedido

```
CAUSAS                                          EFEITOS
────────────────────────────────────────────    ────────────────────────────────────────────
C1: itens vazio/nulo                ──────────► E1: 400 — "itens obrigatórios"
C2: quantidade de item ≤ 0          ──────────► E2: 400 — "quantidade inválida"
C3: produto-service fora do ar      ──────────► E3: 502 Bad Gateway
C4: produto não existe no catálogo  ──────────► E4: 404 Not Found
C5: todos os campos válidos         ──────────► E5: 201 Created, status = PENDENTE
```

C1 e C2 são verificados localmente antes de acionar o produto-service.

| C1 | C2 | C3 | C4 | C5 | E1 | E2 | E3 | E4 | E5 |
|----|----|----|----|----|----|----|----|----|----|
| ✓  |    |    |    |    | ✓  |    |    |    |    |
|    | ✓  |    |    |    |    | ✓  |    |    |    |
|    |    | ✓  |    |    |    |    | ✓  |    |    |
|    |    |    | ✓  |    |    |    |    | ✓  |    |
|    |    |    |    | ✓  |    |    |    |    | ✓  |

### Avanço de status

```
CAUSAS                               EFEITOS
────────────────────────────────     ──────────────────────────────────────
C1: ID não existe         ─────────► E1: 404 Not Found
C2: status = CANCELADO    ─────────► E2: 422 — "pedido cancelado"
C3: status = CONCLUIDO    ─────────► E3: 200 OK → CONTESTADO (única transição)
C4: transição válida      ─────────► E4: 200 OK → próximo status
```

### Falha de infraestrutura

```
CAUSAS                                       EFEITOS
──────────────────────────────────────────   ─────────────────────────────────────────────
C1: timeout ao chamar produto-service ──────► E1: 502 Bad Gateway + mensagem de erro
C2: connection refused                ──────► E1: 502 Bad Gateway + mensagem de erro
C3: produto-service retorna 404       ──────► E2: 404 repassado com contexto do produto
C4: produto-service retorna 200       ──────► E3: pedido processado normalmente
```

---

## Análise de fronteiras

### StatusHistorico — audit trail

| Cenário | Registro esperado |
|---------|-------------------|
| Pedido criado | 1 entrada (PENDENTE) |
| Avanço bem-sucedido | +1 entrada por transição |
| Tentativa inválida | Nenhuma entrada adicionada |

---

## Propriedades (jqwik)

| Propriedade | Invariante |
|-------------|------------|
| P1 | Toda sequência válida de avanços termina em CONCLUIDO ou CANCELADO |
| P2 | Todo pedido criado tem pelo menos 1 entrada no histórico |
| P3 | Transição inválida não altera o status atual |
| P4 | Quantidade > 0 e produto válido → pedido é criável |

---

## Testes negativos

| Cenário | Resultado esperado |
|---------|--------------------|
| produto-service simulado com timeout (WireMock) | 502 com mensagem clara |
| Cancelar pedido CONCLUIDO | 422 |
| Avançar pedido CANCELADO | 422 |
| Pedido com lista de itens vazia | 400 |
| ID não UUID no path | 400 |

---

## Cobertura

Configurada via JaCoCo no `pom.xml` com mínimo de 90% de linhas. O build falha se não atingir.

```bash
mvn verify -pl pedido-service
# Relatório: pedido-service/target/site/jacoco/index.html
```

| Camada | Alvo |
|--------|------|
| domain | ≥ 90% |
| service | ≥ 90% |
| controller | ≥ 90% |
| repository | ≥ 90% |
| client (excluído do JaCoCo) | — |
