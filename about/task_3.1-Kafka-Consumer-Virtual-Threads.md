# Task 3.1 — Consumidor Concorrente com Java 25

## Objetivo

Dar vida ao módulo `trade-query-service`: consumir `TradeExecutedEvent` do
tópico `market.trades.v1`, usando Virtual Threads (Java 25) para o
processamento concorrente das mensagens, com o encanamento pronto pra
receber a persistência real (Task 3.2).

## O que foi feito

- `TradeQueryServiceApplication` — main class (módulo só tinha `build.gradle`
  até agora)
- `TradeExecutedEventListener` — `@KafkaListener` no tópico, desserializa o
  Avro real (via Schema Registry), mapeia pro DTO interno
  `TradeProjecaoComando` e delega ao port `TradeProjectionHandler`
- `KafkaConsumerConfig` — customiza o container factory autoconfigurado
  pelo Spring Boot: ack manual (`MANUAL_IMMEDIATE`) + executor de Virtual
  Threads (`SimpleAsyncTaskExecutor.setVirtualThreads(true)`)
- `LoggingTradeProjectionHandler` — implementação stub (só loga), existe
  pra o Spring context ter um bean real do port até a Task 3.2 trocar por
  persistência MongoDB
- Testes: unitário (Mockito) do mapeamento e do comportamento de ack: e um
  teste de integração ponta a ponta publicando um evento Avro de verdade no
  Kafka e confirmando que o listener consome

## Decisões e trade-offs

**Por que um DTO interno (`TradeProjecaoComando`) em vez de passar o
`TradeExecutedEvent` (Avro) direto pro port.** Mesmo racional do
`TradeEventPayload` no lado da ingestão (Task 2.3): a camada de aplicação
não deveria depender do tipo gerado pelo Avro — se o formato de mensageria
mudar (schema evolution, ou até trocar Avro por outra coisa no futuro), só
o listener precisa mudar, não o port nem quem o implementa.

**`ConcurrentKafkaListenerContainerFactoryConfigurer` para customizar,
não recriar do zero a configuração do listener.** O Spring Boot já
autoconfigura um container factory inteiro a partir de
`spring.kafka.consumer.*` no `application.yml` — reescrever isso manualmente
duplicaria lógica e ficaria dessincronizado toda vez que uma property nova
fosse adicionada. O `Configurer` aplica tudo que o Boot já monta, e só
depois sobrepomos os dois pontos que interessam (ack mode, executor).

**Virtual Threads via `SimpleAsyncTaskExecutor`, não um `ExecutorService`
manual.** É o mecanismo suportado nativamente pelo Spring Framework
(6.1+) para isso — `setVirtualThreads(true)` troca o pool de platform
threads por Virtual Threads sem precisar gerenciar o ciclo de vida de um
executor customizado.

**Ack manual, não automático.** Com ack automático, o Spring Kafka
confirma o offset *antes* de saber se o processamento deu certo — uma
falha no meio do handler perderia a mensagem silenciosamente (nunca mais
seria reprocessada). Com `MANUAL_IMMEDIATE`, o `ack.acknowledge()` só é
chamado depois que `handler.projetar(...)` retorna sem lançar exceção; se
lançar, a exceção sobe, a mensagem não é confirmada, e o Spring Kafka a
redisponibiliza pro consumidor tentar de novo.

**Idempotência explicitamente NÃO tratada aqui — fica pra Task 3.2.** Essa
era a pendência anotada no `about/task_2.3`: como o outbox publica com
garantia *at-least-once* (pode reenviar o mesmo evento), o consumidor
final precisa ser idempotente. A forma mais simples de resolver isso é do
lado da persistência — usar o `tradeId` como `_id` do documento no
MongoDB, tornando um reprocessamento um upsert idempotente, não uma
duplicata. Como a Task 3.1 ainda não tem MongoDB (só um stub de log), não
faz sentido resolver idempotência aqui — o `LoggingTradeProjectionHandler`
de propósito NÃO é idempotente (logar duas vezes o mesmo evento não causa
dano nenhum), e isso é aceitável justamente porque é temporário.

**Handler stub em vez de deixar o bean do port sem implementação.** Sem
nenhum `@Component implements TradeProjectionHandler`, o Spring Boot falha
ao subir o contexto (`NoSuchBeanDefinitionException`) — não dá pra testar
nem rodar o serviço com o port "vazio". O stub deixa o fluxo real
(consumir → desserializar → mapear → chamar o handler → ack) testável de
ponta a ponta desde já, sem esperar a Task 3.2.

**Sem Dead Letter Topic.** Ficou fora do escopo — hoje uma mensagem que
falha repetidamente fica sendo redisponibilizada indefinidamente (retry
infinito via ack não confirmado), sem um lugar pra "estacionar" mensagens
problemáticas pra investigação manual. Vale de roadmap futuro, mas não foi
pedido nesta task.

## Como funciona (fluxo)

```
Kafka (market.trades.v1)
  → TradeExecutedEventListener.consumir(evento, ack)
      → paraComando(evento)               [Avro -> TradeProjecaoComando]
      → handler.projetar(comando)         [hoje: LoggingTradeProjectionHandler]
      → sucesso: ack.acknowledge()        [offset avança]
      → falha: exceção sobe, SEM ack      [mensagem será reprocessada]
```

O `virtualThreadsKafkaListenerContainerFactory` processa cada mensagem
numa Virtual Thread — sob carga, isso permite escalar o número de
mensagens em processamento concorrente sem o custo de memória de threads
de SO tradicionais, especialmente relevante se `handler.projetar(...)`
vier a fazer I/O bloqueante (uma escrita no MongoDB, por exemplo — Task
3.2).

## Correção durante a validação (via Claude Code, sessão local)

O teste `TradeQueryConsumerEndToEndIT` falhou na primeira execução real com
`SecurityException: Forbidden br.com.erichiroshi.clearing.contracts.event.
TradeExecutedEvent!` — o mesmo bloqueio de reflexão do Avro 1.12+
(CVE-2024-47561) que já tinha sido corrigido no `OutboxEventProcessor` do
`trade-ingestion-service` (Task 2.3). **Esse é um erro meu de não aplicar
uma correção já conhecida por analogia**: o consumidor desserializa a
mesma classe `TradeExecutedEvent`, então era previsível que bateria no
mesmo bloqueio — deveria ter entrado no `KafkaConsumerConfig` desde a
primeira entrega desta task, não só depois de falhar na prática.
Corrigido com o mesmo `System.setProperty("org.apache.avro.
SERIALIZABLE_PACKAGES", ...)`, agora num `static` initializer do
`KafkaConsumerConfig`.

Também trocado `KafkaContainer` (genérico) por `ConfluentKafkaContainer`
no teste — classe específica do Testcontainers para imagens
`confluentinc/cp-kafka`, mais correta que a genérica (desenhada pra imagem
vanilla do Apache Kafka) para esse caso. O `TradeIngestionEndToEndIT` da
Task 2.4 já usava essa mesma classe.

## Pendências / próximos passos

- Idempotência real (Task 3.2, via `_id` = `tradeId` no MongoDB)
- Dead Letter Topic / estratégia de reprocessamento manual pra mensagens
  com falha persistente
- `LoggingTradeProjectionHandler` precisa ser removido/substituído quando
  a Task 3.2 entregar a implementação real — hoje ele é o único bean do
  port, então basta o novo `@Component` substituir (ou os dois convivem
  temporariamente e o stub é removido depois)
