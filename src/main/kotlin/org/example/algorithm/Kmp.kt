package org.example.algorithm

/** ---------- шаги для визуализации ---------- */
data class KmpStep(val i: Int, val j: Int, val pi: List<Int> = emptyList())
data class PiStep (val i: Int, val j: Int, val pi: List<Int>)

/** ---------- класс-обёртка над KMP ---------- */
class Kmp {

    /** префикс-функция */
    private fun buildPi(p: String): List<Int> {
        val pi = MutableList(p.length) { 0 }
        var j = 0
        for (i in 1 until p.length) {
            while (j > 0 && p[i] != p[j]) j = pi[j - 1]
            if (p[i] == p[j]) j++
            pi[i] = j
        }
        return pi
    }

    /** пошаговый KMP-поиск `pattern` в `text` */
    fun generateSteps(text: String, pattern: String): List<KmpStep> {
        val out = mutableListOf<KmpStep>()
        if (pattern.isEmpty()) return out
        val pi = buildPi(pattern)
        var j = 0
        for (i in text.indices) {
            while (j > 0 && text[i] != pattern[j]) {
                out += KmpStep(i, j, pi); j = pi[j - 1]
            }
            if (text[i] == pattern[j]) {
                out += KmpStep(i, j, pi)     // до ++
                j++
                out += KmpStep(i, j, pi)     // после ++
                if (j == pattern.length) j = pi[j - 1]
            } else out += KmpStep(i, j, pi)
        }
        return out
    }

    /** пошаговая постройка π-массива */
    fun piSteps(p: String): List<PiStep> {
        val pi = MutableList(p.length) { 0 }
        val out = mutableListOf<PiStep>()
        var j = 0
        for (i in 1 until p.length) {
            while (j > 0 && p[i] != p[j]) { j = pi[j - 1]; out += PiStep(i, j, pi.toList()) }
            if (p[i] == p[j]) j++
            pi[i] = j
            out += PiStep(i, j, pi.toList())
        }
        return out
    }

    /** проверка циклического сдвига.
     *  Возвращает (индекс начала `pattern` в `text`, trace-шаги) */
    fun cyclicShift(A: String, B: String): Pair<Int, List<KmpStep>> {
        if (A.length != B.length) return -1 to emptyList()
        if (A.isEmpty())          return 0  to emptyList()

        val text  = A + A                     // «раскручиваем» строку
        val trace = generateSteps(text, B)
        val matchEnd = trace
            .firstOrNull { it.j == B.length } // первый шаг, где j == |B|
            ?.i                               // позиция последнего совпавшего символа

        val shift = matchEnd
            ?.let { it - B.length + 1 }       // переводим в начало
            ?.takeIf { it < A.length } ?: -1  // валиден только < |A|

        return shift to trace
    }
}
