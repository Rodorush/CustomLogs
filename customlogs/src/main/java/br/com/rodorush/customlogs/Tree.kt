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
 * Destino para onde um registro de log é despachado.
 *
 * Cada árvore plantada em [Log] decide sozinha o que fazer com o registro — escrever no logcat,
 * mandar para um serviço de crash reporting, descartar. É aqui que mora a diferença de
 * comportamento entre debug e release: o app planta [DebugTree] num caso e [CrashlyticsTree] no
 * outro, e o código que chama `Log.d(...)` não muda.
 *
 * Implementações precisam ser seguras para chamada concorrente: [log] é invocado na thread de quem
 * registrou, sem sincronização.
 */
public abstract class Tree {

    /**
     * Recebe um registro já resolvido.
     *
     * @param priority nível do registro, uma das constantes de [Log] ([Log.VERBOSE] a [Log.ERROR]).
     * @param tag identificador de origem, tipicamente o nome da classe que registrou.
     * @param t exceção associada, ou `null` quando o registro não tem uma.
     * @param keys pares de contexto passados na chamada, na ordem em que vieram. Vazio quando não
     *   houve nenhum. Cabe à árvore decidir o que fazer com eles — a [CrashlyticsTree] os transforma
     *   em custom keys, a [DebugTree] os anexa à mensagem.
     */
    public abstract fun log(
        priority: Int,
        tag: String,
        message: String,
        t: Throwable?,
        keys: Array<out Pair<String, Any?>>,
    )
}
