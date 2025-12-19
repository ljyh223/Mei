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
        val cleanXml = xml.replace(Regex("\\s+"), " ").trim()
        val stack = ArrayDeque<XmlElement>() // Use ArrayDeque for stack operations
        var i = 0

        while (i < cleanXml.length) {
            when {
                cleanXml[i] == '<' -> {
                    if (i + 1 < cleanXml.length && cleanXml[i + 1] == '/') {
                        // Handle closing tag
                        val endIndex = cleanXml.indexOf('>', i + 1)
                        if (stack.size > 1) {
                            val currentElement = stack.removeLast()
                            val parent = stack.removeLast() // Pop parent
                            val newChildren = parent.children.toMutableList().apply { add(currentElement) }
                            stack.addLast(parent.copy(children = newChildren)) // Push updated parent back
                        }
                        i = endIndex + 1
                    } else {
                        // Handle opening tag
                        val endIndex = cleanXml.indexOf('>', i + 1)
                        val tagPart = cleanXml.substring(i + 1, endIndex)

                        val isSelfClosing = tagPart.endsWith("/")
                        val actualTagPart = if (isSelfClosing) tagPart.dropLast(1).trim() else tagPart

                        val (tagName, attributes) = parseTagAndAttributes(actualTagPart)
                        val newElement =
                            XmlElement(
                                tagName,
                                attributes,
                                emptyList(),
                                ""
                            )

                        if (isSelfClosing) {
                            if (stack.isNotEmpty()) {
                                val parent = stack.removeLast() // Pop
                                val newChildren = parent.children.toMutableList().apply { add(newElement) }
                                stack.addLast(parent.copy(children = newChildren)) // Push updated
                            } else {
                                stack.addLast(newElement) // This becomes the root
                            }
                        } else {
                            stack.addLast(newElement)
                        }
                        i = endIndex + 1
                    }
                }
                else -> {
                    // Handle text content
                    val nextTagIndex = cleanXml.indexOf('<', i)
                    if (nextTagIndex == -1) break

                    val rawText = cleanXml.substring(i, nextTagIndex)

                    if (rawText.isNotEmpty() && stack.isNotEmpty()) {
                        val trimmedText = rawText.trim()

                        // Case 1: Add to current element's text
                        if (trimmedText.isNotEmpty()) {
                            val currentElement = stack.removeLast()
                            stack.addLast(currentElement.copy(text = currentElement.text + trimmedText))
                        }

                        // Case 2: Handle whitespace as a separate text node child of the current top
                        val stringBetweenTags = rawText.replace(trimmedText, "")
                        if (stringBetweenTags.isNotEmpty()) {
                            if (stack.isNotEmpty()) {
                                val textNode =
                                    XmlElement(
                                        name = "#text",
                                        text = stringBetweenTags,
                                        attributes = emptyList(),
                                        children = emptyList()
                                    )
                                val parent = stack.removeLast()
                                val newChildren = parent.children.toMutableList().apply { add(textNode) }
                                stack.addLast(parent.copy(children = newChildren))
                            }
                        }
                    }
                    i = nextTagIndex
                }
            }
        }

        return if (stack.isNotEmpty()) stack.first() else XmlElement(
            "",
            emptyList(),
            emptyList(),
            ""
        )
    }

    private fun parseTagAndAttributes(tagPart: String): Pair<String, List<XmlAttribute>> {
        val parts = tagPart.split(" ")
        val tagName = parts.getOrElse(0) { "" }
        val attributes = mutableListOf<XmlAttribute>()
        var i = 1
        while (i < parts.size) {
            val part = parts[i]
            if (part.contains("=")) {
                val attrParts = part.split("=", limit = 2)
                if (attrParts.size == 2) {
                    val attrName = attrParts[0]
                    var attrValue = attrParts[1]
                    if (attrValue.startsWith("\"") && !attrValue.endsWith("\"")) {
                        var j = i + 1
                        while (j < parts.size && !parts[j].endsWith("\"")) {
                            attrValue += " " + parts[j]
                            j++
                        }
                        if (j < parts.size) {
                            attrValue += " " + parts[j]
                        }
                        i = j
                    }
                    attrValue = attrValue.removeSurrounding("\"")
                    attributes.add(
                        XmlAttribute(
                            attrName,
                            attrValue
                        )
                    )
                }
            }
            i++
        }
        return tagName to attributes
    }
}