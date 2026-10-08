package com.example.instructionmanual.data

import java.util.UUID

const val DEFAULT_CATEGORY = "未分类"

val PRESET_CATEGORIES = listOf(
    "未分类",
    "网络设备",
    "电脑数码",
    "手机平板",
    "家电",
    "影音设备",
    "机顶盒",
    "开发板",
    "仪器仪表",
    "工具五金",
    "其他",
)

/** 一张说明书图片。文件保存在 [ManualRepository.imageDir] 下，这里只存文件名。 */
data class ManualImage(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
)

/** 一台设备及其说明书图片。 */
data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String = "",
    val model: String = "",
    val category: String = DEFAULT_CATEGORY,
    val serialNumber: String = "",
    val purchaseDate: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val images: List<ManualImage> = emptyList(),
) {
    val cover: ManualImage? get() = images.firstOrNull()

    /** 供搜索使用的合并文本。 */
    val searchHaystack: String
        get() = buildString {
            append(name).append(' ')
            append(brand).append(' ')
            append(model).append(' ')
            append(category).append(' ')
            append(serialNumber).append(' ')
            append(notes)
        }.lowercase()

    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return q.split(' ').filter { it.isNotEmpty() }.all { searchHaystack.contains(it) }
    }

    /** 型号显示文本，例如 "小米 AX3000"。 */
    val displayModel: String
        get() = listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ")
}

enum class SortOrder(val label: String) {
    RecentUpdated("最近修改"),
    RecentCreated("最近添加"),
    Name("名称"),
    Category("分类"),
}

/** 导入备份时的合并策略。 */
enum class ImportMode(val label: String) {
    Merge("合并（保留现有设备）"),
    Replace("覆盖（清空后导入）"),
}
