package com.davidschachter.westportnow;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.os.Build;

public class DatabaseActivity extends Activity {
	Button  createDBButton, dropDBButton, articlesButton, authorsButton;
	ListView resultListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_database);

		createDBButton = (Button) findViewById(R.id.createDBButton);
		dropDBButton   = (Button) findViewById(R.id.dropDBButton);
		articlesButton = (Button) findViewById(R.id.articlesButton);
		authorsButton  = (Button) findViewById(R.id.authorsButton);
	}

	private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

	public void createDB(View view)              { Article.createDB(this);     toast("Created database '" + Article.getPath() + "'."); }
	public void dropDB(View view)                { Article.dropDB(this);       toast("Dropped database.");}
	public void showAuthors(View view)           { Article.dumpAuthors(this);  toast("Authors dumped to log."); }
	public void showArticles(View view)          { Article.dumpArticles(this); toast("Articles dumped to log."); }
	
	public void showSubjects(View view)          { Article.startDB(this); for (String subject  : Article.getSubjects())                            Log.d("Subject", "'" + subject + "'"); }
	public void showNewArticles(View view)       { Article.startDB(this); for (Article article : Article.getArticlesNoOlderThan(1396745520, null)) Log.d("Article", "" + article); }
	public void showNewSportsArticles(View view) { Article.startDB(this); for (Article article : Article.getArticlesNoOlderThan(0, "Sports"))      Log.d("Article", "" + article); }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.database, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
