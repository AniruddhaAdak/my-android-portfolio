package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "guestbook_entries")
data class GuestbookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0
)

@Dao
interface GuestbookDao {
    @Query("SELECT * FROM guestbook_entries ORDER BY timestamp DESC")
    fun getAllEntriesFlow(): Flow<List<GuestbookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: GuestbookEntity)

    @Query("UPDATE guestbook_entries SET likes = likes + 1 WHERE id = :entryId")
    suspend fun incrementLikes(entryId: Int)

    @Delete
    suspend fun deleteEntry(entry: GuestbookEntity)
}

@Database(entities = [GuestbookEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun guestbookDao(): GuestbookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "portfolio_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
