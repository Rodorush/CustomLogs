# CustomLogs

[![Maven Central](https://img.shields.io/maven-central/v/io.github.rodorush/customlogs)](https://central.sonatype.com/artifact/io.github.rodorush/customlogs)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

Fachada de log para Android que resolve dois problemas de uma vez: **o que sai em cada tipo de
build**, e **o contexto que acompanha o erro**.

Trocar `android.util.Log` por `br.com.rodorush.customlogs.Log` faz com que `DEBUG` e `VERBOSE`
sumam em release sem `if (BuildConfig.DEBUG)` espalhado pelo código, e permite anexar pares
chave/valor à chamada — que chegam ao Firebase Crashlytics como *custom keys*, de modo que o
relatório venha com o estado que produziu o erro, não só com a stack.

```kotlin
Log.e(TAG, "Erro em fetchCandlesticks", e, "ticker" to ticker, "timeframe" to timeframe)
```

## Instalação

```kotlin
dependencies {
    implementation("io.github.rodorush:customlogs:0.1.0")
}
```

Nada além do `mavenCentral()` que você já tem. A library traz o `firebase-crashlytics` junto —
veja [Dependências](#dependências).

## Uso

Plante uma árvore uma vez, no `Application`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.plant(if (BuildConfig.DEBUG) DebugTree() else CrashlyticsTree())
    }
}
```

E registre normalmente, com uma `TAG` por arquivo:

```kotlin
private const val TAG = "CandlestickRepository"

Log.d(TAG, "fetchCandlesticks: ${velas.size} velas")
Log.w(TAG, "sendEmailVerification: falha", task.exception)
Log.e(TAG, "Erro ao chamar updateCandlesticks", e, "ticker" to ticker, "timeframe" to timeframe)
```

Os cinco níveis (`v`, `d`, `i`, `w`, `e`) têm duas formas: com e sem `Throwable`. O `Throwable` é
nulável, então `task.exception` do Firebase entra direto, sem `?.let`.

Sem nenhuma árvore plantada, toda chamada é descartada em silêncio — é o que faz os testes
unitários rodarem sem `android.util.Log`.

### `println` sem TAG

Para depuração rápida, existe um substituto direto do `println` do Kotlin, com a tag deduzida da
classe chamadora:

```kotlin
import br.com.rodorush.customlogs.println

println("chegou aqui")
```

O import faz esta função ganhar da `kotlin.io.println` no arquivo. Como ela registra em nível
`DEBUG`, **some em release** — ao contrário da original, que continuaria imprimindo em produção.
Para log que vai ficar no código, prefira `Log` com uma `TAG` explícita: deduzir custa uma stack
trace por chamada, e a ofuscação embaralha o nome deduzido.

## Comportamento por nível

O que cada nível faz depende da árvore plantada:

| Nível | `DebugTree` (debug) | `CrashlyticsTree` (release) |
|---|---|---|
| `VERBOSE` | logcat | **descartado** |
| `DEBUG` | logcat | **descartado** |
| `INFO` | logcat | breadcrumb (`log`) |
| `WARN` | logcat | breadcrumb + `recordException` **se houver `Throwable`** |
| `ERROR` | logcat | breadcrumb + `recordException` **se houver `Throwable`** |

Em todo registro que chega ao Crashlytics, os pares chave/valor viram `setCustomKey` antes do
envio, junto com uma chave `log_level` (`INFO`/`WARN`/`ERROR`).

### Duas coisas que costumam surpreender

**"Fatal" não existe por aqui.** O Crashlytics não expõe API para marcar um evento como fatal:
`recordException` produz **sempre um não-fatal**, e fatal, no console, é só o que veio de exceção
não capturada que derrubou o app. `WARN` e `ERROR` caem no mesmo balde — a custom key `log_level`
é o que permite separá-los ao filtrar.

**Erro sem `Throwable` não vira evento.** `Log.e(TAG, "mensagem")` deixa só o breadcrumb: a
mensagem aparece anexada ao próximo relatório, mas não cria um evento por si. Para que um erro
vire evento no console, a chamada precisa levar o `Throwable`.

## Sua própria árvore

`Tree` é uma classe aberta com um método só. Dá para mandar log para onde você quiser — um
arquivo, um backend, um buffer em memória para testes:

```kotlin
class RecordingTree : Tree() {
    val records = mutableListOf<String>()

    override fun log(
        priority: Int,
        tag: String,
        message: String,
        t: Throwable?,
        keys: Array<out Pair<String, Any?>>,
    ) {
        records += "${Log.levelName(priority)}/$tag: $message"
    }
}
```

`Log.plant()` aceita quantas árvores você quiser; `Log.uproot(tree)` remove uma e
`Log.uprootAll()` limpa tudo, o que é útil entre testes.

## Dependências

A library depende de `firebase-crashlytics` — quem consome leva o Firebase junto. É consequência
de a `CrashlyticsTree` vir na caixa.

Se isso incomodar, a evolução natural é quebrar em `customlogs-core` (sem Firebase) e
`customlogs-crashlytics`; ainda não foi feito porque o único consumidor hoje já usa Firebase.
Abra uma issue se for o seu caso.

**Não depende do Timber.** O contrato de `Tree` carrega os pares chave/valor, e a
`Timber.Tree` não tem onde encaixá-los. Em compensação, a `DebugTree` daqui porta as duas
armadilhas de logcat que o Timber resolve: quebra de mensagens acima de 4000 caracteres (acima
disso o logcat trunca em silêncio) e corte de tag acima de 23 caracteres até a API 25.

## Documentação de API

KDoc navegável em
[javadoc.io/doc/io.github.rodorush/customlogs](https://javadoc.io/doc/io.github.rodorush/customlogs).

## Requisitos

- `minSdk` 23
- Compilada contra a API 35, `jvmTarget` 1.8

## Licença

Apache License 2.0 — veja [LICENSE](LICENSE).

Copyright 2026 Rodolfo Pereira de Andrade.
