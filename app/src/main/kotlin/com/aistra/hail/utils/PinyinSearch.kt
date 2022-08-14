package com.aistra.hail.utils

import net.sourceforge.pinyin4j.PinyinHelper

object PinyinSearch {
    public fun searchCap(appName: String, pinyinCap: String): Boolean {
        if (pinyinCap.length < 80) {
            for (index in getNameStringList(appName)) {
                if (index.contains(pinyinCap, true)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public fun searchAllSpell(appName: String, pinyinAll: String): Boolean {
        if (pinyinAll.length < 30) {
            for (index in getNameStringPinyinAll(appName)) {
                if (index.contains(pinyinAll, true)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    fun getNameStringPinyinAll(target: String): ArrayList<String> {
        val res = ArrayList<String>();
        getNameCapListPinyinAll(Array<String>(target.length, { "" }), 0, target, res);
        return res;
    }

    fun getNameStringList(target: String): ArrayList<String> {
        val res = ArrayList<String>();
        getNameCapList(CharArray(target.length), 0, target, res);
        return res;
    }

    fun getNameCapList(
        capList: CharArray,
        currentIndex: Int,
        target: String,
        result: ArrayList<String>
    ) {
        if (currentIndex == target.length - 1) {
            val arrayOrNull = PinyinHelper.toHanyuPinyinStringArray(target[currentIndex]);
            if (arrayOrNull == null) {
                capList[currentIndex] = target[currentIndex];
                result.add(String(capList));
            } else {
                val arrayOrNullCharArray = arrayOrNull.map { e -> e[0] }.distinct().toCharArray()
                for (item in arrayOrNullCharArray) {
                    capList[currentIndex] = item;
                    result.add(String(capList));
                }
            }

        } else {
            val arrayOrNull = PinyinHelper.toHanyuPinyinStringArray(target[currentIndex]);
            if (arrayOrNull == null) {
                val arr = capList.copyOf()
                arr[currentIndex] = target[currentIndex];
                val newindex = currentIndex + 1;
                getNameCapList(arr, newindex, target, result)
            } else {
                val arrayOrNullCharArray = arrayOrNull.map { e -> e[0] }.distinct().toCharArray()
                for (item in arrayOrNullCharArray) {
                    val arr = capList.copyOf()
                    arr[currentIndex] = item;
                    val newindex = currentIndex + 1;
                    getNameCapList(arr, newindex, target, result)
                }
            }

        }
    }

    fun getNameCapListPinyinAll(
        fullList: Array<String>,
        currentIndex: Int,
        target: String,
        result: ArrayList<String>
    ) {
        if (currentIndex == target.length - 1) {
            val arrayOrNull = PinyinHelper.toHanyuPinyinStringArray(target[currentIndex]);
            if (arrayOrNull == null) {
                fullList[currentIndex] = target[currentIndex].toString();
                result.add(fullList.joinToString(""));
            } else {
                val arrayDis = arrayOrNull.map { e -> e.substring(0, e.length - 1) }.distinct()
                for (item in arrayDis) {
                    fullList[currentIndex] = item;
                    result.add(fullList.joinToString(""));
                }
            }

        } else {
            val arrayOrNull = PinyinHelper.toHanyuPinyinStringArray(target[currentIndex]);
            if (arrayOrNull == null) {
                val arr = fullList.copyOf()
                arr[currentIndex] = target[currentIndex].toString();
                val newindex = currentIndex + 1;
                getNameCapListPinyinAll(arr, newindex, target, result)
            } else {
                val arrayDis = arrayOrNull.map { e -> e.substring(0, e.length - 1) }.distinct()
                for (item in arrayDis) {
                    val arr = fullList.copyOf()
                    arr[currentIndex] = item;
                    val newindex = currentIndex + 1;
                    getNameCapListPinyinAll(arr, newindex, target, result)
                }
            }

        }
    }
}