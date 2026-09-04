# Task 1.3 — Pipeline CI/CD com Análise de Qualidade

## Objetivo

Ter um pipeline de CI que builda, testa e barra o merge de código com
cobertura insuficiente ou problemas de qualidade — antes que isso chegue
na `develop`.

## O que foi feito

- `.github/workflows/ci.yml`: build + testes de todos os módulos, geração
  de relatório de cobertura (Jacoco), análise SonarCloud e verificação do
  Quality Gate.
- `build.gradle` (raiz): plugin `org.sonarqube`, `jacoco` aplicado em todos
  os submódulos via `subprojects {}`, bloco `sonar {}` com `projectKey` e
  `organization` apontando pro SonarCloud.

## Decisões e trade-offs

**SonarCloud (não uma instância própria) para a organização `erichiroshi`
em CI; SonarQube local via Docker para uso durante o desenvolvimento.**
SonarCloud é gratuito para projetos públicos e não exige manter
infraestrutura própria — faz sentido para CI. Uma instância local (que
apareceu depois no `docker-compose.yml`) serve pra rodar a análise durante
o desenvolvimento, sem depender de internet nem consumir o rate limit do
SonarCloud a cada `./gradlew sonar` local.

**Jacoco por submódulo, agregado via `sonar.coverage.jacoco.
xmlReportPaths`.** Em vez de um único relatório de cobertura pro monorepo
inteiro, cada módulo gera o seu (`build/reports/jacoco/test/
jacocoTestReport.xml`), e o Sonar recebe a lista de todos os caminhos. Isso
evita ter que configurar um plugin de agregação Gradle separado — o Sonar
já sabe juntar múltiplos relatórios XML.

**Quality Gate bloqueando de verdade — só depois de um retrabalho.** A
primeira versão do pipeline rodava a análise do Sonar mas não falhava o
job quando o Quality Gate reprovava (a step ficou fora do `ci.yml` por um
tempo, comentada como "removida por estar falhando"). O bug raiz: a action
`sonarqube-quality-gate-action` consulta a API do Sonar diretamente e
precisa da env `SONAR_HOST_URL` — sem ela, tenta falar com o
`sonarqube.com` público em vez do SonarCloud. Corrigido explicitando
`SONAR_HOST_URL: https://sonarcloud.io` tanto na análise quanto na
verificação do gate.

**Não pinar Actions por SHA (por enquanto).** Pinar por commit hash (em
vez de tag, ex: `actions/checkout@v4`) é mais seguro contra supply-chain
attacks — uma tag pode ser movida, um SHA não. Cheguei a tentar isso, mas
não tinha como validar os hashes reais no ambiente onde gerei os arquivos,
e inventar um hash quebraria o workflow (pior que não pinar). Ficou como
próximo passo, usando uma ferramenta que resolve os SHAs de verdade
(Dependabot ou Renovate), não hash "de memória".

## Como funciona (fluxo)

```
push/PR em main ou develop
  → checkout (fetch-depth: 0, necessário pro Sonar analisar blame/histórico)
  → setup JDK 25
  → ./gradlew build jacocoTestReport   (builda + testa todos os módulos)
  → publica resultados de teste como artifact
  → ./gradlew sonar                    (envia a análise pro SonarCloud)
  → sonarqube-quality-gate-action      (consulta o resultado do Quality Gate;
                                         falha o job se reprovado)
```

Com branch protection configurada na `develop`/`main` exigindo esse check,
o botão de merge do PR fica bloqueado até o Quality Gate passar.

## Pendências / próximos passos

- Nenhum threshold de cobertura customizado configurado no SonarCloud (usa
  o Quality Gate padrão "Sonar way", que valida principalmente código
  *novo*, não o projeto inteiro). Se quiser uma cobertura mínima
  específica do projeto todo, isso se configura direto no SonarCloud, não
  no `build.gradle`.
- Sem cache de dependências Gradle configurado no workflow (o
  `gradle/actions/setup-gradle` já faz cache de forma automática, mas vale
  revisar se está realmente sendo reaproveitado entre runs).
