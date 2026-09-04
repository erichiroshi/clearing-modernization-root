# Task 2.3 — Produtor de Eventos de Alta Performance (Kafka + Outbox)

## Objetivo

Publicar o `TradeExecutedEvent` no Kafka depois que um trade é validado —
resolvendo o problema clássico de EDA conhecido como "dual write": como
garantir que "o trade foi salvo no Postgres" e "o evento foi publicado no
Kafka" nunca fiquem inconsistentes entre si, já que são dois sistemas
diferentes que não compartilham uma transação nativa.

## O que foi feito

- **Transactional Outbox Pattern**: tabela `trade_events_outbox`
  (`V2__create_outbox_table.sql`), escrita **na mesma transação** do
  `ExecutarTradeUseCase` (Task 2.2).
- **`TradeEventPublisher`** (port de aplicação) / **`OutboxTradeEvent
  Publisher`** (implementação) — só grava o registro `PENDENTE`, não fala
  com o Kafka diretamente.
- **`OutboxPoller`** (`@Scheduled`, a cada 2s por padrão) — busca até 50
  eventos pendentes.
- **`OutboxEventProcessor`** (`@Transactional` por evento) — publica no
  Kafka via `KafkaTemplate`, marca `PUBLICADO` ou registra falha
  (`registrarFalha`, até 5 tentativas antes de virar `FALHA` terminal), e
  — se publicou com sucesso — chama `trade.liquidar()`.
- **`TradeEventCodec`** — converte entre `TradeEventPayload` (record
  interno), JSON (formato armazenado no outbox) e o objeto Avro
  `TradeExecutedEvent` (formato publicado, gerado na Task 1.2).

## Decisões e trade-offs

**Por que Outbox e não só publicar direto dentro da transação, ou só um
listener `AFTER_COMMIT`.** A ideia mais simples — publicar no Kafka dentro
da mesma transação do Postgres — não funciona porque Kafka não participa
de transações distribuídas com o Postgres (não tem 2PC entre os dois). A
segunda ideia mais simples — `@TransactionalEventListener(phase =
AFTER_COMMIT)` publicando direto — resolve "só publica se o Postgres
commitou", mas não resolve o inverso: se o Kafka estiver fora do ar bem
naquele momento, o evento se perde pra sempre, sem re-tentativa, porque
não existe registro persistido de que ele deveria ter sido enviado. O
Outbox Pattern resolve os dois lados: o registro do "preciso publicar
isso" é gravado na mesma transação ACID do trade (garantido), e a
publicação de fato é assíncrona, com retry, porque o registro persiste
independente do Kafka estar disponível ou não.

**Payload do outbox em JSON, não em bytes Avro já serializados.** Cheguei
a considerar serializar o Avro (via `KafkaAvroSerializer`) no momento de
gravar o outbox e guardar os bytes prontos — mas isso amarra o formato
armazenado no banco ao *wire format* específico do Confluent (que inclui
um cabeçalho com o ID do schema no Schema Registry). Se o schema evoluir
ou o registry mudar, registros antigos no outbox ficariam presos ao schema
antigo. Guardar um JSON simples (via Jackson) desacopla o outbox do
detalhe de serialização — o `TradeEventCodec.paraAvro()` remonta o objeto
Avro *na hora de publicar*, sempre com o serializer/schema atual. Bônus:
JSON é legível direto numa query SQL, o que ajuda a depurar.

**Transação curta por evento (`OutboxEventProcessor.processar`), não uma
transação única pro lote inteiro.** Processar os 50 eventos pendentes numa
única transação gigante significaria segurar uma conexão de banco aberta
durante várias chamadas de rede bloqueantes ao Kafka em sequência — ruim
pro pool de conexões e pro tempo de lock. Cada evento vira sua própria
transação curta.

**Trade-off aceito conscientemente: o envio ao Kafka ainda é bloqueante
dentro dessa transação curta (`.get(5, TimeUnit.SECONDS)`).** A versão
"mais correta" separaria o envio (fora de qualquer transação) da
atualização de status (uma transação rápida, só de `UPDATE`) — mas isso
introduz uma nova janela de inconsistência (e se o Kafka confirmar mas o
`UPDATE` de status falhar?) que exigiria idempotência mais cuidadosa no
lado do consumidor (Task 3.1/3.2) pra não processar o mesmo evento duas
vezes. Pro escopo e o propósito didático deste projeto, o ganho não
compensou a complexidade extra agora — fica anotado como próximo passo se
o projeto for tratado como "produção de verdade".

**Limite de 5 tentativas antes de `FALHA` terminal.** Sem isso, um evento
com problema permanente (ex: payload corrompido) ficaria sendo re-tentado
para sempre, a cada 2 segundos, indefinidamente. Depois de 5 falhas, o
evento vai pro estado `FALHA` e o poller para de tentar sozinho — em um
sistema real, isso dispararia um alerta pra intervenção manual.

**`liquidar()` só acontece depois da confirmação do Kafka, não no
`ExecutarTradeUseCase`.** Fecha o ciclo de vida definido na Task 2.1:
`PENDENTE → VALIDADO` acontece na Task 2.2 (validação + persistência);
`VALIDADO → LIQUIDADO` só acontece aqui, quando a publicação no Kafka é
confirmada — reflete o momento real em que a clearing pode dizer que a
operação está de fato concluída (o evento já está disponível para os
sistemas consumidores).

## Como funciona (fluxo)

```
ExecutarTradeUseCase.executar(...)          [transação A, Postgres]
  → (Task 2.2: valida, debita, reduz posição, salva trade)
  → tradeEventPublisher.registrarEventoPendente(trade)
    → grava outbox: status=PENDENTE, payload=JSON
  → COMMIT   (trade VALIDADO + outbox PENDENTE, atômico)

  [assíncrono, thread separada]

OutboxPoller (a cada 2s)
  → busca até 50 outbox PENDENTE, mais antigos primeiro
  → para cada um: OutboxEventProcessor.processar(evento)   [transação B]
      → monta TradeExecutedEvent (Avro) a partir do JSON
      → kafkaTemplate.send(topic, tradeId, evento).get(5s)
      → sucesso:
          outbox.marcarComoPublicado()
          trade.liquidar()  → trade VALIDADO -> LIQUIDADO, salva
      → falha:
          outbox.registrarFalha()  → tentativas++, ou FALHA se >= 5
```

## Pendências / próximos passos

- **Testes de integração ponta a ponta com Kafka real** (Testcontainers +
  container do Kafka/Schema Registry) ainda não foram escritos — os testes
  desta task cobrem o codec (JSON ↔ Avro) e as transições de estado do
  outbox isoladamente, mas não o fluxo completo publicando de fato num
  broker. Fica para a Task 2.4 (Testes Rigorosos).
- Sem idempotência explícita no consumidor (isso é do `trade-query-
  service`, Task 3.1/3.2) — se o outbox re-tentar um evento que na
  verdade já chegou ao Kafka (ex: o `send` teve sucesso mas o `.get()`
  deu timeout por lentidão de rede, não porque falhou de verdade), o
  mesmo `TradeExecutedEvent` pode ser publicado mais de uma vez. O
  consumidor precisa ser projetado para tratar isso (chave de
  idempotência = `tradeId`, que já é o mesmo em republicações).
- Sem alerta/observabilidade quando um evento vai para `FALHA` — hoje só
  fica visível numa query manual na tabela `trade_events_outbox`.
