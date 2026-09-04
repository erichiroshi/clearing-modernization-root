# Task 2.4 — Testes Rigorosos (Padrão AAA)

## Objetivo

Fechar as lacunas de teste deixadas pelas tasks anteriores: cobertura com
Mockito (explicitamente pedida, e ainda não usada no `trade-ingestion-
service`) e o teste de integração ponta a ponta com Kafka real que ficou
anotado como pendência no `about/task_2.3`.

## O que foi feito

- **`ExecutarTradeUseCaseTest`** (Mockito) — 6 cenários: caminho feliz,
  comprador inexistente, vendedor inexistente, saldo insuficiente, posição
  insuficiente, e uma verificação explícita de que o evento só é
  registrado com o `Trade` *já persistido* (não uma instância anterior).
- **`OutboxEventProcessorTest`** (Mockito) — sucesso (marca `PUBLICADO` +
  liquida o trade), falha isolada (mantém `PENDENTE`, incrementa
  tentativa), e o caminho até `FALHA` terminal depois de 5 tentativas.
- **`MappersTest`** — round-trip domínio → entidade → domínio para os três
  mappers, sem precisar de banco.
- **`TradeIngestionEndToEndIT`** — Postgres + Kafka + Schema Registry reais
  via Testcontainers, com o `OutboxPoller` rodando de verdade (scheduling
  ligado, ao contrário do `ExecutarTradeUseCaseIT` da Task 2.2). Confirma
  que a mensagem chega no tópico em Avro de verdade e que o trade avança
  para `LIQUIDADO`.

## Decisões e trade-offs

**Por que Mockito nos testes do use case e do processor, mas não nos
testes do domínio (Task 2.1).** O `clearing-domain` não tem nenhuma
dependência externa pra mockar — `Comprador`, `Vendedor` e `Trade` só
colaboram entre si, então os testes de domínio usam os objetos reais.
Já `ExecutarTradeUseCase` e `OutboxEventProcessor` dependem de portas
(`CompradorRepository`, `TradeEventPublisher`, `KafkaTemplate`) cuja
implementação real (banco, Kafka) é lenta e não é o que se quer testar
nesse nível — mockar as dependências deixa esses testes rodando em
milissegundos e focados só na lógica de orquestração.

**`mock(TradeExecutedEvent.class)` em vez de construir um evento Avro real
nos testes do `OutboxEventProcessor`.** O `OutboxEventProcessor` não olha
pra dentro do evento Avro — ele só recebe o que `codec.paraAvro()` (que
está mockado) devolve e repassa pro `KafkaTemplate`. Não há necessidade de
um objeto Avro válido de verdade nesses testes; usar um mock evita
depender dos detalhes de tipo exato dos campos Avro (o `executedAt` pode
ser `Instant` ou `Long` dependendo de como o plugin gerou a classe — ver
nota na Task 2.3) — o `TradeIngestionEndToEndIT` é quem valida isso de
verdade, com o objeto real.

**Teste ponta a ponta com Kafka real, não só mockado.** Testes com Mockito
provam que a lógica de orquestração está certa *supondo* que o
`KafkaTemplate`/Schema Registry funcionam como esperado — mas não provam
que a configuração real (serializer, URL do registry, formato do tópico)
está certa. O `TradeIngestionEndToEndIT` sobe os três componentes de
verdade (Postgres, Kafka, Schema Registry) via Testcontainers e um
`KafkaConsumer` manual (com `KafkaAvroDeserializer`) — é o único teste do
projeto que exercita o caminho de produção completo, do use case até uma
mensagem de verdade no tópico.

**Rede Docker customizada (`Network.newNetwork()`) para os containers de
teste.** O Schema Registry precisa falar com o Kafka pelo endereço interno
da rede Docker (`kafka:9092`), não pelo endereço mapeado no host que o
`KafkaContainer` expõe para o processo de teste — por isso os dois
containers compartilham uma rede nomeada com alias explícito
(`withNetworkAliases("kafka")`).

**Risco assumido conscientemente: este é o teste de maior chance de
precisar de ajuste manual.** Não consegui rodar Docker-in-Docker neste
ambiente (nem baixar dependências do Maven Central), então
`TradeIngestionEndToEndIT` foi escrito com cuidado mas nunca executado de
fato. Deixei comentários no próprio arquivo apontando os dois pontos mais
prováveis de quebrar (alias de rede do Kafka, timeout de inicialização do
Schema Registry) para acelerar o debug caso ele falhe na primeira
tentativa.

## Como funciona (fluxo do `TradeIngestionEndToEndIT`)

```
@BeforeEach: limpa as tabelas, sobe um KafkaConsumer<String, TradeExecutedEvent>
             manual (KafkaAvroDeserializer), inscrito em market.trades.v1

executarTradeUseCase.executar(...)
  → (fluxo real das Tasks 2.2/2.3: valida, persiste, grava outbox)

await() [Awaitility, até 20s]:
  → consumidorDeTeste.poll() até achar a mensagem publicada pelo
    OutboxPoller real (rodando a cada 500ms neste teste)
  → confere key = tradeId, value.tradeId, value.assetSymbol

await() [Awaitility, até 20s]:
  → confere que o TradeEntity no Postgres já está LIQUIDADO
    (confirmado pelo OutboxEventProcessor depois do envio ao Kafka)
```

## Pendências / próximos passos

- Sem teste cobrindo o cenário "Kafka fica indisponível no meio do
  processamento" de ponta a ponta (só coberto no nível de unidade, com
  Mockito, no `OutboxEventProcessorTest`) — simular isso com Testcontainers
  exigiria derrubar o container do Kafka no meio do teste, o que é possível
  mas não foi priorizado aqui.
- SonarQube local (do `docker-compose.yml`) ainda não foi usado de fato
  para validar as métricas desta task — rodar
  `./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token gerado na UI local>`
  com a stack local no ar é o próximo passo manual, fora do que dá pra
  automatizar aqui.
