package athena.communications

import okhttp3.*

class CommReq {

    companion object {

        @JvmStatic
        private val JSON: MediaType? = MediaType.get("application/json; charset=utf-8")

        @JvmStatic
        private val client: OkHttpClient? = OkHttpClient()

        @JvmStatic
        fun commonPostJsonBody(urlString: String?, jsonBody: String, headersRaw: Map<String, String>) : String {
            val body: RequestBody = RequestBody.create(JSON!!, jsonBody)
            try {
                val req: Request = when (headersRaw.isNullOrEmpty()) {
                    true -> {
                        Request.Builder().url(urlString!!).post(body).build()
                    }
                    false -> {
                        val h: Headers = Headers.of(headersRaw)
                        Request.Builder().url(urlString!!).headers(h).post(body).build()
                    }
                }
                val response: Response? = client!!.newCall(req).execute()
                return response!!.body()!!.string()
            } catch (ex: Exception) {
                throw ex
            }
        }

        @JvmStatic
        fun commonGetWithQuery(url: String?, params: List<QueryParam>, headersRaw: Map<String, String>): String {
            if (!params.isNullOrEmpty()) {
                var urlFin = url.plus("?")
                var c = 0
                for (item in params) {
                    urlFin = urlFin.plus(item.name).plus("=").plus(item.value)
                    if (c < params.size - 1) {
                        urlFin = urlFin.plus("&")
                    }
                    c = c.inc()
                }
                val req: Request = when (headersRaw.isNullOrEmpty()) {
                    true -> {
                        Request.Builder().url(url!!).build()
                    }
                    false -> {
                        val h: Headers = Headers.of(headersRaw)
                        Request.Builder().url(url!!).headers(h).build()
                    }
                }
                return try {
                    val response: Response? = client!!.newCall(req).execute()
                    response!!.body()!!.string()
                } catch (ex: Exception) {
                    throw ex
                }
            }
            return ""
        }

        @JvmStatic
        fun commonGetWithPathParam(url: String?, headersRaw: Map<String, String>): String {
            val req: Request = when (headersRaw.isNullOrEmpty()) {
                true -> {
                    Request.Builder().url(url!!).build()
                }
                false -> {
                    val h: Headers = Headers.of(headersRaw)
                    Request.Builder().url(url!!).headers(h).build()
                }
            }
            return try {
                val response: Response? = client!!.newCall(req).execute()
                response!!.body()!!.string()
            } catch (ex: Exception) {
                throw ex
            }
        }
    }
}

data class QueryParam (val name: String, val value: String)
