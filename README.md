# Clearing Modernization

Monorepo multi-módulo (Gradle, Groovy DSL) para a modernização do fluxo de
Clearing de produtos financeiros, usando Arquitetura Orientada a Eventos
(EDA) com CQRS.

## Módulos

| Módulo                     | Responsabilidade                                                                 | Depende de                                  |
|-----------------------------|-----------------------------------------------------------------------------------|----------------------------------------------|
| `clearing-contracts`        | Schemas Avro (`.avsc`) e classes Java geradas — contrato de mensageria do Kafka  | —                                             |
| `clearing-domain`           | Domínio puro (DDD/Clean Architecture): entidades, regras, Use Cases             | —                                             |
| `trade-ingestion-service`   | Escrita — valida trade, persiste em PostgreSQL (ACID), publica no Kafka         | `clearing-domain`, `clearing-contracts`      |
| `trade-query-service`       | Leitura — consome Kafka (Virtual Threads), projeta em MongoDB (CQRS)            | `clearing-contracts`                          |

## Stack

- Java 25 (Virtual Threads)
- Spring Boot 4
- PostgreSQL (escrita transacional)
- MongoDB (leitura otimizada)
- Apache Kafka (KRaft mode) + Confluent Schema Registry
- Apache Avro (contrato de eventos)
- OpenTelemetry (rastreamento distribuído entre os dois microsserviços)

## Estado atual

Estrutura de módulos e build files Gradle prontos. Ambiente de
desenvolvimento local disponível via `docker-compose.yml` (Task 1.1).
As demais tasks do Épico 1 (schema Avro, pipeline CI/CD) e dos épicos
seguintes serão desenvolvidas incrementalmente, cada uma em sua própria
branch `feature/nome-da-task` a partir de `develop`, seguindo GitFlow.

### Épico 1 — Infraestrutura Automatizada e Contratos de Dados

- [x] **Task 1.1** — Ambiente Docker de Alta Disponibilidade
- [x] **Task 1.2** — Contrato de Dados com Apache Avro
- [x] **Task 1.3** — Pipeline CI/CD com Análise de Qualidade
## Ambiente local (Task 1.1)

`docker-compose.yml` sobe:

- **PostgreSQL 17** — porta `5432` (db/user/senha: `clearing`)
- **MongoDB 8** — porta `27017` (usuário root: `clearing`/`clearing`)
- **Kafka em modo KRaft, 3 nós** (`kafka-1`, `kafka-2`, `kafka-3`, roles
  broker+controller combinados) — portas `9092`, `9093`, `9094` no host
- **Confluent Schema Registry** — porta `8081`

```bash
docker compose up -d
docker compose ps
```

O `CLUSTER_ID` está fixo no compose (necessário ser idêntico entre os
3 nós em modo KRaft). Fator de replicação padrão configurado para `3`
nos tópicos internos, condizente com os 3 brokers do cluster.

## Contrato de dados (Task 1.2)

O evento `TradeExecutedEvent` (`clearing-contracts/src/main/avro/trade-executed.avsc`)
é o contrato publicado no tópico `market.trades.v1` pelo `trade-ingestion-service`
e consumido pelo `trade-query-service`. Campos monetários (`quantity`, `price`,
`totalAmount`) usam o logicalType `decimal` (mapeado para `BigDecimal`), e
`executedAt` usa `timestamp-millis`.

A classe Java `TradeExecutedEvent` é gerada automaticamente a partir do
`.avsc` durante `compileJava` (plugin `com.github.davidmc24.gradle.plugin.avro`),
em `clearing-contracts/build/generated-main-avro-java` — não deve ser
versionada nem editada manualmente.

## CI/CD (Task 1.3)

Pipeline em `.github/workflows/ci.yml`: builda todos os módulos, roda os
testes com cobertura (Jacoco) e envia a análise pro SonarCloud
(organização `erichiroshi`). O merge do PR fica bloqueado se o Quality
Gate falhar — cobertura insuficiente ou bugs/code smells acima do
threshold.

Pré-requisitos no GitHub: secret `SONAR_TOKEN` configurado e branch
protection na `develop`/`main` exigindo o check `Build, testes e
análise SonarCloud` passando antes do merge.

## Antes do primeiro build

- Confirmar a versão do `gradle-avro-plugin` (o projeto foi doado ao Apache
  Avro; verifique a versão mais recente antes de rodar o build).
- Confirmar a versão do `io.confluent:kafka-avro-serializer` compatível com
  a versão do Kafka que for usada no `docker-compose.yml`.
- Gerar o Gradle Wrapper localmente (`gradle wrapper --gradle-version <9.x>`)
  — não foi gerado aqui por falta de acesso de rede ao Gradle Services.
