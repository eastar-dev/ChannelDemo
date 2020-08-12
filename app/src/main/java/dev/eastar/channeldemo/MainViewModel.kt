package dev.eastar.channeldemo

import android.log.Log
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
//     @ExperimentalCoroutinesApi
//    private val users = MutableStateFlow<Resource<List<ApiUser>>>(Resource.loading(null))
    private val sendChannel by lazy { Channel<Int>() }
    val channel: ReceiveChannel<Int> = sendChannel
//    val livedate = sendChannel.consumeAsFlow().asLiveData()
//    val livedate = sendChannel.receiveAsFlow().asLiveData()
//    val startMain = sendChannel.receiveAsFlow().asLiveData()

    fun onClicked(v: View) = viewModelScope.launch {
        Log.e(v)
        kotlin.runCatching {
            val shift = (v as TextView).text.toString().toIntOrNull() ?: 0
            Log.e(shift, 0x01 shl shift - 1)
            sendChannel.send(0x01 shl shift - 1)
        }.getOrThrow()
    }
}