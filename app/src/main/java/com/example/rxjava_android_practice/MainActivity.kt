package com.example.rxjava_android_practice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.TelephonyCallback
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.example.rxjava_android_practice.databinding.ActivityMainBinding
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mDisposable : Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        val view = binding.root

        mDisposable = getObservable()
            .debounce(500, TimeUnit.MILLISECONDS)
            .filter{s -> s.isNotEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(getObserver())


        setContentView(view)
    }

    private fun getObservable() : Observable<CharSequence> {
        return Observable.create { emitter ->  binding.edittext.addTextChangedListener(
            object: TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    if (p0 != null) {
                        emitter.onNext(p0)
                    }
                }

                override fun afterTextChanged(p0: Editable?) {
                }
            }
        )}
    }

    private fun getObserver() : DisposableObserver<CharSequence> {
        return object : DisposableObserver<CharSequence>() {
            override fun onNext(word : CharSequence){
                Log.i("testtest", "onNext: $word")
            }
            override fun onComplete(){}
            override fun onError(e : Throwable){}
        }
    }

    override fun onDestroy() {
        mDisposable.dispose()
        super.onDestroy()
    }
}