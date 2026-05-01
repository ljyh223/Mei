package com.ljyh.mei.utils.lyric.xml

internal data class XmlAttribute(
    val name: String,
    val value: String
)

internal data class XmlElement(
    val name: String,
    val attributes: List<XmlAttribute>,
    val children: List<XmlElement>,
    val text: String
)

internal class SimpleXmlParser {
    fun parse(xml: String): XmlElement {
        val stack = ArrayDeque<MutableElement>()
        var i = 0

        while (i < xml.length) {
            val c = xml[i]
            when {
                c == '<' -> {
                    if (i + 1 < xml.length && xml[i + 1] == '/') {
                        // Closing tag
                        val endIndex = xml.indexOf('>', i + 1)
                        if (endIndex == -1) break

                        if (stack.size > 1) {
                            val current = stack.removeLast().toXmlElement()
                            stack.last().children.add(current)
                        }
                        i = endIndex + 1
                    } else if (i + 1 < xml.length && xml[i + 1] == '!' && xml.startsWith("!--", i + 1)) {
                        // Comment
                        val endIndex = xml.indexOf("-->", i + 3)
                        i = if (endIndex == -1) xml.length else endIndex + 3
                    } else if (i + 1 < xml.length && xml[i + 1] == '?') {
                        // Prolog/Processing instruction
                        val endIndex = xml.indexOf("?>", i + 2)
                        i = if (endIndex == -1) xml.length else endIndex + 2
                    } else {
                        // Opening tag
                        val endIndex = xml.indexOf('>', i + 1)
                        if (endIndex == -1) break

                        var tagPart = xml.substring(i + 1, endIndex)
                        val isSelfClosing = tagPart.endsWith('/')
                        if (isSelfClosing) {
                            tagPart = tagPart.substring(0, tagPart.length - 1).trim()
                        }

                        val (tagName, attributes) = parseTagAndAttributes(tagPart)
                        val newElement = MutableElement(tagName, attributes.toMutableList())

                        if (isSelfClosing) {
                            if (stack.isNotEmpty()) {
                                stack.last().children.add(newElement.toXmlElement())
                            } else {
                                // Root is self-closing? Rare but handle it.
                                return newElement.toXmlElement()
                            }
                        } else {
                            stack.addLast(newElement)
                        }
                        i = endIndex + 1
                    }
                }
                c.isWhitespace() -> {
                    // Handle whitespace
                    var j = i
                    while (j < xml.length && xml[j].isWhitespace()) {
                        j++
                    }
                    val whitespace = xml.substring(i, j)
                    if (stack.isNotEmpty()) {
                        val textNode = XmlElement("#text", emptyList(), emptyList(), whitespace)
                        stack.last().children.add(textNode)
                    }
                    i = j
                }
                else -> {
                    // Text content
                    val nextTagIndex = xml.indexOf('<', i)
                    val rawText = if (nextTagIndex == -1) xml.substring(i) else xml.substring(i, nextTagIndex)

                    if (rawText.isNotEmpty() && stack.isNotEmpty()) {
                        stack.last().textBuilder.append(rawText.trim())
                    }
                    i = if (nextTagIndex == -1) xml.length else nextTagIndex
                }
            }
        }

        return if (stack.isNotEmpty()) stack.first().toXmlElement() else XmlElement("", emptyList(), emptyList(), "")
    }

    private fun parseTagAndAttributes(tagPart: String): Pair<String, List<XmlAttribute>> {
        val firstSpace = tagPart.indexOf(' ')
        if (firstSpace == -1) return tagPart to emptyList()

        val tagName = tagPart.substring(0, firstSpace)
        val attributes = mutableListOf<XmlAttribute>()

        var i = firstSpace + 1
        while (i < tagPart.length) {
            // Skip spaces
            while (i < tagPart.length && tagPart[i].isWhitespace()) i++
            if (i >= tagPart.length) break

            val equalsIndex = tagPart.indexOf('=', i)
            if (equalsIndex == -1) break

            val attrName = tagPart.substring(i, equalsIndex).trim()
            i = equalsIndex + 1

            // Skip spaces after '='
            while (i < tagPart.length && tagPart[i].isWhitespace()) i++
            if (i >= tagPart.length) break

            val quote = tagPart[i]
            if (quote == '"' || quote == '\'') {
                val nextQuote = tagPart.indexOf(quote, i + 1)
                if (nextQuote == -1) break
                val attrValue = tagPart.substring(i + 1, nextQuote)
                attributes.add(XmlAttribute(attrName, attrValue))
                i = nextQuote + 1
            } else {
                // Unquoted value
                var nextSpace = i
                while (nextSpace < tagPart.length && !tagPart[nextSpace].isWhitespace()) nextSpace++
                val attrValue = tagPart.substring(i, nextSpace)
                attributes.add(XmlAttribute(attrName, attrValue))
                i = nextSpace
            }
        }

        return tagName to attributes
    }

    private class MutableElement(
        val name: String,
        val attributes: MutableList<XmlAttribute> = mutableListOf(),
        val children: MutableList<XmlElement> = mutableListOf(),
        val textBuilder: StringBuilder = StringBuilder()
    ) {
        fun toXmlElement() = XmlElement(name, attributes, children, textBuilder.toString())
    }
}