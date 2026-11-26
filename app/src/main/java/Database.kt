package com.mitienda

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// Tabla: cada fila es un juego agregado al carrito
@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameId: Int
)

// Consultas
@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY id")
    fun observeAll(): Flow<List<CartItem>>

    @Insert
    suspend fun insert(item: CartItem)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clear()
}

// Base de datos
@Database(entities = [CartItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "pixelforge.db")
                .build().also { INSTANCE = it }
        }
    }
}