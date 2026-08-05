# Stacking Tracker

App Android nativo para acompanhar estoque de prata fisica (moedas, barras, redondas).
Offline-first: tudo fica no dispositivo, sem conta e sem login. Só a cotação depende de rede.

## Stack

Kotlin · Jetpack Compose · Material 3 · Room · MVVM (ViewModel + StateFlow) · Retrofit + kotlinx.serialization · Coil

minSdk 26 · compileSdk/targetSdk 36 · Gradle Kotlin DSL

Gradle 9.6.1 · AGP 9.3.1 · Kotlin 2.2.10 · KSP 2.2.10-2.0.2 · Compose BOM 2026.06.01 · Room 2.8.4

Essas versões não são livres, e a combinação foi encontrada na marra. As amarras:

- O AGP 9 **embute o suporte a Kotlin** e recusa o plugin `kotlin-android`, que por isso não aparece
  no `libs.versions.toml`.
- O AGP 9 traz o KGP **2.2.10** como dependência de runtime. Os plugins de Compose e serialização
  seguem essa versão; usar outra exigiria sobrescrever o `classpath` no `buildscript`.
- O AGP 8.x usa uma API interna do Gradle removida na **9.6.0**, então voltar para o AGP 8 obrigaria
  a fixar o Gradle em 9.5.x.
- `androidx.core:core:1.19.0` exige **AGP 9.1+** e `compileSdk` **37**. O `targetSdk` fica em 36 de
  propósito: é o Android mais recente em que o app foi de fato exercitado.

## Como abrir

O wrapper está versionado, então basta:

```bash
./gradlew assembleDebug
```

Ou abrir a pasta direto no Android Studio.

## Configuração da API de cotação

Copie `local.properties.example` para `local.properties` e preencha:

```properties
metals.api.baseUrl=https://api.metals.dev/v1/
metals.api.key=SUA_CHAVE
```

`local.properties` está no `.gitignore`. Os valores viram `BuildConfig.METALS_BASE_URL` e
`BuildConfig.METALS_API_KEY`.

O app espera do endpoint `latest` uma resposta assim:

```json
{
  "metals":     { "silver": 31.42 },
  "currencies": { "BRL": 5.42 }
}
```

com o preço da prata em **USD por onça troy**. Outro formato? Ajuste
[MetaisDto.kt](app/src/main/java/com/stacking/tracker/data/remote/MetaisDto.kt) e o mapeamento em
[CotacaoRepository.atualizar()](app/src/main/java/com/stacking/tracker/data/repo/CotacaoRepository.kt).

**Sem chave o app continua funcionando.** A tela de Cotação tem "Lançar manual", que grava um spot
digitado à mão no mesmo histórico — é o que alimenta valor de mercado, lucro e prêmio.

## Telas

| Tela | O que mostra |
|---|---|
| Painel | Valor de mercado, investido, lucro/prejuízo em R$ e %, prêmio médio, estoque em oz e gramas, spot atual |
| Inventário | Lista de peças com busca por nome/marca, filtro por tipo e ordenação por data, valor ou peso |
| Adicionar/Editar | Formulário com onças troy, onças finas e prêmio calculados em tempo real |
| Detalhe | Ficha completa, foto, prêmio contra o spot da compra, editar e excluir |
| Cotação | Spot USD e BRL, botão de atualizar, entrada manual e histórico |

## Cálculos

```
pesoTroyOz     = pesoGramas / 31.1035
ozFinas        = pesoTroyOz * pureza
valorAtualPeca = ozFinas * precoOzAtual
premioPercent  = (precoPago - valorSpotNaCompra) / valorSpotNaCompra * 100
lucroTotal     = valorMercadoTotal - totalInvestido
```

Sobre o **prêmio**: a entity `Peca` não guarda o spot do dia da compra, então o spot de referência
vem do histórico de cotações — a cotação mais recente que não seja posterior ao **fim do dia** da
compra. Se a compra for anterior a todo o histórico, usa a cotação mais antiga disponível. Sem
nenhuma cotação registrada, o prêmio aparece como `--` em vez de um número inventado.

A comparação é por dia, não por instante: uma peça comprada hoje e uma cotação lançada hoje às 14h
se encontram, mesmo a compra sendo gravada como meia-noite. Para compras antigas, lance uma cotação
manual com o spot da época.

Datas de compra são gravadas como meia-noite no **fuso local**. O DatePicker do Material 3 trabalha
em UTC, então as conversões nos dois sentidos ficam em
[Datas.kt](app/src/main/java/com/stacking/tracker/core/Datas.kt) — sem elas a data escolhida volta um
dia para quem está a oeste de Greenwich.

Os cálculos estão isolados em [Calculos.kt](app/src/main/java/com/stacking/tracker/core/Calculos.kt)
e cobertos por testes JVM em
[CalculosTest.kt](app/src/test/java/com/stacking/tracker/core/CalculosTest.kt):

```bash
./gradlew testDebugUnitTest
```

## Estrutura

```
com.stacking.tracker
├── core/            Calculos.kt, Formatos.kt      (lógica pura, testável)
├── data/
│   ├── local/       entities, DAOs, StackingDatabase
│   ├── remote/      MetaisApi, DTOs, Retrofit
│   └── repo/        PecaRepository, CotacaoRepository
├── ui/
│   ├── theme/       cores, tipografia, tema
│   ├── componentes/ Painel, LinhaDado, CartaoMetrica, CampoTexto
│   ├── dashboard/ inventario/ editor/ detalhe/ cotacao/   (Screen + ViewModel)
│   └── navegacao/   rotas e barra de abas
├── util/            fotos, preferências
└── ContainerApp.kt  service locator (sem Hilt: o grafo é pequeno)
```

## Decisões

- **Sem Hilt.** Cinco ViewModels e três dependências não justificam o processador de anotações.
  `ContainerApp` é criado na `Application` e uma `viewModelFactory` única resolve tudo.
- **Filtro e ordenação em memória.** O DAO devolve a coleção inteira; ordenar por valor depende da
  cotação, que não está na tabela `pecas`. Uma coleção de stacker cabe folgada na memória.
- **Fotos copiadas para o armazenamento interno.** A permissão da `Uri` do seletor morre quando o
  processo reinicia, então guardar só a `Uri` deixaria a foto quebrada depois.
- **Sem migração de banco ainda.** Versão 1, schema exportado em `app/schemas/`. Ao alterar uma
  entity, escreva a `Migration` — não há `fallbackToDestructiveMigration`.
