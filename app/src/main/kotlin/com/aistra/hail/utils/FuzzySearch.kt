package com.aistra.hail.utils

import com.aistra.hail.app.HailData
import org.apache.commons.text.similarity.LevenshteinDistance

/** 使用莱文斯坦距离 (Levenshtein distance) 实现模糊搜索 */
object FuzzySearch {
    private val levenshteinDistance = LevenshteinDistance()

    /**
     * 两个字符串差异小于原始字符串长度 且 原始字符串依次包含输入字符串的每个字符 则显示在搜索结果中
     * @param raw 需要匹配的原始字符串
     * @param query 输入的字符串
     */
    fun search(raw: String?, query: String?): Boolean {
        if (query.isNullOrEmpty()) return true
        if (raw.isNullOrEmpty()) return false
        if (raw.contains(query, true)) return true
        if (!HailData.fuzzySearch) return false
        val rawUpp = raw.uppercase()
        val queryUpp = query.uppercase()
        val diff = levenshteinDistance.apply(rawUpp, queryUpp)
        return diff < rawUpp.length && containsInOrder(rawUpp, queryUpp)
    }

    /** 判断字符串A是否依次包含字符串B的每个字符 */
    private fun containsInOrder(strA: String, strB: String): Boolean {
        var indexA = 0  // 用于跟踪字符串A中的位置
        for (charB in strB) {
            // 在字符串A的当前位置之后查找字符charB
            val foundIndex = strA.indexOf(charB, indexA)
            // 如果未找到字符或者字符的位置不是当前位置，表示不包含按顺序的字符
            if (foundIndex == -1) return false
            // 移动到下一个位置，以便查找下一个字符
            indexA = foundIndex + 1
        }
        return true
    }
}