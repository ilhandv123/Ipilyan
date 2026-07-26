package com.ipilyan.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ipilyan.app.databinding.ActivityMainBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding

  private val fields = listOf(
    "ip_target" to "IP Target",
    "type" to "Type",
    "country" to "Country",
    "country_code" to "Country Code",
    "city" to "City",
    "continent" to "Continent",
    "continent_code" to "Continent Code",
    "region" to "Region",
    "region_code" to "Region Code",
    "latitude" to "Latitude",
    "longitude" to "Longitude",
    "maps" to "Maps",
    "is_eu" to "EU",
    "postal" to "Postal",
    "calling_code" to "Calling Code",
    "capital" to "Capital",
    "borders" to "Borders",
    "flag" to "Country Flag",
    "asn" to "ASN",
    "org" to "ORG",
    "isp" to "ISP",
    "domain" to "Domain",
    "tz_id" to "ID",
    "dst" to "DST",
    "offset" to "OFFSET",
    "utc" to "UTC",
    "current_time" to "Current Time",
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (application as App).checkCrashAndLaunch()
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    @Suppress("DEPRECATION")
    window.statusBarColor = Color.BLACK
    @Suppress("DEPRECATION")
    window.navigationBarColor = Color.BLACK
    if (Build.VERSION.SDK_INT >= 29) {
      @Suppress("DEPRECATION")
      window.isNavigationBarContrastEnforced = false
      @Suppress("DEPRECATION")
      window.isStatusBarContrastEnforced = false
    }
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.btnLookup.setOnClickListener { lookupIp() }
    binding.btnMyIp.setOnClickListener { lookupMyIp() }
    binding.etIp.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_SEARCH) { lookupIp(); true } else false
    }
  }

  private fun lookupMyIp() {
    binding.etIp.setText("")
    setStatus("DETECTING IP...")
    Thread {
      try {
        val ip = fetchJson("https://ipapi.co/json/").optString("ip", "").ifEmpty {
          fetchJson("http://ip-api.com/json/?fields=query").optString("query", "")
        }
        runOnUiThread {
          binding.etIp.setText(ip)
          if (ip.isNotEmpty()) { binding.etIp.setSelection(ip.length); lookupIp() }
          else setStatus("COULD NOT DETECT IP")
        }
      } catch (e: Exception) {
        runOnUiThread { setStatus("NETWORK ERROR") }
      }
    }.start()
  }

  private fun lookupIp() {
    val ip = binding.etIp.text.toString().trim()
    if (ip.isEmpty()) { setStatus("ENTER A VALID IP OR DOMAIN"); return }

    setStatus("FETCHING...")
    binding.resultsContainer.removeAllViews()
    binding.tvResultsHeader.visibility = View.GONE
    binding.divider2.visibility = View.GONE

    Thread {
      try {
        val json = fetchJson("https://ipapi.co/$ip/json/")
        runOnUiThread {
          if (!json.optBoolean("error", false)) showResults(json)
          else fallbackLookup(ip)
        }
      } catch (e: Exception) {
        runOnUiThread { fallbackLookup(ip) }
      }
    }.start()
  }

  private fun fallbackLookup(ip: String) {
    Thread {
      try {
        val json = fetchJson("http://ip-api.com/json/$ip")
        runOnUiThread {
          if (json.optString("status", "") == "success") showResultsFallback(json)
          else setStatus("COULD NOT FIND IP INFO")
        }
      } catch (e: Exception) {
        runOnUiThread { setStatus("NETWORK ERROR") }
      }
    }.start()
  }

  private fun showResults(json: JSONObject) {
    setStatus(null)
    binding.tvResultsHeader.visibility = View.VISIBLE
    binding.divider2.visibility = View.VISIBLE

    val cc = safe(json, "country_code")
    val tz = safe(json, "timezone")
    for ((key, _) in fields) {
      addRow(getLabel(key), extractIpapiCo(json, cc, tz, key), key)
    }
  }

  private fun showResultsFallback(json: JSONObject) {
    setStatus(null)
    binding.tvResultsHeader.visibility = View.VISIBLE
    binding.divider2.visibility = View.VISIBLE

    val cc = safe(json, "countryCode")
    val tz = safe(json, "timezone")
    for ((key, _) in fields) {
      addRow(getLabel(key), extractIpApi(json, cc, tz, key), key)
    }
  }

  private fun getLabel(key: String): String {
    return fields.firstOrNull { it.first == key }?.second ?: key
  }

  private fun extractIpapiCo(json: JSONObject, cc: String, tz: String, key: String): String {
    val v = when (key) {
      "ip_target" -> safe(json, "ip")
      "type" -> safe(json, "version")
      "country" -> safe(json, "country_name")
      "country_code" -> cc
      "city" -> safe(json, "city")
      "continent" -> codeToContinent(cc)
      "continent_code" -> safe(json, "continent_code")
      "region" -> safe(json, "region")
      "region_code" -> safe(json, "region_code")
      "latitude" -> fmtCoord(json, "latitude")
      "longitude" -> fmtCoord(json, "longitude")
      "maps" -> coordUrl(json, "latitude", "longitude")
      "is_eu" -> if (json.has("in_eu") && !json.isNull("in_eu")) bool(json, "in_eu") else "-"
      "postal" -> safe(json, "postal")
      "calling_code" -> safe(json, "country_calling_code")
      "capital" -> safe(json, "country_capital")
      "borders" -> "-"
      "flag" -> flagEmoji(cc)
      "asn" -> safe(json, "asn")
      "org" -> safe(json, "org")
      "isp" -> safe(json, "org")
      "domain" -> "-"
      "tz_id" -> tz
      "dst" -> "-"
      "offset" -> safe(json, "utc_offset")
      "utc" -> safe(json, "utc_offset")
      "current_time" -> currentTimeInTz(tz)
      else -> "-"
    }
    return v.ifEmpty { "-" }
  }

  private fun extractIpApi(json: JSONObject, cc: String, tz: String, key: String): String {
    val v = when (key) {
      "ip_target" -> safe(json, "query")
      "type" -> {
        val q = safe(json, "query")
        if (q.contains(':')) "IPv6" else if (q.contains('.')) "IPv4" else "-"
      }
      "country" -> safe(json, "country")
      "country_code" -> cc
      "city" -> safe(json, "city")
      "continent" -> codeToContinent(cc)
      "continent_code" -> codeToContinentCode(cc)
      "region" -> safe(json, "regionName")
      "region_code" -> safe(json, "region")
      "latitude" -> fmtCoord(json, "lat")
      "longitude" -> fmtCoord(json, "lon")
      "maps" -> coordUrl(json, "lat", "lon")
      "is_eu" -> if (isEuCountry(cc)) "true" else "false"
      "postal" -> safe(json, "zip")
      "calling_code" -> "-"
      "capital" -> "-"
      "borders" -> "-"
      "flag" -> flagEmoji(cc)
      "asn" -> safe(json, "as")
      "org" -> safe(json, "org")
      "isp" -> safe(json, "isp")
      "domain" -> "-"
      "tz_id" -> tz
      "dst" -> "-"
      "offset" -> "-"
      "utc" -> "-"
      "current_time" -> currentTimeInTz(tz)
      else -> "-"
    }
    return v.ifEmpty { "-" }
  }

  private fun currentTimeInTz(tz: String): String {
    if (tz == "-") return "-"
    return try {
      ZonedDateTime.now(ZoneId.of(tz)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (e: Exception) { "-" }
  }

  private fun safe(json: JSONObject, f: String): String {
    return if (json.has(f) && !json.isNull(f)) json.optString(f, "-") else "-"
  }

  private fun fmtCoord(json: JSONObject, f: String): String {
    return if (json.has(f) && !json.isNull(f)) {
      val d = json.optDouble(f, Double.NaN)
      if (!d.isNaN()) d.toString() else "-"
    } else "-"
  }

  private fun coordUrl(json: JSONObject, latF: String, lonF: String): String {
    val lat = json.optDouble(latF, Double.NaN)
    val lon = json.optDouble(lonF, Double.NaN)
    return if (json.has(latF) && json.has(lonF) && !lat.isNaN() && !lon.isNaN())
      "https://maps.google.com/?q=$lat,$lon" else "-"
  }

  private fun bool(json: JSONObject, f: String): String {
    return if (json.has(f) && !json.isNull(f)) json.optBoolean(f).toString() else "-"
  }

  private fun isEuCountry(cc: String): Boolean = cc in setOf(
    "AT","BE","BG","HR","CY","CZ","DK","EE","FI","FR","DE","GR","HU",
    "IE","IT","LV","LT","LU","MT","NL","PL","PT","RO","SK","SI","ES","SE"
  )

  private fun codeToContinent(cc: String): String = mapOf(
    "AF" to "Asia", "AL" to "Europe", "DZ" to "Africa", "AO" to "Africa",
    "AR" to "South America", "AM" to "Asia", "AU" to "Oceania", "AT" to "Europe",
    "AZ" to "Asia", "BD" to "Asia", "BY" to "Europe", "BE" to "Europe",
    "BZ" to "North America", "BJ" to "Africa", "BT" to "Asia", "BO" to "South America",
    "BA" to "Europe", "BW" to "Africa", "BR" to "South America", "BN" to "Asia",
    "BG" to "Europe", "BF" to "Africa", "BI" to "Africa", "KH" to "Asia",
    "CM" to "Africa", "CA" to "North America", "CV" to "Africa", "CF" to "Africa",
    "TD" to "Africa", "CL" to "South America", "CN" to "Asia", "CO" to "South America",
    "KM" to "Africa", "CG" to "Africa", "CD" to "Africa", "CR" to "North America",
    "HR" to "Europe", "CU" to "North America", "CY" to "Europe", "CZ" to "Europe",
    "DK" to "Europe", "DJ" to "Africa", "DO" to "North America", "EC" to "South America",
    "EG" to "Africa", "SV" to "North America", "GQ" to "Africa", "ER" to "Africa",
    "EE" to "Europe", "ET" to "Africa", "FJ" to "Oceania", "FI" to "Europe",
    "FR" to "Europe", "GA" to "Africa", "GM" to "Africa", "GE" to "Asia",
    "DE" to "Europe", "GH" to "Africa", "GR" to "Europe", "GT" to "North America",
    "GN" to "Africa", "GW" to "Africa", "GY" to "South America", "HT" to "North America",
    "HN" to "North America", "HU" to "Europe", "IS" to "Europe", "IN" to "Asia",
    "ID" to "Asia", "IR" to "Asia", "IQ" to "Asia", "IE" to "Europe",
    "IL" to "Asia", "IT" to "Europe", "CI" to "Africa", "JM" to "North America",
    "JP" to "Asia", "JO" to "Asia", "KZ" to "Asia", "KE" to "Africa",
    "KI" to "Oceania", "KP" to "Asia", "KR" to "Asia", "KW" to "Asia",
    "KG" to "Asia", "LA" to "Asia", "LV" to "Europe", "LB" to "Asia",
    "LS" to "Africa", "LR" to "Africa", "LY" to "Africa", "LI" to "Europe",
    "LT" to "Europe", "LU" to "Europe", "MG" to "Africa", "MW" to "Africa",
    "MY" to "Asia", "MV" to "Asia", "ML" to "Africa", "MT" to "Europe",
    "MH" to "Oceania", "MR" to "Africa", "MU" to "Africa", "MX" to "North America",
    "FM" to "Oceania", "MD" to "Europe", "MC" to "Europe", "MN" to "Asia",
    "ME" to "Europe", "MA" to "Africa", "MZ" to "Africa", "MM" to "Asia",
    "NA" to "Africa", "NP" to "Asia", "NL" to "Europe", "NZ" to "Oceania",
    "NI" to "North America", "NE" to "Africa", "NG" to "Africa", "MK" to "Europe",
    "NO" to "Europe", "OM" to "Asia", "PK" to "Asia", "PW" to "Oceania",
    "PS" to "Asia", "PA" to "North America", "PG" to "Oceania", "PY" to "South America",
    "PE" to "South America", "PH" to "Asia", "PL" to "Europe", "PT" to "Europe",
    "QA" to "Asia", "RO" to "Europe", "RU" to "Europe", "RW" to "Africa",
    "WS" to "Oceania", "SM" to "Europe", "ST" to "Africa", "SA" to "Asia",
    "SN" to "Africa", "RS" to "Europe", "SC" to "Africa", "SL" to "Africa",
    "SG" to "Asia", "SK" to "Europe", "SI" to "Europe", "SB" to "Oceania",
    "SO" to "Africa", "ZA" to "Africa", "SS" to "Africa", "ES" to "Europe",
    "LK" to "Asia", "SD" to "Africa", "SR" to "South America", "SE" to "Europe",
    "CH" to "Europe", "SY" to "Asia", "TW" to "Asia", "TJ" to "Asia",
    "TZ" to "Africa", "TH" to "Asia", "TL" to "Asia", "TG" to "Africa",
    "TO" to "Oceania", "TN" to "Africa", "TR" to "Europe", "TM" to "Asia",
    "TV" to "Oceania", "UG" to "Africa", "UA" to "Europe", "AE" to "Asia",
    "GB" to "Europe", "US" to "North America", "UY" to "South America",
    "UZ" to "Asia", "VU" to "Oceania", "VA" to "Europe", "VE" to "South America",
    "VN" to "Asia", "YE" to "Asia", "ZM" to "Africa", "ZW" to "Africa"
  )[cc] ?: "-"

  private fun codeToContinentCode(cc: String): String = mapOf(
    "AF" to "AS", "AL" to "EU", "DZ" to "AF", "AO" to "AF", "AR" to "SA",
    "AM" to "AS", "AU" to "OC", "AT" to "EU", "AZ" to "AS", "BD" to "AS",
    "BY" to "EU", "BE" to "EU", "BZ" to "NA", "BJ" to "AF", "BT" to "AS",
    "BO" to "SA", "BA" to "EU", "BW" to "AF", "BR" to "SA", "BN" to "AS",
    "BG" to "EU", "BF" to "AF", "BI" to "AF", "KH" to "AS", "CM" to "AF",
    "CA" to "NA", "CV" to "AF", "CF" to "AF", "TD" to "AF", "CL" to "SA",
    "CN" to "AS", "CO" to "SA", "KM" to "AF", "CG" to "AF", "CD" to "AF",
    "CR" to "NA", "HR" to "EU", "CU" to "NA", "CY" to "EU", "CZ" to "EU",
    "DK" to "EU", "DJ" to "AF", "DO" to "NA", "EC" to "SA", "EG" to "AF",
    "SV" to "NA", "GQ" to "AF", "ER" to "AF", "EE" to "EU", "ET" to "AF",
    "FJ" to "OC", "FI" to "EU", "FR" to "EU", "GA" to "AF", "GM" to "AF",
    "GE" to "AS", "DE" to "EU", "GH" to "AF", "GR" to "EU", "GT" to "NA",
    "GN" to "AF", "GW" to "AF", "GY" to "SA", "HT" to "NA", "HN" to "NA",
    "HU" to "EU", "IS" to "EU", "IN" to "AS", "ID" to "AS", "IR" to "AS",
    "IQ" to "AS", "IE" to "EU", "IL" to "AS", "IT" to "EU", "CI" to "AF",
    "JM" to "NA", "JP" to "AS", "JO" to "AS", "KZ" to "AS", "KE" to "AF",
    "KI" to "OC", "KR" to "AS", "KW" to "AS", "KG" to "AS", "LA" to "AS",
    "LV" to "EU", "LB" to "AS", "LS" to "AF", "LR" to "AF", "LY" to "AF",
    "LI" to "EU", "LT" to "EU", "LU" to "EU", "MG" to "AF", "MW" to "AF",
    "MY" to "AS", "MV" to "AS", "ML" to "AF", "MT" to "EU", "MH" to "OC",
    "MR" to "AF", "MU" to "AF", "MX" to "NA", "FM" to "OC", "MD" to "EU",
    "MC" to "EU", "MN" to "AS", "ME" to "EU", "MA" to "AF", "MZ" to "AF",
    "MM" to "AS", "NA" to "AF", "NP" to "AS", "NL" to "EU", "NZ" to "OC",
    "NI" to "NA", "NE" to "AF", "NG" to "AF", "MK" to "EU", "NO" to "EU",
    "OM" to "AS", "PK" to "AS", "PW" to "OC", "PA" to "NA", "PG" to "OC",
    "PY" to "SA", "PE" to "SA", "PH" to "AS", "PL" to "EU", "PT" to "EU",
    "QA" to "AS", "RO" to "EU", "RU" to "EU", "RW" to "AF", "WS" to "OC",
    "SM" to "EU", "ST" to "AF", "SA" to "AS", "SN" to "AF", "RS" to "EU",
    "SC" to "AF", "SL" to "AF", "SG" to "AS", "SK" to "EU", "SI" to "EU",
    "SB" to "OC", "SO" to "AF", "ZA" to "AF", "ES" to "EU", "LK" to "AS",
    "SD" to "AF", "SR" to "SA", "SE" to "EU", "CH" to "EU", "SY" to "AS",
    "TW" to "AS", "TJ" to "AS", "TZ" to "AF", "TH" to "AS", "TL" to "AS",
    "TG" to "AF", "TO" to "OC", "TN" to "AF", "TR" to "EU", "TM" to "AS",
    "TV" to "OC", "UG" to "AF", "UA" to "EU", "AE" to "AS", "GB" to "EU",
    "US" to "NA", "UY" to "SA", "UZ" to "AS", "VU" to "OC", "VA" to "EU",
    "VE" to "SA", "VN" to "AS", "YE" to "AS", "ZM" to "AF", "ZW" to "AF"
  )[cc] ?: "-"

  private fun flagEmoji(cc: String): String {
    if (cc.length != 2) return "-"
    val r1 = Character.toChars(0x1F1E6 - 'A'.code + cc[0].code).concatToString()
    val r2 = Character.toChars(0x1F1E6 - 'A'.code + cc[1].code).concatToString()
    return r1 + r2
  }

  private fun addRow(label: String, value: String, key: String) {
    val row = layoutInflater.inflate(R.layout.detail_item, binding.resultsContainer, false)
    row.findViewById<TextView>(R.id.label).text = label

    if (key == "maps" && value.startsWith("http")) {
      val tv = row.findViewById<TextView>(R.id.value)
      tv.text = "OPEN MAPS"
      tv.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
      row.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value))) }
    } else {
      row.findViewById<TextView>(R.id.value).text = value
    }

    binding.resultsContainer.addView(row)
    val divider = View(this)
    divider.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
    divider.setBackgroundResource(R.color.divider)
    binding.resultsContainer.addView(divider)
  }

  private fun fetchJson(urlString: String): JSONObject {
    val url = URL(urlString)
    val conn = url.openConnection() as HttpURLConnection
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("User-Agent", "IPILYAN/1.0")
    conn.requestMethod = "GET"
    val text = conn.inputStream.bufferedReader().readText()
    conn.disconnect()
    return JSONObject(text)
  }

  private fun setStatus(msg: String?) {
    if (msg == null) { binding.tvStatus.visibility = View.GONE }
    else { binding.tvStatus.text = msg; binding.tvStatus.visibility = View.VISIBLE }
  }
}
