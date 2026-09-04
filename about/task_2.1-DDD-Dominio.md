# Task 2.1 — Domínio Isolado (DDD)

## Objetivo

Modelar as regras de negócio centrais de uma câmara de compensação —
validação de garantias e execução de um trade — de forma isolada de
qualquer framework, pra que a lógica de negócio seja testável em
milissegundos e não dependa de Spring, JPA ou banco de dados pra ser
validada.

## O que foi feito

Módulo `clearing-domain`, pacote `br.com.erichiroshi.clearing.domain`:

- **`Comprador`** — saldo/garantia disponível; `debitar()` valida antes de
  mutar.
- **`Vendedor`** — posições de ativos em custódia (`Map<ticker,
  quantidade>`); `reduzirPosicao()` valida antes de mutar.
- **`Ativo`** — value object (ticker + nome), igualdade por ticker.
- **`Trade`** — aggregate root com ciclo de vida
  `PENDENTE → VALIDADO → LIQUIDADO` (ou `REJEITADO`).
- **`SaldoInsuficienteException`**, **`PosicaoInsuficienteException`**.
- Ports (`CompradorRepository`, `VendedorRepository`, `TradeRepository`) —
  interfaces implementadas só na Task 2.2, com JPA.
- 12 testes unitários (JUnit 5 + AssertJ).

## Decisões e trade-offs

**Ciclo de estados no `Trade`, em vez de um objeto tudo-ou-nada.** O
objetivo declarado do projeto é aprender/demonstrar como uma câmara de
compensação funciona — e isso é justamente o registro da intenção, a
validação/bloqueio de garantias, e só depois a liquidação, como etapas
distintas e observáveis. Um `Trade` que só existe se der tudo certo não
mostra nada disso. Optei por 4 estados: `PENDENTE` (registrado, nada
validado ainda), `VALIDADO` (garantias conferidas e já debitadas/
reduzidas), `LIQUIDADO` (persistência + publicação do evento confirmadas —
fechado na Task 2.3), `REJEITADO` (terminal, alguma garantia faltou).

**Delivery versus Payment (DvP) implementado explicitamente em
`Trade.validar()`.** É o princípio central de uma clearing: o ativo só
muda de dono se o dinheiro também mudar, atomicamente — nunca uma ponta
sem a outra. Por isso `validar()` primeiro checa as duas condições
(`possuiSaldoSuficiente` e `possuiPosicaoSuficiente`) **sem mutar nada**, e
só debita/reduz se as duas passarem. Testado explicitamente:
`validarSemSaldoDoCompradorDeveRejeitarSemMexerNaPosicaoDoVendedor` e o
espelho pro vendedor garantem que uma falha não deixa a outra ponta
parcialmente executada.

**`Trade` com dois factories: `registrar()` e `reconstituir()`.**
`registrar()` sempre cria um trade novo, em `PENDENTE`, com ID e timestamp
gerados na hora — é o único caminho que a lógica de negócio usa.
`reconstituir()` foi adicionado depois (durante a Task 2.2) porque os
adaptadores de persistência precisam recriar o objeto de domínio a partir
do que já está salvo no banco (com o status e os timestamps reais), sem
reexecutar as regras de criação. Separar os dois evita confundir "estou
criando um trade novo" com "estou carregando um trade existente" — um erro
comum é usar o mesmo construtor/factory pros dois casos e acabar gerando
um novo ID ou um novo timestamp sem querer ao recarregar do banco.

**`Vendedor` guarda posições como `Map<String, BigDecimal>` simples, não
uma lista de objetos `Posicao`.** Para o escopo atual (só ticker +
quantidade), um Value Object `Posicao` seria estrutura extra sem
comportamento próprio. Fica como candidato a refatoração se posições
ganharem mais atributos (ex: custo médio, data de aquisição).

**Nenhuma dependência do domínio em Lombok, Spring ou qualquer coisa fora
do JDK.** Módulo `clearing-domain` só tem `org.apache.avro:avro`? Não —
nem isso: as dependências de main são zero; só `testImplementation` (JUnit
5, Mockito, AssertJ). Isso é o que a Task 2.1 pedia ("sem nenhuma
dependência do Spring, JPA ou drivers de banco de dados") levado ao
extremo — o pacote `model` inteiro compila com `javac` puro.

## Como funciona (fluxo)

```
Trade.registrar(comprador, vendedor, ativo, quantidade, preco)
  → status = PENDENTE (nada mutado ainda)

trade.validar()
  → checa comprador.possuiSaldoSuficiente(valorTotal)
  → checa vendedor.possuiPosicaoSuficiente(ticker, quantidade)
  → se as duas OK: comprador.debitar(...) + vendedor.reduzirPosicao(...)
    → status = VALIDADO
  → se uma falhar: status = REJEITADO, lança a exceção correspondente
    (nada foi mutado)

trade.liquidar()   [chamado só depois da Task 2.3 confirmar o Kafka]
  → status = LIQUIDADO
```

## Pendências / próximos passos

- Sem suporte a "netting multilateral" (compensar múltiplos trades do
  mesmo participante antes de liquidar) — é uma ideia de roadmap discutida
  mas fora do escopo atual, candidata a um épico futuro.
- `Ativo` é um Value Object simples; não há um `AtivoRepository` nem
  validação de que o ticker existe num catálogo real de ativos negociáveis.
