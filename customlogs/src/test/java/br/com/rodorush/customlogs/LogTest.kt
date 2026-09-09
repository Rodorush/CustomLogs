package br.com.rodorush.customlogs

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Cobre o despacho de [Log] com uma árvore de mentira. Não toca em classe de Android — é o motivo
 * de os níveis serem constantes da própria biblioteca.
 */
class LogTest {

    private lateinit var tree: RecordingTree

    @Before
    fun setUp() {
        Log.uprootAll()
        tree = RecordingTree()
        Log.plant(tree)
    }

    @After
    fun tearDown() {
        Log.uprootAll()
    }

    @Test
    fun `sem arvore plantada nao explode`() {
        Log.uprootAll()
        Log.e("TAG", "ninguem ouve", RuntimeException("x"), "k" to "v")
        assertTrue(Log.forest.isEmpty())
    }

    @Test
    fun `cada nivel chega com a prioridade correspondente`() {
        Log.v("TAG", "v")
        Log.d("TAG", "d")
        Log.i("TAG", "i")
        Log.w("TAG", "w")
        Log.e("TAG", "e")

        assertEquals(
            listOf(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR),
            tree.records.map { it.priority },
        )
        assertEquals(listOf("v", "d", "i", "w", "e"), tree.records.map { it.message })
    }

    @Test
    fun `chamada sem throwable chega com throwable nulo e sem pares`() {
        Log.d("TAG", "mensagem")

        val record = tree.records.single()
        assertEquals("TAG", record.tag)
        assertNull(record.t)
        assertTrue(record.keys.isEmpty())
    }

    @Test
    fun `throwable nulo explicito e aceito`() {
        // FirebaseAuthProvider do app faz exatamente isto: task.exception e nullable.
        val ausente: Throwable? = null
        Log.w("TAG", "falha", ausente)

        assertNull(tree.records.single().t)
    }

    @Test
    fun `pares de contexto chegam na ordem e sem perda`() {
        val boom = IllegalStateException("boom")
        Log.e("TAG", "Erro em fetchCandlesticks", boom, "ticker" to "PETR4", "timeframe" to "1d")

        val record = tree.records.single()
        assertSame(boom, record.t)
        assertEquals(
            listOf("ticker" to "PETR4", "timeframe" to "1d"),
            record.keys.toList(),
        )
    }

    @Test
    fun `pares sao aceitos sem throwable`() {
        Log.i("TAG", "sem excecao", "ticker" to "VALE3")

        val record = tree.records.single()
        assertNull(record.t)
        assertEquals(listOf<Pair<String, Any?>>("ticker" to "VALE3"), record.keys.toList())
    }

    @Test
    fun `valor nulo em par e preservado`() {
        Log.d("TAG", "msg", "ticker" to null)

        assertEquals(listOf<Pair<String, Any?>>("ticker" to null), tree.records.single().keys.toList())
    }

    @Test
    fun `todas as arvores plantadas recebem o registro`() {
        val outra = RecordingTree()
        Log.plant(outra)

        Log.d("TAG", "para as duas")

        assertEquals(1, tree.records.size)
        assertEquals(1, outra.records.size)
    }

    @Test
    fun `uproot remove apenas a arvore indicada`() {
        val outra = RecordingTree()
        Log.plant(outra)
        Log.uproot(tree)

        Log.d("TAG", "so para a outra")

        assertTrue(tree.records.isEmpty())
        assertEquals(1, outra.records.size)
        assertEquals(listOf<Tree>(outra), Log.forest)
    }

    @Test
    fun `println usa o nivel debug e deduz a tag do chamador`() {
        println("sem tag")

        val record = tree.records.single()
        assertEquals(Log.DEBUG, record.priority)
        assertEquals("sem tag", record.message)
        assertEquals("LogTest", record.tag)
    }

    @Test
    fun `println aceita nulo`() {
        println(null)

        assertEquals("null", tree.records.single().message)
    }

    @Test
    fun `levelName cobre os cinco niveis`() {
        assertEquals("VERBOSE", Log.levelName(Log.VERBOSE))
        assertEquals("DEBUG", Log.levelName(Log.DEBUG))
        assertEquals("INFO", Log.levelName(Log.INFO))
        assertEquals("WARN", Log.levelName(Log.WARN))
        assertEquals("ERROR", Log.levelName(Log.ERROR))
    }

    private class RecordingTree : Tree() {
        val records = mutableListOf<Record>()

        override fun log(
            priority: Int,
            tag: String,
            message: String,
            t: Throwable?,
            keys: Array<out Pair<String, Any?>>,
        ) {
            records += Record(priority, tag, message, t, keys)
        }
    }

    private class Record(
        val priority: Int,
        val tag: String,
        val message: String,
        val t: Throwable?,
        val keys: Array<out Pair<String, Any?>>,
    )
}
