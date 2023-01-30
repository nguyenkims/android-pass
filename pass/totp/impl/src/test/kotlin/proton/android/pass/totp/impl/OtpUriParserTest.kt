package proton.android.pass.totp.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import proton.android.pass.common.api.None
import proton.android.pass.common.api.Result
import proton.android.pass.common.api.some
import proton.android.pass.totp.api.MalformedOtpUri
import proton.android.pass.totp.api.TotpAlgorithm
import proton.android.pass.totp.api.TotpDigits
import proton.android.pass.totp.api.TotpSpec

class OtpUriParserTest {

    @Test
    fun `can parse uri with all parameters`() {
        val input =
            "otpauth://totp/thisisalabel?secret=somerandomsecret&issuer=theissuer&algorithm=SHA256&digits=8&period=24"
        val expected = TotpSpec(
            label = "thisisalabel",
            secret = "somerandomsecret",
            issuer = "theissuer".some(),
            algorithm = TotpAlgorithm.Sha256,
            digits = TotpDigits.Eight,
            validPeriodSeconds = 24
        )

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Success(expected))
    }

    @Test
    fun `can parse reference uri`() {
        val input =
            "otpauth://totp/thisisthelabel?secret=thisisthesecret&algorithm=SHA1&digits=6&period=10"
        val expected = TotpSpec(
            label = "thisisthelabel",
            secret = "thisisthesecret",
            issuer = None,
            algorithm = TotpAlgorithm.Sha1,
            digits = TotpDigits.Six,
            validPeriodSeconds = 10
        )

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Success(expected))
    }

    @Test
    fun `can add all default parameters`() {
        val input = "otpauth://totp/thisisthelabel?secret=thisisthesecret"
        val expected = TotpSpec(
            label = "thisisthelabel",
            secret = "thisisthesecret",
            issuer = None,
            algorithm = TotpAlgorithm.Sha1,
            digits = TotpDigits.Six,
            validPeriodSeconds = 30
        )

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Success(expected))
    }

    @Test
    fun `can detect missing scheme`() {
        val input = "totp/thisisthelabel?secret=thisisthesecret"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.MissingScheme))
    }

    @Test
    fun `can detect invalid scheme`() {
        val input = "wrong://totp/thisisthelabel?secret=thisisthesecret"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.InvalidScheme("wrong")))
    }

    @Test
    fun `can detect missing host`() {
        val input = "otpauth:///thisisthelabel?secret=thisisthesecret"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.MissingHost))
    }

    @Test
    fun `can detect invalid host`() {
        val input = "otpauth://invalid/thisisthelabel?secret=thisisthesecret"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.InvalidHost("invalid")))
    }

    @Test
    fun `can detect missing label`() {
        val input = "otpauth://totp?secret=thisisthesecret"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.MissingLabel))
    }

    @Test
    fun `can detect missing secret`() {
        val input = "otpauth://totp/thisisthelabel?digits=6"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.MissingSecret))
    }

    @Test
    fun `can detect invalid algorithm`() {
        val input = "otpauth://totp/thisisthelabel?secret=thisisthesecret&algorithm=wrong"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.InvalidAlgorithm("wrong")))
    }

    @Test
    fun `can detect invalid digit count (number)`() {
        val input = "otpauth://totp/thisisthelabel?secret=thisisthesecret&digits=300"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.InvalidDigitCount("300")))
    }

    @Test
    fun `can detect invalid digit count (not number)`() {
        val input = "otpauth://totp/thisisthelabel?secret=thisisthesecret&digits=notanumber"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.InvalidDigitCount("notanumber")))
    }

    @Test
    fun `can detect invalid period`() {
        val input = "otpauth://totp/thisisthelabel?secret=thisisthesecret&period=notanumber"

        val parsed = OtpUriParser.parse(input)
        assertThat(parsed).isEqualTo(Result.Error(MalformedOtpUri.InvalidValidity("notanumber")))
    }
}