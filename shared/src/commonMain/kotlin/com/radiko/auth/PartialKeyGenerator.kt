package com.radiko.auth

import com.radiko.crypto.RadikoKeyStore
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object PartialKeyGenerator {
    @OptIn(ExperimentalEncodingApi::class)
    private val lenientBase64 = Base64.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    @OptIn(ExperimentalEncodingApi::class)
    fun generate(offset: Int, length: Int): String {
        require(offset >= 0) { "offset must be >= 0" }
        require(length > 0) { "length must be > 0" }

        val decodedKey = lenientBase64.decode(RadikoKeyStore.FULL_KEY_BASE64)
        require(offset + length <= decodedKey.size) {
            "Requested key slice exceeds the available key size"
        }
        val partial = decodedKey.copyOfRange(offset, offset + length)
        // 使用标准 Base64（带 = 填充），与 JS btoa() 输出一致，服务端要求此格式
        return Base64.encode(partial)
    }
}

