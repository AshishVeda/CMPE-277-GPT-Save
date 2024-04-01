package com.example.chatgpt_save;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "audit_log.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table for Audit Prompt
        db.execSQL("CREATE TABLE IF NOT EXISTS AuditPrompt ("
                + "sequence_number INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date_time TEXT,"
                + "prompt TEXT"
                + ");");

        // Create table for Responses
        db.execSQL("CREATE TABLE IF NOT EXISTS Responses ("
                + "sequence_number INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date_time TEXT,"
                + "response TEXT"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop tables if they exist
        db.execSQL("DROP TABLE IF EXISTS AuditPrompt");
        db.execSQL("DROP TABLE IF EXISTS Responses");
        onCreate(db);
    }
}
