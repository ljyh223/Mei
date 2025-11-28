package com.ljyh.mei.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.di.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel(){
    // 直接从 DAO 获取 Flow
    val historyList = repository.getHistoryStream()



    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }


}