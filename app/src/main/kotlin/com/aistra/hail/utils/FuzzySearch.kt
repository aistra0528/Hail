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
        return diff < lenTextToSearch && containsInOrder(textToSearchUpp, queryUpp)
    }
    /**
     * 判断一个字符串A是否依次包含另一个字符串B的每个字符，并且这些字符是按顺序从A的开头开始的
     * @param strA
     * @param strB
     * */
    private fun containsInOrder(strA: String, strB: String): Boolean {
        var indexA = 0  // 用于跟踪字符串A中的位置
        for (charB in strB) {
            // 在字符串A的当前位置之后查找字符charB
            val foundIndex = strA.indexOf(charB, indexA)
            // 如果未找到字符或者字符的位置不是当前位置，表示不包含按顺序的字符
            if (foundIndex == -1 || foundIndex != indexA) {
                return false
            }
            // 移动到下一个位置，以便查找下一个字符
            indexA = foundIndex + 1
        }
        return true
    }
    @JvmStatic
    fun main(args: Array<String>) {
        assert(search("支付宝", "支"))
        assert(search("World Peace", "wp"))
        assert(search("World Peace", "pee"))
        assert(!search("World Peace", "dow"))
    }
}