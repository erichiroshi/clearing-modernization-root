# Task 1.1 — Ambiente Docker de Alta Disponibilidade

## Objetivo

Ter um ambiente local reproduzível com tudo que o sistema de clearing
precisa pra rodar: banco transacional (Postgres), banco de leitura
(MongoDB) e um cluster de mensageria (Kafka) com Schema Registry — sem
depender de nada instalado na máquina além do Docker.

## O que foi feito

- `docker-compose.yml` na raiz do monorepo, subindo:
  - **PostgreSQL 17** — porta `5432`
  - **MongoDB 8** — porta `27017`
  - **Kafka em modo KRaft, 3 nós** (`kafka-1`, `kafka-2`, `kafka-3`), cada um
    combinando os papéis de *broker* e *controller*
  - **Confluent Schema Registry** — porta `8081`, apontando pro cluster Kafka

## Decisões e trade-offs

**KRaft em vez de Zookeeper.** O Kafka abandonou o Zookeeper como
dependência obrigatória a partir da série 3.x/4.x — rodar em modo KRaft
(Kafka Raft) significa um serviço a menos pra subir e gerenciar, e é o
caminho que o próprio projeto Kafka recomenda para novas instalações.

**3 nós em vez de 1.** O script de contexto original pedia um ambiente
"clusterizado" — um único broker não demonstra nada sobre replicação,
eleição de líder ou tolerância a falha, que são justamente os motivos de
uma clearing usar Kafka em vez de, por exemplo, uma fila simples. Com 3
nós, o fator de replicação `3` nos tópicos internos (`KAFKA_DEFAULT_
REPLICATION_FACTOR`) faz sentido e é testável de verdade (dá pra derrubar
um nó e ver o cluster continuar respondendo).

**`CLUSTER_ID` fixo no compose.** Modo KRaft exige que todos os nós
concordem sobre qual é o `CLUSTER_ID` do cluster — gerei um UUID válido uma
vez e fixei no arquivo, em vez de gerar dinamicamente a cada `up`. Isso
evita um erro comum de "cluster ID mismatch" quando os volumes já existem
de uma subida anterior mas o ID mudou.

**`KAFKA_AUTO_CREATE_TOPICS_ENABLE: false`.** Deliberado: tópicos devem ser
criados explicitamente (a caminho da Task 2.3, quando o `TradeExecutedEvent`
for publicado em `market.trades.v1`), não aparecer magicamente na primeira
vez que alguém publica nele por engano.

## Como funciona (fluxo)

```
docker compose up -d
```

Sobe os 6 containers (postgres, mongodb, kafka-1/2/3, schema-registry) na
rede `clearing-net`. O Schema Registry só sobe depois dos 3 brokers Kafka
estarem no ar (`depends_on`), porque ele precisa se conectar no cluster pra
armazenar o schema store.

## Pendências / próximos passos

- Não há `healthcheck` explícito nos containers do Kafka nem do Schema
  Registry (só no Postgres e MongoDB) — funciona porque o `depends_on`
  simples é suficiente pra ordem de subida, mas não garante que o Kafka já
  esteja *pronto* para receber conexões quando o Schema Registry tentar se
  conectar. Na prática o Schema Registry tem retry próprio e converge, mas
  seria mais robusto adicionar healthchecks reais.
- Sem persistência de configuração do Schema Registry entre subidas (só os
  dados do Kafka/Postgres/Mongo têm volume nomeado).
