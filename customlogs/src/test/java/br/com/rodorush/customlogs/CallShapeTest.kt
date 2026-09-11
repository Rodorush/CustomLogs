package br.com.rodorush.customlogs

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Trava as formas de chamada que os consumidores realmente usam.
 *
 * As oito formas abaixo foram extraídas das 88 chamadas do ChartPatternTracker, que consome esta
 * library. Um teste de compilação, na prática: se uma assinatura pública mudar de um jeito que
 * quebre qualquer uma delas, este arquivo para de compilar antes de o consumidor descobrir.
 */
class CallShapeTest {

    private lateinit var tree: CountingTree

    @Before
    fun setUp() {
        Log.uprootAll()
        tree = CountingTree()
        Log.plant(tree)
    }

    @After
    fun tearDown() {
        Log.uprootAll()
    }

    @Test
    fun `as formas usadas pelos consumidores compilam e despacham`() {
        val erro = IllegalStateException("falha")

        // `task.exception` do Firebase é Exception?, não Exception: a sobrecarga precisa aceitar nulo.
        val talvezErro: Exception? = null

        val ticker = "PETR4"
        val timeframe = "1d"

        Log.d(TAG, "mensagem simples")
        Log.d(TAG, "com interpolação: ${1 + 1} velas")
        Log.w(TAG, "aviso sem exceção")
        Log.w(TAG, "aviso com exceção", erro)
        Log.w(TAG, "aviso com exceção possivelmente nula", talvezErro)
        Log.e(TAG, "erro sem exceção")
        Log.e(TAG, "erro com exceção", erro)
        Log.e(TAG, "erro com dois pares", erro, "ticker" to ticker, "timeframe" to timeframe)
        Log.e(TAG, "erro com três pares", erro, "ticker" to ticker, "timeframe" to timeframe, "origem" to "cache")

        // Níveis que o consumidor ainda não usa, mas que o contrato promete.
        Log.v(TAG, "verbose")
        Log.v(TAG, "verbose com exceção", erro)
        Log.i(TAG, "info")
        Log.i(TAG, "info com exceção", erro)
        Log.i(TAG, "info com par", "ticker" to ticker)

        assertEquals(14, tree.count)
    }

    @Test
    fun `plant aceita as duas arvores da caixa`() {
        // Instanciar DebugTree/CrashlyticsTree aqui exigiria Android e Firebase; o que importa é
        // que plant aceite qualquer Tree, que é o que o Application do consumidor faz.
        Log.uprootAll()
        Log.plant(CountingTree())
        assertEquals(1, Log.forest.size)
    }

    private class CountingTree : Tree() {
        var count = 0

        override fun log(
            priority: Int,
            tag: String,
            message: String,
            t: Throwable?,
            keys: Array<out Pair<String, Any?>>,
        ) {
            count++
        }
    }

    private companion object {
        const val TAG = "CallShapeTest"
    }
}
