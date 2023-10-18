package com.aistra.hail.utils

import net.sourceforge.pinyin4j.PinyinHelper
import java.util.Locale

/** 中文拼音搜索类 */
object PinyinSearch {
    /**
     * 当前语言是中文时使用首字母和全拼进行搜索，满足任一条件则显示在搜索结果中
     * @param raw 需要匹配的原始字符串
     * @param query 输入的字符串
     */
    fun searchPinyinAll(raw: String?, query: String?): Boolean {
        if (query.isNullOrEmpty()) return true
        if (raw.isNullOrEmpty()) return false
        return if (Locale.getDefault().language != Locale.CHINESE.language) false
        else searchCap(raw, query) || searchAllSpell(raw, query)
    }

    /**
     * 根据拼音首字母进行搜索
     * 如搜索“计算器”时只需输入 "jsq"
     */
    private fun searchCap(raw: String, pinyinCap: String): Boolean {
        if (pinyinCap.length > 8) return false // "最强多媒体播放器".length
        for (index in getNameStringList(raw)) {
            if (index.contains(pinyinCap, true)) {
                return true
            }
        }
        return false
    }

    /**
     * 根据全部拼音进行搜索
     * 如搜索“计算器”时只需输入 "jisuanqi"
     */
    private fun searchAllSpell(raw: String, pinyinAll: String): Boolean {
        if (pinyinAll.length > 48) return false // "chuang".length * 8
        for (index in getNameStringPinyinAll(raw)) {
            if (index.contains(pinyinAll, true)) {
                return true
            }
        }
        return false
    }

    private fun getNameStringPinyinAll(target: String): ArrayList<String> {
        val res = ArrayList<String>()
        getNameCapListPinyinAll(Array(target.length) { "" }, 0, target, res)
        return res
    }

    private fun getNameStringList(target: String): ArrayList<String> {
        val res = ArrayList<String>()
        getNameCapList(CharArray(target.length), 0, target, res)
        return res
    }

    private fun getNameCapList(
        capList: CharArray, currentIndex: Int, target: String, result: ArrayList<String>
    ) {
        if (currentIndex == target.length - 1) {
            val arrayOrNull = PinyinHelper.toHanyuPinyinStringArray(target[currentIndex])
            if (arrayOrNull == null) {
                capList[currentIndex] = target[currentIndex]
                result.add(String(capList))
            } else {
                val arrayOrNullCharArray = arrayOrNull.map { e -> e[0] }.distinct().toCharArray()
                for (item in arrayOrNullCharArray) {
                    capList[currentIndex] = item
                    result.add(String(capList))
                }
            }

        } else {
            val arrayOrNull = PinyinHelper.toHanyuPinyinStringArray(target[currentIndex])
            if (arrayOrNull == null) {
                val arr = capList.copyOf()
                arr[currentIndex] = target[currentIndex]
                val newIndex = currentIndex + 1
                getNameCapList(arr, newIndex, target, result)
            } else {
                val arrayOrNullCharArray = arrayOrNull.map { e -> e[0] }.distinct().toCharArray()
                for (item in arrayOrNullCharArray) {
                    val arr = capList.copyOf()
                    arr[currentIndex] = item
                    val newIndex = currentIndex + 1
                    getNameCapList(arr, newIndex, target, result)
                }
            }

        }
    }

    private fun getNameCapListPinyinAll(
        fullList: Array<String>, currentIndex: Int, target: String, result: ArrayList<String>
    ) {
        if (currentIndex == target.length - 1) {
            val arrayOrNull = PinyinHelper.toHanyuPinyinStringArray(target[currentIndex])
            if (arrayOrNull == null) {
                fullList[currentIndex] = target[currentIndex].toString()
                result.add(fullList.joinToString(""))
            } else {
                val arrayDis = arrayOrNull.map { e -> e.substring(0, e.length - 1) }.distinct()
                for (item in arrayDis) {
                    fullList[currentIndex] = item
                    result.add(fullList.joinToString(""))
                }
            }

        } else {
            val arrayOrNull = PinyinHelper.toHanyuPinyinStringArray(target[currentIndex])
            if (arrayOrNull == null) {
                val arr = fullList.copyOf()
                arr[currentIndex] = target[currentIndex].toString()
                val newIndex = currentIndex + 1
                getNameCapListPinyinAll(arr, newIndex, target, result)
            } else {
                val arrayDis = arrayOrNull.map { e -> e.substring(0, e.length - 1) }.distinct()
                for (item in arrayDis) {
                    val arr = fullList.copyOf()
                    arr[currentIndex] = item
                    val newIndex = currentIndex + 1
                    getNameCapListPinyinAll(arr, newIndex, target, result)
                }
            }

        }
    }
}