package com.aistra.hail.utils

import net.sourceforge.pinyin4j.PinyinHelper
import java.util.Locale

/**中文拼音搜索类*/
object PinyinSearch {
    /**
     * 分别使用首字母和全拼进行匹配，满足条件之一就返回true,
     * 如果当前语言不是中文就直接返回false
     * @param textToSearch 需要匹配的字符串
     * @param textInput 用户输入
     * */
    fun searchPinyinAll(textToSearch: String?, textInput: String): Boolean {
        if (textToSearch == null) {
            return false
        }
        val language = Locale.getDefault().language
        return if (language.equals(Locale.CHINESE.language)) {
            searchCap(textToSearch, textInput) || searchAllSpell(textToSearch, textInput)
        } else {
            false
        }
    }

    /**
     * 根据首字母进行搜索
     * 比如搜索”计算器“ 只需要输入 ”jsq“
     * */
    private fun searchCap(appName: String, pinyinCap: String): Boolean {
        if (pinyinCap.length < 80) {
            for (index in getNameStringList(appName)) {
                if (index.contains(pinyinCap, true)) {
                    return true
                }
            }
            return false
        } else {
            return false
        }
    }

    /**
     * 根据全部拼音进行搜索
     * 比如搜索”计算器“ 只需要输入 "jisuanqi"
     * */
    private fun searchAllSpell(appName: String, pinyinAll: String): Boolean {
        if (pinyinAll.length < 30) {
            for (index in getNameStringPinyinAll(appName)) {
                if (index.contains(pinyinAll, true)) {
                    return true
                }
            }
            return false
        } else {
            return false
        }
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