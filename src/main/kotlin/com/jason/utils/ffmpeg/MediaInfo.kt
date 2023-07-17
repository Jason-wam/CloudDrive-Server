package com.jason.utils.ffmpeg

import com.jason.utils.Configure
import com.jason.utils.extension.formatPath
import com.jason.utils.extension.symbolicPath
import org.json.JSONObject
import java.io.File

class MediaInfo {
    val format = Format()
    val streams = ArrayList<Stream>()
    var isHdr: Boolean = false

    class Format {
        var formatName = ""
        var formatLongName = ""
        var duration = 0.0
        var size = 0L
        var bitRate = 0L
        val tags = ArrayList<Tag>()

        class Tag {
            var name: String = ""
            var value: String = ""
        }
    }

    class Stream {
        var index = 0
        var codecName = ""
        var codecLongName = ""
        var profile = ""
        var codecType = ""
        var codecTagString = ""
        var codecTag = ""
        var width = 0
        var height = 0
        var codedWidth = 0
        var codedHeight = 0
        var pixFmt = ""
        var colorRange = ""
        var colorSpace = ""
        var durationTs = 0L
        var duration = 0.0
        var bitsPerRawSample = ""

        var colorTransfer = ""
        var colorPrimaries = ""
        val tags = ArrayList<Tag>()

        class Tag {
            var name: String = ""
            var value: String = ""
        }
    }

    companion object {
        fun create(file: File): MediaInfo {
            return create(file.symbolicPath())
        }

        fun create(path: String): MediaInfo {
            val params = ArrayList<String>()
            params.add(Configure.ffprobe)
            params.add("-v error")
            params.add("-select_streams v")
            params.add("-show_format")
            params.add("-show_streams")
            params.add("-i \"${path.formatPath()}\"")
            params.add("-print_format json")

            val command = params.joinToString(" ")
            val process = Runtime.getRuntime().exec(command)
            val json = process.inputStream.reader().use {
                it.readText()
            }
            process.inputStream.close()
            process.errorStream.close()
            process.destroy()

//            println(json)
            val obj = JSONObject(json)
            val formatObj = obj.optJSONObject("format")
            val streamsArray = obj.optJSONArray("streams")

            return MediaInfo().apply {
                format.formatName = formatObj?.optString("format_name").orEmpty()
                format.formatLongName = formatObj?.optString("format_long_name").orEmpty()
                format.size = formatObj?.optLong("size") ?: 0
                format.bitRate = formatObj?.optLong("bit_rate") ?: 0
                format.duration = formatObj?.optDouble("duration") ?: 0.0
                formatObj?.optJSONObject("tags")?.let { tags ->
                    tags.keys().forEach { key ->
                        format.tags.add(Format.Tag().apply {
                            this.name = key
                            this.value = tags.getString(key)
                        })
                    }
                }

                if (streamsArray != null) {
                    for (i in 0 until streamsArray.length()) {
                        streamsArray.getJSONObject(i).let { stream ->
                            streams.add(Stream().apply {
                                index = stream.optInt("index")
                                codecName = stream.optString("codec_name")
                                codecLongName = stream.optString("codec_long_name")
                                profile = stream.optString("profile")
                                codecType = stream.optString("codec_type")
                                codecTagString = stream.optString("codec_tag_name")
                                codecTag = stream.optString("codec_tag")
                                width = stream.optInt("width")
                                height = stream.optInt("height")
                                codedWidth = stream.optInt("codec_width")
                                codedHeight = stream.optInt("codec_height")
                                pixFmt = stream.optString("pix_fmt")
                                colorRange = stream.optString("color_range")
                                colorSpace = stream.optString("color_space")
                                durationTs = stream.optLong("duration_ts")
                                duration = stream.optDouble("duration")
                                bitsPerRawSample = stream.optString("bits_per_raw_sample")
                                colorTransfer = stream.optString("color_transfer")
                                colorPrimaries = stream.optString("color_primaries")
                                if (isHdr.not()) {
                                    isHdr = colorPrimaries.contains("bt2020") &&
                                            colorTransfer.contains("arib-std-b67") ||
                                            colorTransfer.contains(
                                        "smpte2084"
                                    )
                                }
                                stream.optJSONObject("tags")?.let { tags ->
                                    tags.keys().forEach { key ->
                                        this.tags.add(Stream.Tag().apply {
                                            this.name = key
                                            this.value = tags.getString(key)
                                        })
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}