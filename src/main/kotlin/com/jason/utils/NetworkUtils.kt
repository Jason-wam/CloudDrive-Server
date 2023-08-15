package com.jason.utils

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.regex.Pattern


object NetworkUtils {

    /**
     * Get local Ip address.
     */
    fun getLocalIPAddresses(): List<String> {
        return ArrayList<String>().apply {
            try {
                val enumeration = NetworkInterface.getNetworkInterfaces()
                if (enumeration != null) {
                    while (enumeration.hasMoreElements()) {
                        val element = enumeration.nextElement()
                        val addresses = element.inetAddresses
                        if (addresses != null) {
                            while (addresses.hasMoreElements()) {
                                val address = addresses.nextElement()
                                if ((!address.isLoopbackAddress) && (address is Inet4Address)) {
                                    add(address.hostAddress)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Check if valid IPV4 address.
     *
     * @param input the address string to check for validity.
     * @return True if the input parameter is a valid IPv4 address.
     */
    private fun isIPv4Address(input: String?): Boolean {
        return IPV4_PATTERN.matcher(input.orEmpty()).matches()
    }

    /**
     * Ipv4 address check.
     */
    private val IPV4_PATTERN = Pattern.compile(
        "^(" + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
    )
}