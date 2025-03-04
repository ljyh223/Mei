package com.ljyh.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.music.data.model.UserAccount
import com.ljyh.music.data.model.room.QQSong
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.repository.ShareRepository
import com.ljyh.music.data.repository.UserRepository
import com.ljyh.music.di.QQSongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: ShareRepository,
    private val userRepository: UserRepository,
) : ViewModel() {


    private val _userAccount = MutableStateFlow<Resource<UserAccount>>(Resource.Loading)
    val userAccount: StateFlow<Resource<UserAccount>> = _userAccount

    fun getUserAccount(){
        viewModelScope.launch {
            _userAccount.value = Resource.Loading
            _userAccount.value = userRepository.getUserAccount()
        }
    }


}