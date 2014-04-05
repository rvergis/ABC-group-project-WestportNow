package com.davidschachter.westportnow;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MyDatabaseHelper extends SQLiteOpenHelper {
	String sql_create;
	
	public MyDatabaseHelper(Context context, String db_name, int db_version, String _sql_create) {
		super(context, db_name, null, db_version);
		Log.d("MyDatabaseHelper.MyDatabaseHelper()", "Starting database '" + db_name + "', version '" + db_version + "'.");
		sql_create = _sql_create;
		Log.d("MyDatabaseHelper.MyDatabaseHelper()", "Leaving function.");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("MyDatabaseHelper.onCreate()", "Entering function.");
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		Log.d("MyDatabaseHelper.onOpen()", "Entering function with sql_create=\n" + sql_create);
		if (sql_create != null) {
			db.execSQL(sql_create);		
		}
		Log.d("MyDatabaseHelper.onOpen()", "Leaving function.");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d("MyDatabaseHelper.onUpdate()", "Entering function, oldVersion=" + oldVersion + ", newVersion=" + newVersion + ".");
		// TODO Auto-generated method stub
	}
}
