package com.ljyh.mei.data.model

data class UserData(
    val id: Long = -1L,
    val nickname: String = "",
    val avatarUrl: String = ""
){
    val isVisitor = id == -1L

    companion object {
        // 游客模式
        val VISITOR = UserData()
    }
}