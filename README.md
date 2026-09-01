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
- [ ] Task 1.2 — Contrato de Dados com Apache Avro
- [ ] Task 1.3 — Pipeline CI/CD com Análise de Qualidade

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

## Antes do primeiro build

- Confirmar a versão do `gradle-avro-plugin` (o projeto foi doado ao Apache
  Avro; verifique a versão mais recente antes de rodar o build).
- Confirmar a versão do `io.confluent:kafka-avro-serializer` compatível com
  a versão do Kafka que for usada no `docker-compose.yml`.
- Gerar o Gradle Wrapper localmente (`gradle wrapper --gradle-version <9.x>`)
  — não foi gerado aqui por falta de acesso de rede ao Gradle Services.
