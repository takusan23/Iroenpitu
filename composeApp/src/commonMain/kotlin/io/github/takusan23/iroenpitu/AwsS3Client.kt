package io.github.takusan23.iroenpitu

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** S3 クライアント */
@OptIn(ExperimentalTime::class)
object AwsS3Client {

    // Kotlin Multiplatform HTTP Client
    private val httpClient = HttpClient()

    // xml パーサーの代わりに正規表現
    private val regexKey = "<Key>(.*?)</Key>".toRegex()
    private val regexLastModified = "<LastModified>(.*?)</LastModified>".toRegex()

    /**
     * バケット内のオブジェクト一覧を取得する
     *
     * @param bucketName バケット名
     */
    suspend fun getObjectList(bucketName: String): List<ListObject> {
        // 認証情報を読み出す
        val (region, accessKey, secretAccessKey) = loadAwsCredential() ?: return emptyList()

        val now = Clock.System.now()
        val url = "https://s3.$region.amazonaws.com/$bucketName/?list-type=2"
        val amzDateString = now.formatAmzDateString()
        val yyyyMMddString = now.formatYearMonthDayDateString()

        // 署名を作成
        val requestHeader = hashMapOf(
            "x-amz-date" to amzDateString,
            "host" to "s3.$region.amazonaws.com"
        )
        val signature = generateAwsSign(
            url = url,
            httpMethod = "GET",
            contentType = null,
            region = region,
            service = "s3",
            amzDateString = amzDateString,
            yyyyMMddString = yyyyMMddString,
            secretAccessKey = secretAccessKey,
            accessKey = accessKey,
            requestHeader = requestHeader
        )

        // レスポンス xml を取得
        val response = httpClient.get {
            url(url)
            headers {
                // 署名をリクエストヘッダーにつける
                requestHeader.forEach { (name, value) ->
                    this[name] = value
                }
                this["Authorization"] = signature
            }
        }

        // XML パーサー入れるまでもないので、正規表現で戦う、、、
        val responseXml = response.bodyAsText()
        val keyList = regexKey.findAll(responseXml).toList().map { it.groupValues[1] }
        val lastModifiedList = regexLastModified.findAll(responseXml).toList().map { it.groupValues[1] }

        // data class
        // 同じ数ずつあるはず
        return keyList.indices.map { index ->
            ListObject(
                key = keyList[index],
                lastModified = lastModifiedList[index]
            )
        }.sortedByDescending { it.lastModified }
    }

    /**
     * S3 バケットにデータを投稿する
     *
     * @param bucketName バケット名
     * @param key オブジェクトのキー（名前）
     * @param byteArray バイナリデータ
     */
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun putObject(
        bucketName: String,
        key: String,
        byteArray: ByteArray
    ): Boolean {
        // 認証情報を読み出す
        val (region, accessKey, secretAccessKey) = loadAwsCredential() ?: return false

        val now = Clock.System.now()
        val url = "https://s3.$region.amazonaws.com/$bucketName/$key"
        val amzDateString = now.formatAmzDateString()
        val yyyyMMddString = now.formatYearMonthDayDateString()

        // 署名を作成
        val requestHeader = hashMapOf(
            "x-amz-date" to amzDateString,
            "host" to "s3.$region.amazonaws.com"
        )
        val signature = generateAwsSign(
            url = url,
            httpMethod = "PUT",
            contentType = null,
            region = region,
            service = "s3",
            amzDateString = amzDateString,
            yyyyMMddString = yyyyMMddString,
            secretAccessKey = secretAccessKey,
            accessKey = accessKey,
            requestHeader = requestHeader,
            payloadSha256 = byteArray.sha256().toHexString()
        )

        // PutObject する
        val response = httpClient.put {
            url(url)
            headers {
                requestHeader.forEach { (name, value) ->
                    this[name] = value
                }
                this["Authorization"] = signature
            }
            setBody(byteArray)
        }
        return response.status == HttpStatusCode.OK
    }

    /**
     * オブジェクトを削除する
     *
     * @param bucketName バケット名
     * @param key オブジェクトのキー
     */
    suspend fun deleteObject(
        bucketName: String,
        key: String
    ): Boolean {
        // 認証情報を読み出す
        val (region, accessKey, secretAccessKey) = loadAwsCredential() ?: return false

        val now = Clock.System.now()
        val url = "https://s3.$region.amazonaws.com/$bucketName/$key"
        val amzDateString = now.formatAmzDateString()
        val yyyyMMddString = now.formatYearMonthDayDateString()

        // 署名を作成
        val requestHeader = hashMapOf(
            "x-amz-date" to amzDateString,
            "host" to "s3.$region.amazonaws.com"
        )
        val signature = generateAwsSign(
            url = url,
            httpMethod = "DELETE",
            contentType = null,
            region = region,
            service = "s3",
            amzDateString = amzDateString,
            yyyyMMddString = yyyyMMddString,
            secretAccessKey = secretAccessKey,
            accessKey = accessKey,
            requestHeader = requestHeader
        )

        // 削除する
        val response = httpClient.delete {
            url(url)
            headers {
                // 署名をリクエストヘッダーにつける
                requestHeader.forEach { (name, value) ->
                    this[name] = value
                }
                this["Authorization"] = signature
            }
        }
        // 204 No Content を返す
        return response.status == HttpStatusCode.NoContent
    }

    /**
     * [preference]から AWS の認証情報を得る。
     * @return リージョン、アクセスキー、シークレットアクセスキー。ない場合は null
     */
    private suspend fun loadAwsCredential(): Triple<String, String, String>? {
        val map = preference.load()
        return Triple(
            first = map[Preference.KEY_REGION] ?: return null,
            second = map[Preference.KEY_ACCESS_KEY] ?: return null,
            third = map[Preference.KEY_SECRET_ACCESS_LEY] ?: return null
        )
    }

    data class ListObject(
        val key: String,
        val lastModified: String
    )
}
