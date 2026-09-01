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

Este é o esqueleto inicial do monorepo: estrutura de módulos e build files
Gradle. Nenhum código de aplicação foi gerado ainda — as próximas tasks
(Épico 1: `docker-compose.yml`, schema Avro, pipeline CI/CD) serão
desenvolvidas incrementalmente, cada uma em sua própria branch
`feature/nome-da-task` a partir de `develop`, seguindo GitFlow.

## Antes do primeiro build

- Confirmar a versão do `gradle-avro-plugin` (o projeto foi doado ao Apache
  Avro; verifique a versão mais recente antes de rodar o build).
- Confirmar a versão do `io.confluent:kafka-avro-serializer` compatível com
  a versão do Kafka que for usada no `docker-compose.yml`.
- Gerar o Gradle Wrapper localmente (`gradle wrapper --gradle-version <9.x>`)
  — não foi gerado aqui por falta de acesso de rede ao Gradle Services.
