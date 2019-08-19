package com.anupkunwar.retrofitcalladapter

import okhttp3.Request
import retrofit2.*
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.Executor


interface MyCallback<T> {
    //for 200...300 error code
    fun success(response: Response<T>)

    //for 401 response
    fun unauthenticated(response: Response<*>)

    //for 400..499 response except 401
    fun clientError(response: Response<*>)

    // for 500..599 response
    fun serverError(response: Response<*>)

    //for network error while making the call
    fun networkError(e: IOException)

    //for unexpected error
    fun unexpectedError(t: Throwable)
}

interface MyCall<T> {
    fun cancel()
    fun enqueue(callback: MyCallback<T>)
    fun clone(): MyCall<T>
    fun isExecuted(): Boolean
    fun isCancelled(): Boolean
    fun request(): Request
    @Throws(
        IOException::class,
        UnauthenticatedException::class,
        ClientException::class,
        ServerException::class
    )
    fun execute(): Response<T>
}

class ErrorHandlingCallAdapterFactory : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (getRawType((returnType)) != MyCall::class.java) {
            return null
        }
        if (returnType !is ParameterizedType) {
            throw IllegalStateException("Mycall must have generic type (e.g) MyCall<ResponseBody>")
        }

        val responseType = getParameterUpperBound(0, returnType)
        val callbackExecutor = retrofit.callbackExecutor()
        return ErrorHandlingCallAdapter(responseType, callbackExecutor)
    }

}

private class ErrorHandlingCallAdapter(private val responseType: Type, private val callbackExecutor: Executor?) :
    CallAdapter<R, MyCall<R>> {

    override fun adapt(call: Call<R>): MyCall<R> {
        return MyCallAdapter(call, callbackExecutor)
    }

    override fun responseType(): Type {
        return responseType
    }

}


private class MyCallAdapter<T>(private val call: Call<T>, private val callbackExecutor: Executor?) : MyCall<T> {
    override fun execute(): Response<T> {
        val response: Response<T> = call.execute()
        when (response.code()) {
            401 -> throw UnauthenticatedException(response)
            in 400..499 -> throw ClientException(response)
            in 500..599 -> throw ServerException(response)
        }
        return response

    }

    override fun request(): Request {
        return call.request()
    }

    override fun isExecuted(): Boolean {
        return call.isExecuted
    }

    override fun cancel() {
        call.cancel()
    }

    override fun isCancelled(): Boolean {
        return call.isCanceled
    }

    override fun enqueue(callback: MyCallback<T>) {
        call.enqueue(object : Callback<T> {
            override fun onFailure(call: Call<T>, t: Throwable) {
                if (t is IOException) {
                    callback.networkError(t)
                } else {
                    callback.unexpectedError(t)
                }
            }

            override fun onResponse(call: Call<T>, response: Response<T>) {
                val code = response.code()
                if (callbackExecutor != null) {
                    callbackExecutor.execute {
                        doCallback(callback, response, code)
                    }
                } else {
                    doCallback(callback, response, code)
                }


            }

        })
    }

    private fun <T> doCallback(callback: MyCallback<T>, response: Response<T>, code: Int) {
        when (code) {
            in 200..299 -> callback.success(response)
            401 -> callback.unauthenticated(response)
            in 400..499 -> callback.clientError(response)
            in 500..599 -> callback.serverError(response)
            else -> callback.unexpectedError(RuntimeException("Unexpected response $response"))
        }
    }


    override fun clone(): MyCall<T> {
        return MyCallAdapter(call.clone(), callbackExecutor)
    }
}


class UnauthenticatedException(val response: Response<*>) : Exception()
class ClientException(val response: Response<*>) : Exception()
class ServerException(val response: Response<*>) : Exception()

