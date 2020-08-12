package dev.eastar.channeldemo

import android.log.Log
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.jakewharton.rxbinding2.widget.RxTextView
import dev.eastar.base.CActivity
import dev.eastar.channeldemo.databinding.ActivityMainBinding
import kotlinx.coroutines.delay

class MainActivity : CActivity() {

    private lateinit var bb: ActivityMainBinding
    val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bb.root)
        bb.vm = vm
        bb.lifecycleOwner = this

        onloadOnce()
    }

    private fun onloadOnce() {
        RxTextView.textChangeEvents(bb.edit)
            .subscribe {
                val result = it.text().toString().toIntOrNull() ?: 0
                onResult(result)
            }.autoDispose()

        lifecycleScope.launchWhenStarted {
            while (true) {
                delay(0)
                val result = vm.channel.receive()
                Log.i(result)
                onResult(result)
            }
        }

//        vm.livedate.observeForever {
//            onResult(it)
//        }
    }

    fun onResult(result: Int) {
        //@formatter:off
        if (result and 0x01 == 0x01) toggleFragment(bb.fragmentContainerView1, Fragment1::class.java)
        if (result and 0x02 == 0x02) toggleFragment(bb.fragmentContainerView2, Fragment2::class.java)
        if (result and 0x04 == 0x04) toggleFragment(bb.fragmentContainerView3, Fragment3::class.java)
        if (result and 0x08 == 0x08) toggleFragment(bb.fragmentContainerView4, Fragment4::class.java)
        //@formatter:on
    }

    private fun toggleFragment(view: View, clz: Class<out Fragment>) {
        supportFragmentManager.commit(true) {
            val fr = supportFragmentManager.findFragmentByTag(clz.simpleName)
            if (fr == null)
                add(view.id, clz.newInstance(), clz.simpleName)
            else
                remove(fr)
        }

    }
}