# Task 2.2 — Persistência Corretiva (PostgreSQL) + Flyway

## Objetivo

Persistir os agregados de domínio (`Comprador`, `Vendedor`, `Trade`) no
PostgreSQL, com controle transacional adequado para cenários concorrentes
— sem vazar detalhes de JPA/Hibernate pro `clearing-domain`.

## O que foi feito

Módulo `trade-ingestion-service`, pacote
`br.com.erichiroshi.clearing.ingestion`:

- **Migrations Flyway** (`V1__create_tables.sql`): tabelas `compradores`,
  `vendedores`, `vendedor_posicoes` (filha, pro `@ElementCollection`) e
  `trades`, com `CHECK` constraints e índices.
- **Entidades JPA** (`CompradorEntity`, `VendedorEntity`, `TradeEntity`) —
  isoladas em `infrastructure.persistence.entity`, nunca importadas pelo
  domínio.
- **Mappers** (`CompradorMapper`, `VendedorMapper`, `TradeMapper`) —
  conversão explícita domínio ↔ entidade, incluindo um método
  `atualizarEntidade(...)` pra sincronizar uma entidade JPA já gerenciada
  com o estado atual do objeto de domínio depois de uma mutação.
- **Adapters** (`CompradorRepositoryAdapter`, etc.) — implementam os ports
  definidos na Task 2.1.
- **`ExecutarTradeUseCase`** — orquestra tudo numa única `@Transactional`.
- Teste de integração com Testcontainers (Postgres real).

## Decisões e trade-offs

**Flyway em vez de `ddl-auto: update`.** A ideia original do plano era
deixar o Hibernate gerar o schema automaticamente — mais rápido de
prototipar, mas não é como um projeto profissional versiona banco de
dados: sem migrations, não tem como saber *quando* uma coluna foi
adicionada, não dá pra revisar mudança de schema em PR, e "funciona na
minha máquina" vira comum quando o schema diverge entre ambientes. Trocado
para Flyway a pedido explícito, com `ddl-auto: validate` — o Hibernate
passa a só *conferir* que o mapeamento das entidades bate com o schema
real, nunca cria nada sozinho.

**Lock pessimista (`SELECT ... FOR UPDATE`), não lock otimista
(`@Version`).** Numa clearing, dois trades tentando debitar o mesmo
Comprador ou reduzir a posição do mesmo Vendedor ao mesmo tempo precisam
ser *serializados* de verdade, não apenas detectados em conflito depois do
fato. Lock otimista devolveria uma exceção de conflito de versão pro
segundo trade — que exigiria uma lógica de retry pra ser útil (senão o
cliente só recebe um erro numa situação de concorrência normal, que não é
um erro de negócio). Isso seria escopo extra não pedido. Lock pessimista
resolve com uma linha (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) e sem
precisar de retry.

**Mappers com método `atualizarEntidade`, não só `paraEntidade`.** Quando
um `Comprador`/`Vendedor` já existe no banco e é mutado (debitado/
reduzido) dentro da transação, o adapter não cria uma entidade nova — ele
localiza a entidade já gerenciada pelo Hibernate (`findById` dentro da
mesma transação) e atualiza os campos nela. Isso deixa o *dirty checking*
do Hibernate funcionar da forma esperada (o `UPDATE` sai automaticamente
no commit) e evita duplicar linhas ou perder a instância gerenciada.

**Reaproveitar o enum `StatusTrade` do domínio direto no `@Enumerated` da
entidade.** Simplificação consciente: em um sistema maior, a camada de
infra normalmente teria seu próprio enum de persistência, pra não acoplar
o schema do banco ao vocabulário interno do domínio (se o domínio
renomear um estado, a migração de banco vira um problema separado). Para o
escopo didático deste projeto, o acoplamento foi aceito — é um trade-off
que vale a pena nomear caso o projeto cresça.

**`TradeEntity.comprador`/`.vendedor` como `@ManyToOne(FetchType.LAZY)`,
carregados via `getReferenceById` no `TradeRepositoryAdapter`.** Evita um
segundo `SELECT` desnecessário: o comprador e o vendedor já foram
carregados (e travados) pelo `ExecutarTradeUseCase` antes do `Trade` ser
criado, então basta pedir ao Hibernate uma *referência* gerenciada pra
satisfazer a foreign key, sem ir ao banco de novo.

## Como funciona (fluxo)

```
ExecutarTradeUseCase.executar(...)   [@Transactional]
  → compradorRepository.buscarPorId()   → SELECT ... FOR UPDATE (trava a linha)
  → vendedorRepository.buscarPorId()    → SELECT ... FOR UPDATE (trava a linha)
  → Trade.registrar(...) + trade.validar()   [domínio puro, em memória]
  → compradorRepository.salvar()   → UPDATE (dirty checking)
  → vendedorRepository.salvar()    → UPDATE (dirty checking)
  → tradeRepository.salvar()       → INSERT
  → COMMIT (libera os locks)
```

Se `trade.validar()` lançar `SaldoInsuficienteException` ou
`PosicaoInsuficienteException` — que são `RuntimeException` — o Spring
faz rollback automático do zero: nada é persistido, mesmo que o comprador
já tenha sido carregado.

## Pendências / próximos passos

- Sem paginação/timeout configurado explicitamente no lock pessimista — em
  alta concorrência real, um `SELECT FOR UPDATE` sem timeout pode segurar
  outras transações esperando indefinidamente. Vale revisar
  `hibernate.query.timeout` ou `SELECT ... FOR UPDATE NOWAIT`/`SKIP
  LOCKED` se isso virar um projeto sob carga real.
- Não há endpoint HTTP (`POST /trade`) ainda — o `ExecutarTradeUseCase` é
  chamado apenas pelos testes de integração. Adicionar o controller REST
  não fazia parte do escopo explícito da Task 2.2.
