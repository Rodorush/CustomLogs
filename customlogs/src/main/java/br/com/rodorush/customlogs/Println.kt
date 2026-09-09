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
@file:JvmName("Println")

package br.com.rodorush.customlogs

/**
 * Substituto direto de `kotlin.io.println` para depuração rápida, sem precisar declarar uma `TAG`.
 *
 * Importar `br.com.rodorush.customlogs.println` faz esta função ganhar da `kotlin.io.println` no
 * arquivo, e a partir daí todo `println(...)` passa a respeitar as árvores plantadas — em release,
 * onde a [CrashlyticsTree] descarta [Log.DEBUG], a chamada some, em vez de continuar imprimindo em
 * produção como faria a original.
 *
 * A tag é deduzida da classe que chamou. É uma conveniência de desenvolvimento: para log que vai
 * ficar no código, prefira [Log] com uma `TAG` explícita — deduzir custa uma stack trace por
 * chamada, e ofuscação em release embaralha o nome deduzido.
 */
public fun println(message: Any?) {
    Log.d(callerTag(), message.toString())
}

/** Primeira classe da pilha que não seja uma das classes desta biblioteca. */
private fun callerTag(): String {
    for (element in Throwable().stackTrace) {
        val className = element.className
        if (className !in INTERNAL_CLASSES) {
            return simplify(className)
        }
    }
    return DEFAULT_TAG
}

/**
 * Reduz o nome qualificado ao nome simples da classe, tirando os sufixos que o compilador Kotlin
 * gera para lambdas e classes anônimas (`Foo$1`, `Foo$bar$1`) e o `Kt` das funções de topo.
 */
private fun simplify(className: String): String {
    var name = className.substringAfterLast('.')
    name = ANONYMOUS_CLASS.replace(name, "")
    name = name.substringBefore('$')
    return name.removeSuffix("Kt").ifEmpty { DEFAULT_TAG }
}

private val ANONYMOUS_CLASS = Regex("""(\$\d+)+$""")
private const val DEFAULT_TAG = "CustomLogs"

/**
 * Frames a pular ao deduzir a tag. São nomes de classe exatos, não prefixo de pacote: um
 * consumidor pode viver num pacote com o mesmo início, e o pulo por prefixo o descartaria.
 */
private val INTERNAL_CLASSES: Set<String> = setOf(
    "br.com.rodorush.customlogs.Println",
    Log::class.java.name,
    Tree::class.java.name,
    DebugTree::class.java.name,
    CrashlyticsTree::class.java.name,
)
