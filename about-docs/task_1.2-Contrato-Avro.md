# Task 1.2 — Contrato de Dados com Apache Avro

## Objetivo

Definir, de forma versionada e com geração de código, o contrato de
mensageria entre os dois microsserviços: o formato exato do evento que sai
do `trade-ingestion-service` e é consumido pelo `trade-query-service`.

## O que foi feito

- `clearing-contracts/src/main/avro/trade-executed.avsc` — schema Avro do
  `TradeExecutedEvent`, publicado no tópico `market.trades.v1`.
- Campos: `tradeId`, `buyerId`, `sellerId`, `assetSymbol`, `quantity`,
  `price`, `totalAmount` (os três últimos como logicalType `decimal`),
  `executedAt` (logicalType `timestamp-millis`), `traceId` (opcional, pra
  correlação com OpenTelemetry na Task 3.3).
- `clearing-contracts/build.gradle` — plugin `com.github.davidmc24.
  gradle.plugin.avro`, configurado com `enableDecimalLogicalType = true`,
  `stringType = "String"` e `fieldVisibility = "PRIVATE"`.

## Decisões e trade-offs

**Por que Avro (e não JSON puro) para o contrato entre os serviços.** Avro
dá schema evolution controlada (o Schema Registry recusa mudanças
incompatíveis no merge), payload binário mais compacto que JSON, e geração
de código Java tipada — o `trade-ingestion-service` e o `trade-query-
service` compartilham a mesma classe `TradeExecutedEvent` gerada a partir
do `.avsc`, então um erro de "esqueci de mapear um campo" vira erro de
compilação, não bug em produção.

**`decimal` para valores monetários, não `double`.** `double`/`float`
introduzem erro de arredondamento binário — inaceitável pra valores
financeiros. O logicalType `decimal` do Avro mapeia pra `java.math.
BigDecimal`, que é o tipo certo pra dinheiro. Isso exige habilitar
`enableDecimalLogicalType` explicitamente no plugin Gradle — não é o
padrão.

**`ticker` vs `assetSymbol`: mantido só `assetSymbol`.** Durante a
implementação, cheguei a adicionar um campo `ticker` que duplicava
`assetSymbol` (mesma informação, nome diferente). Decisão: manter só
`assetSymbol`, que já cobria o caso de uso (código do ativo, ex: PETR4).

**`traceId` como campo opcional (union `["null", "string"]`) desde já.**
Ele só vai ser preenchido de fato na Task 3.3 (OpenTelemetry), mas
adicionar o campo agora, opcional, evita ter que fazer uma migração de
schema depois — schema evolution é mais simples quando você adiciona um
campo novo opcional do que quando descobre que precisa de um campo que não
existia.

## Como funciona (fluxo)

O plugin `gradle-avro-plugin` lê qualquer `.avsc` em `src/main/avro`
durante `compileJava` e gera a classe Java correspondente em
`build/generated-main-avro-java` — automaticamente, sem passo manual. Essa
classe gerada (`TradeExecutedEvent`, com um builder fluente via
`newBuilder()`) é o que os dois microsserviços importam de
`clearing-contracts` (Task 2.3 usa isso pra montar o evento antes de
publicar no Kafka).

## Pendências / próximos passos

- Nenhum "Ativo" como entidade própria no schema — `assetSymbol` +
  implicitamente o nome do ativo (adicionado depois, na Task 2.3, como
  `ativo_nome` na camada de persistência) cobrem o que foi pedido até
  agora. Se o domínio crescer pra precisar de mais metadados do ativo
  (setor, moeda, mercado), isso vira um schema Avro próprio.
