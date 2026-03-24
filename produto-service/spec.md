# Especificação de Testes — produto-service

Rastreabilidade entre as regras de negócio do produto-service e os casos de teste implementados. Cobre validações de domínio, operações CRUD, geração de SKU, tabelas de decisão e propriedades verificadas com jqwik.

---

## Domínio — `Produto`

### Regras de validação

| Regra | Condição | Resultado |
|-------|----------|-----------|
| R1 | `nome` nulo ou em branco | 400 Bad Request |
| R2 | `preco` negativo | 400 Bad Request |
| R3 | `preco` nulo | 400 Bad Request |
| R4 | `categoria` fora do enum | 400 Bad Request |
| R5 | Dados válidos | 201 Created, SKU gerado automaticamente |
| R6 | ID inexistente em GET / PUT / DELETE | 404 Not Found |

### Enum `CategoriaProduto`

Valores válidos: `MONITORES`, `PERIFERICOS`, `ARMAZENAMENTO`, `COMPONENTES`, `AUDIO_VIDEO`, `GERAL`.

Switches sobre `CategoriaProduto` não devem ter `default` — novos valores forçam revisão explícita em tempo de compilação.

---

## Tabelas de decisão

### POST /api/v1/produtos

| nome válido | preco válido | categoria válida | Resultado |
|-------------|--------------|-----------------|-----------|
| ✓ | ✓ | ✓ | 201 Created + SKU gerado |
| ✗ | ✓ | ✓ | 400 Bad Request |
| ✓ | ✗ | ✓ | 400 Bad Request |
| ✓ | ✓ | ✗ | 400 Bad Request |
| ✗ | ✗ | ✗ | 400 Bad Request |

### PUT /api/v1/produtos/{id}

| ID existe | Dados válidos | Resultado |
|-----------|--------------|-----------|
| ✓ | ✓ | 200 OK + produto atualizado |
| ✓ | ✗ | 400 Bad Request |
| ✗ | ✓ | 404 Not Found |

### DELETE /api/v1/produtos/{id}

| ID existe | Produto sem estoque | Resultado |
|-----------|---------------------|-----------|
| ✓ | ✓ | 204 No Content |
| ✓ | ✗ | 422 — produto com estoque não pode ser removido |
| ✗ | — | 404 Not Found |

---

## Diagramas de causa-efeito

### Criação de produto

```
CAUSAS                                    EFEITOS
────────────────────────────────────      ───────────────────────────────────────
C1: nome nulo ou em branco       ───────► E1: 400 — "nome obrigatório"
C2: preco nulo                   ───────► E2: 400 — "preço obrigatório"
C3: preco < 0                    ───────► E3: 400 — "preço inválido"
C4: categoria inválida           ───────► E4: 400 — "categoria inválida"
C5: todos os campos válidos      ───────► E5: 201 Created + SKU gerado
```

| C1 | C2 | C3 | C4 | C5 | E1 | E2 | E3 | E4 | E5 |
|----|----|----|----|----|----|----|----|----|----|
| ✓  |    |    |    |    | ✓  |    |    |    |    |
|    | ✓  |    |    |    |    | ✓  |    |    |    |
|    |    | ✓  |    |    |    |    | ✓  |    |    |
|    |    |    | ✓  |    |    |    |    | ✓  |    |
|    |    |    |    | ✓  |    |    |    |    | ✓  |

### Busca por ID

```
CAUSAS                          EFEITOS
────────────────────────────    ────────────────────────────────
C1: ID existe          ────────► E1: 200 OK + corpo do produto
C2: ID não existe      ────────► E2: 404 Not Found
C3: ID malformado      ────────► E3: 400 Bad Request
```

---

## Análise de fronteiras

### SkuGenerator

| Entrada | Resultado |
|---------|-----------|
| Nome com acentos ou caracteres especiais | SKU normalizado (sem acentos, uppercase) |
| Nome em branco | Exceção antes de gerar SKU |
| Dois produtos com o mesmo nome | SKUs distintos (sufixo numérico único) |

---

## Propriedades (jqwik)

| Propriedade | Invariante |
|-------------|------------|
| P1 | Todo produto válido criado pode ser recuperado via GET pelo mesmo ID |
| P2 | Qualquer preço ≥ 0 com nome e categoria válidos → criação não lança exceção |
| P3 | SKUs gerados são únicos por execução do `SkuGenerator` |

---

## Testes negativos

| Cenário | Resultado esperado |
|---------|--------------------|
| Corpo da requisição vazio (`{}`) | 400 Bad Request |
| `Content-Type` ausente na requisição | 415 Unsupported Media Type |
| ID inexistente no GET | 404 com mensagem clara |
| Ajuste de estoque negativo | 422 |
| Remover produto com estoque > 0 | 422 |

---

## Cobertura

Configurada via JaCoCo no `pom.xml` com mínimo de 90% de linhas. O build falha se não atingir.

```bash
mvn verify -pl produto-service
# Relatório: produto-service/target/site/jacoco/index.html
```

| Camada | Alvo |
|--------|------|
| domain | ≥ 90% |
| service | ≥ 90% |
| controller | ≥ 90% |
| repository | ≥ 90% |
