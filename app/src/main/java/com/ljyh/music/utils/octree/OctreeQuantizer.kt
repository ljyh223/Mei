package com.ljyh.music.utils.octree

import androidx.compose.ui.graphics.Color

class OctreeQuantizer {
    private val root = OctreeNode()
    private val leaves = mutableListOf<OctreeNode>()

    fun addColor(color: Color) {
        root.addColor(color, 0)
    }

    fun getDominantColors(count: Int): List<Color> {
        traverseTree(root)
        return leaves.sortedByDescending { it.pixelCount }
            .take(count)
            .map { it.getAverageColor() }
    }

    private fun traverseTree(node: OctreeNode) {
        if (node.isLeaf) {
            leaves.add(node)
        } else {
            for (child in node.children) {
                child?.let { traverseTree(it) }
            }
        }
    }
}
