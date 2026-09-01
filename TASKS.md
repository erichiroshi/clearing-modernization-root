Épico 1: Infraestrutura Local e Contratos (Skeletons)
* Task 1.1 [Infra]: Criar o arquivo docker-compose.yml base com PostgreSQL, MongoDB, Apache Kafka e Schema Registry (Confluent).
* Task 1.2 [Contratos]: Criar o arquivo .avsc (Schema Avro) do TradeExecutedEvent e configurar o plugin do Gradle para autogerar as classes Java a partir do schema.
* Task 1.3 [CI/CD]: Configurar o pipeline inicial do GitHub Actions integrando com o SonarQube Cloud para análise estática.

Épico 2: Microsserviço de Ingestão (Trade Ingestion - Postgres)
* Task 2.1 [DDD/Domain]: Implementar entidades de domínio (Comprador, Vendedor, Conta, Limites de Garantia) usando POO pura, sem frameworks nas camadas internas.
* Task 2.2 [Persistence]: Implementar adaptadores do Spring Data JPA (PostgreSQL) e regras de validação transacional (ACID).
* Task 2.3 [Kafka Producer]: Implementar o adaptador de envio do Kafka utilizando as classes geradas pelo Avro.
* Task 2.4 [Tests]: Escrever testes unitários com JUnit 5 e Mockito seguindo o Padrão AAA (Cenários de sucesso e Exceções como: Saldo Insuficiente, Garantia Rejeitada). Cobertura mínima de 85% cobrada no Sonar local.

Épico 3: Microsserviço de Agregação e Consulta (Trade Query - MongoDB)
* Task 3.1 [Kafka Consumer]: Configurar o listener do Kafka com suporte a Virtual Threads do Java 21 e desserialização Avro.
* Task 3.2 [CQRS/Mongo]: Criar a estrutura do documento Agregado e implementar a escrita no MongoDB.
* Task 3.3 [Observabilidade]: Integrar OpenTelemetry para gerar logs estruturados (Trace ID e Span ID cruzando do Produtor ao Consumidor) para rastrear o fluxo das requisições.
* Task 3.4 [API Endpoint & Tests]: Criar o controller de consulta e validar com testes de integração/unitários.