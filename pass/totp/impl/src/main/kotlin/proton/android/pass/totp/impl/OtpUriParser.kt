package proton.android.pass.totp.impl

import proton.android.pass.common.api.Option
import proton.android.pass.common.api.Result
import proton.android.pass.common.api.toOption
import proton.android.pass.totp.api.MalformedOtpUri
import proton.android.pass.totp.api.TotpAlgorithm
import proton.android.pass.totp.api.TotpDigits
import proton.android.pass.totp.api.TotpSpec
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

object OtpUriParser {

    private const val SCHEME = "otpauth"
    private const val HOST = "totp"

    private const val QUERY_SECRET = "secret"
    private const val QUERY_ISSUER = "issuer"
    private const val QUERY_ALGORITHM = "algorithm"
    private const val QUERY_DIGITS = "digits"
    private const val QUERY_PERIOD = "period"

    private const val SHA1_ALGORITHM = "SHA1"
    private const val SHA256_ALGORITHM = "SHA256"
    private const val SHA512_ALGORITHM = "SHA512"

    private const val DEFAULT_ALGORITHM = SHA1_ALGORITHM
    private const val DEFAULT_DIGITS = "6"
    private const val DEFAULT_PERIOD = "30"

    fun parse(input: String): Result<TotpSpec> =
        try {
            val parsed = URI(input)
            extractFields(parsed)
        } catch (e: MalformedOtpUri) {
            Result.Error(e)
        } catch (e: URISyntaxException) {
            Result.Error(MalformedOtpUri.InvalidUri(e))
        }

    @Suppress("ComplexMethod", "ReturnCount")
    private fun extractFields(parsed: URI): Result<TotpSpec> {
        validateScheme(parsed)
        validateHost(parsed)

        val label = extractLabel(parsed)
        val parsedQuery = splitQuery(parsed.query)

        val secret = extractSecret(parsedQuery)
        val issuer = extractIssuer(parsedQuery)
        val algorithm = extractAlgorithm(parsedQuery)
        val digits = extractDigits(parsedQuery)
        val period = extractPeriod(parsedQuery)

        val spec = TotpSpec(
            secret = secret,
            label = label,
            issuer = issuer,
            algorithm = algorithm,
            digits = digits,
            validPeriodSeconds = period
        )
        return Result.Success(spec)
    }

    private fun validateScheme(parsed: URI) {
        val scheme = parsed.scheme
        if (scheme == null || scheme.isEmpty()) throw MalformedOtpUri.MissingScheme
        if (scheme != SCHEME) throw MalformedOtpUri.InvalidScheme(parsed.scheme)
    }

    private fun validateHost(parsed: URI) {
        val host = parsed.host
        if (host == null || host.isEmpty()) throw MalformedOtpUri.MissingHost
        if (host != HOST) throw MalformedOtpUri.InvalidHost(parsed.host)
    }

    private fun extractLabel(parsed: URI): String {
        if (parsed.path.isEmpty()) throw MalformedOtpUri.MissingLabel
        return parsed.path.removePrefix("/")
    }

    private fun extractSecret(query: Map<String, List<String?>>): String {
        val secretList = query[QUERY_SECRET] ?: throw MalformedOtpUri.MissingSecret
        val secret = secretList.firstOrNull() ?: throw MalformedOtpUri.MissingSecret
        return secret
    }

    private fun extractIssuer(query: Map<String, List<String?>>): Option<String> =
        query[QUERY_ISSUER]?.firstOrNull().toOption()

    private fun extractAlgorithm(query: Map<String, List<String?>>): TotpAlgorithm {
        val algorithmString = query[QUERY_ALGORITHM]?.firstOrNull() ?: DEFAULT_ALGORITHM
        return when (algorithmString) {
            SHA1_ALGORITHM -> TotpAlgorithm.Sha1
            SHA256_ALGORITHM -> TotpAlgorithm.Sha256
            SHA512_ALGORITHM -> TotpAlgorithm.Sha512
            else -> throw MalformedOtpUri.InvalidAlgorithm(algorithmString)
        }
    }

    private fun extractDigits(query: Map<String, List<String?>>): TotpDigits {
        val digitsString = query[QUERY_DIGITS]?.firstOrNull() ?: DEFAULT_DIGITS
        return when (digitsString) {
            "6" -> TotpDigits.Six
            "8" -> TotpDigits.Eight
            else -> throw MalformedOtpUri.InvalidDigitCount(digitsString)
        }
    }

    private fun extractPeriod(query: Map<String, List<String?>>): Int {
        val periodString = query[QUERY_PERIOD]?.firstOrNull() ?: DEFAULT_PERIOD
        return periodString.toIntOrNull() ?: throw MalformedOtpUri.InvalidValidity(periodString)
    }

    private fun splitQuery(query: String): Map<String, List<String?>> {
        val queryPairs: MutableMap<String, MutableList<String?>> = mutableMapOf()
        val pairs = query.split("&".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            val key = if (idx > 0) URLDecoder.decode(
                pair.substring(0, idx),
                Charsets.UTF_8.name()
            ) else pair
            if (!queryPairs.containsKey(key)) {
                queryPairs[key] = mutableListOf()
            }
            val value = if (idx > 0 && pair.length > idx + 1) URLDecoder.decode(
                pair.substring(idx + 1),
                Charsets.UTF_8.name()
            ) else null
            queryPairs[key]!!.add(value)
        }
        return queryPairs
    }
}