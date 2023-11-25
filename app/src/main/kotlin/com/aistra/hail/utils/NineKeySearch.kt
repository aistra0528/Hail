package com.aistra.hail.utils

import net.sourceforge.pinyin4j.PinyinHelper
import java.util.Collections

object NineKeySearch {
    fun searchAll(vararg raws: String?, query: String?): Boolean {
        if (query.isNullOrEmpty()) return true
        else if (raws.all { it.isNullOrEmpty() }) return false
        else return raws.any { search(it, query) }
    }

    private fun search(raw: String?, query: String): Boolean {
        return !raw.isNullOrEmpty() && FuzzySearch.search(toNineKey(toPinyin(raw)), query)
    }


    private fun toPinyin(raw: String): String {
        val sb = StringBuilder()
        raw.map {
            when (it) {
                in '0'..'9' -> sb.append(it)
                in 'a'..'z' -> sb.append(it)
                in 'A'..'Z' -> sb.append(it)
                else -> {
                    val r = PinyinHelper.toHanyuPinyinStringArray(it);
                    r?.let { it1 ->
                        for (s in it1) {
                            sb.append(s)
                        }
                    }

                }
            }
        }
        return sb.toString()
    }

    private fun pinyinToNineKey(raw: Char): Char {
        val _raw = if (raw in 'A'..'Z') raw - ('A' - 'a') else raw

        return when (_raw) {
            in 'a'..'c' -> '2'
            in 'd'..'f' -> '3'
            in 'g'..'i' -> '4'
            in 'j'..'l' -> '5'
            in 'm'..'o' -> '6'
            in 'p'..'s' -> '7'
            in 't'..'v' -> '8'
            in 'w'..'z' -> '9'
            else -> _raw
        }
    }

    private fun toNineKey(raw: String): String {
        val sb = StringBuilder()

        for (c in toPinyin(raw)) {
            sb.append(pinyinToNineKey(c))
        }
        return sb.toString()
    }
}