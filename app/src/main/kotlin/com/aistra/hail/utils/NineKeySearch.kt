package com.aistra.hail.utils

import net.sourceforge.pinyin4j.PinyinHelper

object NineKeySearch {
    fun search(query: String?, vararg strings: String?): Boolean =
        query.isNullOrEmpty() || strings.any {
            !it.isNullOrEmpty() && FuzzySearch.search(
                toNineKey(toPinyin(it)),
                query
            )
        }

    private fun toPinyin(raw: String): String = buildString {
        raw.map {
            when (it) {
                in '0'..'9' -> append(it)
                in 'a'..'z' -> append(it)
                in 'A'..'Z' -> append(it)
                else -> PinyinHelper.toHanyuPinyinStringArray(it)?.let { strings ->
                    for (str in strings) append(str)
                }
            }
        }
    }

    private fun pinyinToNineKey(raw: Char): Char = raw.lowercaseChar().let {
        when (it) {
            in 'a'..'c' -> '2'
            in 'd'..'f' -> '3'
            in 'g'..'i' -> '4'
            in 'j'..'l' -> '5'
            in 'm'..'o' -> '6'
            in 'p'..'s' -> '7'
            in 't'..'v' -> '8'
            in 'w'..'z' -> '9'
            else -> it
        }
    }

    private fun toNineKey(raw: String): String = buildString {
        for (ch in toPinyin(raw)) append(pinyinToNineKey(ch))
    }
}