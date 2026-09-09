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

import android.os.Build
import android.util.Log as AndroidLog

/**
 * Árvore de desenvolvimento: escreve tudo no logcat, de [Log.VERBOSE] a [Log.ERROR].
 *
 * Os pares de contexto são anexados ao fim da mensagem, entre chaves, e a stack trace da exceção
 * vai em seguida.
 *
 * Cuida de duas armadilhas do logcat que passam despercebidas até sumir log em campo:
 * - mensagem acima de [MAX_LOG_LENGTH] caracteres é **truncada em silêncio**, então aqui ela sai
 *   quebrada em pedaços, respeitando as quebras de linha existentes;
 * - até a API 25 a tag é limitada a [MAX_TAG_LENGTH] caracteres, e passar do limite lançava
 *   exceção; tags mais longas são cortadas nessas versões.
 *
 * Não deve ser plantada em release — é [CrashlyticsTree] quem faz esse papel.
 */
public class DebugTree : Tree() {

    override fun log(
        priority: Int,
        tag: String,
        message: String,
        t: Throwable?,
        keys: Array<out Pair<String, Any?>>,
    ) {
        val full = buildString {
            append(message)
            if (keys.isNotEmpty()) {
                append(keys.joinToString(separator = ", ", prefix = " {", postfix = "}") {
                    "${it.first}=${it.second}"
                })
            }
            if (t != null) {
                append('\n')
                append(AndroidLog.getStackTraceString(t))
            }
        }
        writeChunked(priority, truncateTag(tag), full)
    }

    /**
     * Escreve respeitando o teto de tamanho do logcat, quebrando primeiro nas quebras de linha da
     * própria mensagem para não partir uma stack trace no meio de um frame.
     */
    private fun writeChunked(priority: Int, tag: String, message: String) {
        if (message.length < MAX_LOG_LENGTH) {
            AndroidLog.println(priority, tag, message)
            return
        }
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            if (newline == -1) newline = length
            do {
                val end = minOf(newline, i + MAX_LOG_LENGTH)
                AndroidLog.println(priority, tag, message.substring(i, end))
                i = end
            } while (i < newline)
            i++
        }
    }

    private fun truncateTag(tag: String): String =
        if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tag
        } else {
            tag.substring(0, MAX_TAG_LENGTH)
        }

    private companion object {
        /** Acima disto o logcat trunca a mensagem sem avisar. */
        const val MAX_LOG_LENGTH = 4000

        /** Limite de tamanho da tag até a API 25. */
        const val MAX_TAG_LENGTH = 23
    }
}
