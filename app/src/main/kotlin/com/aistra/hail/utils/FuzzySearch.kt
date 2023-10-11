package com.aistra.hail.utils

import org.apache.commons.text.similarity.LevenshteinDistance

/**使用莱文斯坦距离 (Levenshtein distance)实现模糊搜索*/
object FuzzySearch {
    private val levenshteinDistance: LevenshteinDistance = LevenshteinDistance()

    /**
     * 两个字符串差异小于搜索字符串长度 且 搜索字符全部包含在搜索字符串中 则显示在搜索结果中
     * @param textToSearch 尝试匹配的字符串
     * @param query 用户输入字符串
     * */
    fun search(textToSearch: String?, query: String?): Boolean {
        if (query.isNullOrEmpty()) {
            return true
        }
        if (textToSearch.isNullOrEmpty()) {
            return false
        }
        if (textToSearch.contains(query, true)) {
            return true
        }
        val textToSearchUpp = textToSearch.uppercase()
        val queryUpp = query.uppercase()
        val diff = levenshteinDistance.apply(textToSearchUpp, queryUpp)
        val lenTextToSearch = textToSearchUpp.length
        return diff < lenTextToSearch && containsAllChars(textToSearchUpp, queryUpp)
    }

    fun containsAllChars(str1: String, str2: String): Boolean {
        val charSet1 = str1.toSet()
        val charSet2 = str2.toSet()
        // 使用交集操作，如果charSet2中的所有字符都在charSet1中，返回true
        return charSet1.containsAll(charSet2)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val testResult1 = search("支付宝", "支")
        assert(testResult1)
        val testResult2 = search("World Peace", "wp")
        assert(testResult2)
        val testResult3 = search("World Peace", "pee")
        assert(testResult3)
    }
}