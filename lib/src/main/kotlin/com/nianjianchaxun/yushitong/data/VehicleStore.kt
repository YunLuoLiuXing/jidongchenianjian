package com.nianjianchaxun.yushitong.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.nianjianchaxun.yushitong.core.VehicleType
import java.time.LocalDate

data class VehicleRecord(
    val id: Long = 0,
    val plate: String = "",
    val alias: String = "",
    val typeName: String,
    val registerDate: String,
    val isMyCar: Boolean = false,
    val owner: String = "",
    val sortOrder: Int = 0,
    val useNature: String = "NON_OPERATING",
    val createdAt: Long = System.currentTimeMillis()
) {
    val type: VehicleType?
        get() = VehicleType.values().firstOrNull { it.name == typeName }
    val regDate: LocalDate?
        get() = runCatching { LocalDate.parse(registerDate) }.getOrNull()
}

class VehicleStore(context: Context) {
    private val helper = Helper(context.applicationContext)

    private class Helper(ctx: Context) : SQLiteOpenHelper(ctx, "nianjian.db", null, 4) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE vehicle(" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "plate TEXT, alias TEXT, typeName TEXT, registerDate TEXT," +
                    "isMyCar INTEGER DEFAULT 0, owner TEXT, sortOrder INTEGER DEFAULT 0, useNature TEXT, createdAt INTEGER)"
            )
        }
        override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
            // v1 -> v2：新增车主/使用人列
            if (o < 2) {
                runCatching { db.execSQL("ALTER TABLE vehicle ADD COLUMN owner TEXT") }
            }
            // v2 -> v3：新增排序列
            if (o < 3) {
                runCatching { db.execSQL("ALTER TABLE vehicle ADD COLUMN sortOrder INTEGER DEFAULT 0") }
            }
            // v3 -> v4：新增使用性质列
            if (o < 4) {
                runCatching { db.execSQL("ALTER TABLE vehicle ADD COLUMN useNature TEXT") }
            }
        }
    }

    fun save(r: VehicleRecord): Long {
        val cv = android.content.ContentValues().apply {
            put("plate", r.plate)
            put("alias", r.alias)
            put("typeName", r.typeName)
            put("registerDate", r.registerDate)
            put("isMyCar", if (r.isMyCar) 1 else 0)
            put("owner", r.owner)
            put("sortOrder", r.sortOrder)
            put("useNature", r.useNature)
            put("createdAt", r.createdAt)
        }
        return helper.writableDatabase.insert("vehicle", null, cv)
    }

    fun list(myOnly: Boolean = false): List<VehicleRecord> {
        val where = if (myOnly) "isMyCar=1" else "1=1"
        val out = mutableListOf<VehicleRecord>()
        helper.readableDatabase.query(
            "vehicle", null, where, null, null, null, "createdAt DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out += VehicleRecord(
                    id = c.getLong(c.getColumnIndexOrThrow("_id")),
                    plate = c.getString(c.getColumnIndexOrThrow("plate")) ?: "",
                    alias = c.getString(c.getColumnIndexOrThrow("alias")) ?: "",
                    typeName = c.getString(c.getColumnIndexOrThrow("typeName")) ?: "",
                    registerDate = c.getString(c.getColumnIndexOrThrow("registerDate")) ?: "",
                    isMyCar = c.getInt(c.getColumnIndexOrThrow("isMyCar")) == 1,
                    owner = c.getString(c.getColumnIndexOrThrow("owner")) ?: "",
                    sortOrder = c.getInt(c.getColumnIndexOrThrow("sortOrder")),
                    useNature = c.getString(c.getColumnIndexOrThrow("useNature")) ?: "NON_OPERATING",
                    createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
                )
            }
        }
        return out
    }

    fun delete(id: Long) {
        helper.writableDatabase.delete("vehicle", "_id=?", arrayOf(id.toString()))
    }

    fun markAsMyCar(id: Long) {
        val cv = android.content.ContentValues().apply { put("isMyCar", 1) }
        helper.writableDatabase.update("vehicle", cv, "_id=?", arrayOf(id.toString()))
    }

    /** 便捷保存（UI 直接调用）：返回新记录 id */
    fun save(plate: String, typeName: String, registerDate: String, alias: String, isMyCar: Boolean): Long {
        return save(VehicleRecord(plate = plate, typeName = typeName, registerDate = registerDate, alias = alias, isMyCar = isMyCar))
    }

    /** 保存带车主的车辆（首页牌照展示用），已存在则更新 */
    fun saveWithOwner(plate: String, typeName: String, registerDate: String, owner: String, useNature: String = "NON_OPERATING"): Long {
        val exist = list(true).firstOrNull { it.plate == plate }
        return if (exist != null) {
            updateInfo(exist.id, plate, owner)
            exist.id
        } else {
            save(VehicleRecord(
                plate = plate,
                alias = "",
                typeName = typeName,
                registerDate = registerDate,
                isMyCar = true,
                owner = owner,
                useNature = useNature
            ))
        }
    }

    /** 修改已保存车辆的车牌与车主 */
    fun updateInfo(id: Long, plate: String, owner: String, typeName: String? = null, registerDate: String? = null, useNature: String? = null) {
        val db = helper.writableDatabase
        db.update("vehicle", android.content.ContentValues().apply {
            put("plate", plate)
            put("owner", owner)
            if (typeName != null) put("typeName", typeName)
            if (registerDate != null) put("registerDate", registerDate)
            if (useNature != null) put("useNature", useNature)
        }, "_id=?", arrayOf(id.toString()))
    }

    /** 批量保存牌照排序（按传入顺序写入 sortOrder） */
    fun updateOrder(ordered: List<VehicleRecord>) {
        val db = helper.writableDatabase
        ordered.forEachIndexed { index, r ->
            db.update("vehicle", android.content.ContentValues().apply {
                put("sortOrder", index)
            }, "_id=?", arrayOf(r.id.toString()))
        }
    }

    /** 我的车辆列表 */
    fun myCars(): List<VehicleRecord> = list().filter { it.isMyCar }.sortedBy { it.sortOrder }

    /** 历史查询记录 */
    fun history(): List<VehicleRecord> = list().filter { !it.isMyCar }
}
