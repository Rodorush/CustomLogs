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

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Árvore de produção: encaminha para o Firebase Crashlytics.
 *
 * | Nível | O que acontece |
 * |---|---|
 * | [Log.VERBOSE], [Log.DEBUG] | descartado — não vai para o logcat nem para o Crashlytics |
 * | [Log.INFO] | vira breadcrumb (`log`), anexado ao próximo relatório |
 * | [Log.WARN], [Log.ERROR] | breadcrumb e, **havendo exceção**, `recordException` |
 *
 * Antes de qualquer registro, os pares de contexto viram custom keys, junto com `log_level`.
 *
 * **Sobre "fatal":** o Crashlytics não expõe API para marcar um evento como fatal —
 * `recordException` sempre produz um **não-fatal**, e fatal, no console, é só o que veio de exceção
 * não capturada que derrubou o app. `WARN` e `ERROR` caem, portanto, no mesmo balde; a custom key
 * `log_level` é o que permite separá-los ao filtrar.
 *
 * Registrar sem exceção (`Log.e(TAG, "mensagem")`) deixa só o breadcrumb: a mensagem aparece
 * anexada ao próximo relatório, mas não cria um evento por si. Para que um erro vire evento,
 * a chamada precisa levar o `Throwable`.
 */
public class CrashlyticsTree : Tree() {

    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    override fun log(
        priority: Int,
        tag: String,
        message: String,
        t: Throwable?,
        keys: Array<out Pair<String, Any?>>,
    ) {
        if (priority <= Log.DEBUG) return

        for ((key, value) in keys) {
            setCustomKey(key, value)
        }
        crashlytics.setCustomKey(KEY_LOG_LEVEL, Log.levelName(priority))
        crashlytics.log("$tag: $message")

        if (priority >= Log.WARN && t != null) {
            crashlytics.recordException(t)
        }
    }

    /**
     * Grava o par com o tipo nativo que o Crashlytics entende, para o valor continuar filtrável no
     * console em vez de virar texto. Tipos fora da lista caem em `toString()`; `null` vira a string
     * `"null"`, porque a API não aceita valor ausente.
     */
    private fun setCustomKey(key: String, value: Any?) {
        when (value) {
            is String -> crashlytics.setCustomKey(key, value)
            is Boolean -> crashlytics.setCustomKey(key, value)
            is Int -> crashlytics.setCustomKey(key, value)
            is Long -> crashlytics.setCustomKey(key, value)
            is Float -> crashlytics.setCustomKey(key, value)
            is Double -> crashlytics.setCustomKey(key, value)
            else -> crashlytics.setCustomKey(key, value.toString())
        }
    }

    private companion object {
        const val KEY_LOG_LEVEL = "log_level"
    }
}
