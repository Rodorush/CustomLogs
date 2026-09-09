/*
 * Copyright 2026 Rodolfo Pereira de Andrade
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.rodorush.customlogs

/**
 * Ponto de entrada da biblioteca: recebe os registros e os despacha para as árvores plantadas.
 *
 * Substitui `android.util.Log` no código do app. Sem nenhuma árvore plantada, toda chamada é
 * descartada em silêncio — é o comportamento desejado em testes unitários, onde `android.util.Log`
 * nem existe.
 *
 * Uso típico, no `Application`:
 * ```
 * Log.plant(if (BuildConfig.DEBUG) DebugTree() else CrashlyticsTree())
 * ```
 *
 * e, no resto do código:
 * ```
 * Log.d(TAG, "fetchCandlesticks: 42 velas")
 * Log.e(TAG, "Erro em fetchCandlesticks", e, "ticker" to ticker, "timeframe" to timeframe)
 * ```
 *
 * Os pares no fim da chamada são contexto estruturado: a [CrashlyticsTree] os grava como custom
 * keys, de modo que o relatório de erro chega com o estado que o produziu, não só com a stack.
 */
public object Log {

    /**
     * Níveis, com os mesmos valores numéricos de `android.util.Log`.
     *
     * São redeclarados aqui de propósito: assim a biblioteca não toca em classe de Android para
     * decidir prioridade, e o despacho pode ser testado na JVM sem Robolectric.
     */
    public const val VERBOSE: Int = 2
    public const val DEBUG: Int = 3
    public const val INFO: Int = 4
    public const val WARN: Int = 5
    public const val ERROR: Int = 6

    @Volatile
    private var trees: Array<Tree> = emptyArray()

    private val lock = Any()

    /** Árvores plantadas no momento, na ordem em que foram plantadas. */
    public val forest: List<Tree>
        get() = trees.asList()

    /**
     * Planta uma árvore. Chamadas seguintes a [v], [d], [i], [w] e [e] passam a alcançá-la.
     *
     * Plantar a mesma instância duas vezes a faz receber o registro duas vezes — a biblioteca não
     * deduplica.
     */
    public fun plant(tree: Tree) {
        synchronized(lock) { trees += tree }
    }

    /** Remove uma árvore específica. Não faz nada se ela não estava plantada. */
    public fun uproot(tree: Tree) {
        synchronized(lock) { trees = trees.filterNot { it === tree }.toTypedArray() }
    }

    /** Remove todas as árvores. Útil para isolar testes. */
    public fun uprootAll() {
        synchronized(lock) { trees = emptyArray() }
    }

    @JvmStatic
    public fun v(tag: String, message: String, vararg keys: Pair<String, Any?>): Unit =
        dispatch(VERBOSE, tag, message, null, keys)

    @JvmStatic
    public fun v(tag: String, message: String, t: Throwable?, vararg keys: Pair<String, Any?>): Unit =
        dispatch(VERBOSE, tag, message, t, keys)

    @JvmStatic
    public fun d(tag: String, message: String, vararg keys: Pair<String, Any?>): Unit =
        dispatch(DEBUG, tag, message, null, keys)

    @JvmStatic
    public fun d(tag: String, message: String, t: Throwable?, vararg keys: Pair<String, Any?>): Unit =
        dispatch(DEBUG, tag, message, t, keys)

    @JvmStatic
    public fun i(tag: String, message: String, vararg keys: Pair<String, Any?>): Unit =
        dispatch(INFO, tag, message, null, keys)

    @JvmStatic
    public fun i(tag: String, message: String, t: Throwable?, vararg keys: Pair<String, Any?>): Unit =
        dispatch(INFO, tag, message, t, keys)

    @JvmStatic
    public fun w(tag: String, message: String, vararg keys: Pair<String, Any?>): Unit =
        dispatch(WARN, tag, message, null, keys)

    @JvmStatic
    public fun w(tag: String, message: String, t: Throwable?, vararg keys: Pair<String, Any?>): Unit =
        dispatch(WARN, tag, message, t, keys)

    @JvmStatic
    public fun e(tag: String, message: String, vararg keys: Pair<String, Any?>): Unit =
        dispatch(ERROR, tag, message, null, keys)

    @JvmStatic
    public fun e(tag: String, message: String, t: Throwable?, vararg keys: Pair<String, Any?>): Unit =
        dispatch(ERROR, tag, message, t, keys)

    /**
     * Nome curto do nível — `"DEBUG"`, `"WARN"`, etc. Usado na custom key `log_level` da
     * [CrashlyticsTree], e exposto porque toda [Tree] de terceiro precisa dele para formatar.
     * Prioridade desconhecida volta como o próprio número.
     */
    @JvmStatic
    public fun levelName(priority: Int): String = when (priority) {
        VERBOSE -> "VERBOSE"
        DEBUG -> "DEBUG"
        INFO -> "INFO"
        WARN -> "WARN"
        ERROR -> "ERROR"
        else -> priority.toString()
    }

    private fun dispatch(
        priority: Int,
        tag: String,
        message: String,
        t: Throwable?,
        keys: Array<out Pair<String, Any?>>,
    ) {
        val current = trees
        for (index in current.indices) {
            current[index].log(priority, tag, message, t, keys)
        }
    }
}
