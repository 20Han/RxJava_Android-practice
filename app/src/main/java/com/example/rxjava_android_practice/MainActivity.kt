package com.example.rxjava_android_practice

import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rxjava_android_practice.databinding.ActivityMainBinding
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val customAdapter = CustomAdapter()
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

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = customAdapter
            customAdapter.getItemPublishSubject().subscribe { s ->
                Toast.makeText(applicationContext, s.title, Toast.LENGTH_SHORT).show()
            }
        }

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

    override fun onStart() {
        super.onStart()

        getItemObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { item ->
                Log.d("testtest", "onResume: ${item.title}")
                customAdapter.updateItem(item)
                customAdapter.notifyDataSetChanged()
            }
    }

    private fun getItemObservable() : Observable<RecyclerItem> {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        return Observable.fromIterable(packageManager.queryIntentActivities(intent, 0))
            .sorted(ResolveInfo.DisplayNameComparator(packageManager))
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { item ->
                val image = item.activityInfo.loadIcon(packageManager)
                val title = item.activityInfo.loadLabel(packageManager).toString()
                RecyclerItem(image, title)
            }
    }
}

class RecyclerItem(val image: Drawable, val title: String) {}

class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var image : ImageView = itemView.findViewById(R.id.item_image)
    var title : TextView = itemView.findViewById(R.id.item_title)

    fun getClickObserver(item : RecyclerItem) : Observable<RecyclerItem> {
        return Observable.create{ emitter -> this.itemView.setOnClickListener{
            emitter.onNext(item)
        }}
    }
}

class CustomAdapter : RecyclerView.Adapter<CustomViewHolder>() {
    var mItems = ArrayList<RecyclerItem>()
    var mPublishSubject = PublishSubject.create<RecyclerItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)

        return CustomViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val item = mItems[position]
        holder.image.setImageDrawable(item.image)
        holder.title.text = item.title
        holder.getClickObserver(item).subscribe(mPublishSubject)
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun updateItems(items : ArrayList<RecyclerItem>) {
        mItems.addAll(items)
    }

    fun updateItem(item : RecyclerItem) {
        mItems.add(item)
    }

    fun getItemPublishSubject() : PublishSubject<RecyclerItem> {
        return mPublishSubject
    }
}