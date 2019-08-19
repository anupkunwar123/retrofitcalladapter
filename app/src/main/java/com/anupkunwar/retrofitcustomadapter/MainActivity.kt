package com.anupkunwar.retrofitcustomadapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.anupkunwar.retrofitcalladapter.ErrorHandlingCallAdapterFactory
import com.anupkunwar.retrofitcalladapter.MyCall
import com.anupkunwar.retrofitcalladapter.MyCallback
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addCallAdapterFactory(ErrorHandlingCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val loginService = retrofit.create(LoginService::class.java)
        val request = loginService.getToDoList()

        val adapter = Adapter()
        recyclerView.adapter = adapter

        request.enqueue(object : MyCallback<List<ToDo>> {
            override fun success(response: Response<List<ToDo>>) {
                //You may want to use live data to avoid memory leak here
                //done for simplicity sake
                adapter.list = response.body()
            }


            override fun unauthenticated(response: Response<*>) {
                print(response)
            }

            override fun clientError(response: Response<*>) {
                print(response)
            }

            override fun serverError(response: Response<*>) {
                print(response)
            }

            override fun networkError(e: IOException) {
                print(e)
            }

            override fun unexpectedError(t: Throwable) {
                print(t)
            }

        })

        Thread {
            try {
                //inside there you can execute the request and catch the relevant exception
                val response = request.execute()
                print(response.body())
            } catch (e: Exception) {
                print(e)
            }

        }.start()
    }

    class Adapter : RecyclerView.Adapter<ToDoViewHolder>() {

        var list: List<ToDo>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoViewHolder {
            return ToDoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false))
        }

        override fun getItemCount(): Int {
            return list?.size ?: 0
        }

        override fun onBindViewHolder(holder: ToDoViewHolder, position: Int) {
            holder.bindItem(list!![position])
        }

    }


    class ToDoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textView)

        fun bindItem(toDo: ToDo) {
            textView.text = toDo.title
        }

    }
}

interface LoginService {
    @GET("todos")
    fun getToDoList(): MyCall<List<ToDo>>

}

data class ToDo(val userId: Long, val id: Long, val title: String, val completed: Boolean)



