package com.davidschachter.westportnow;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Article {	final static String DB_NAME          = "main";
final static int    DB_VERSION       = 1;

final static String
    SQL_FETCH_ARTICLES_NO_OLDER_THAN =
        "SELECT a.id, a.title, a.url, a.ts, a.author_id, u.name, u.url, u.email, a.subject, a.content, a.wn_id" +
        " FROM articles AS a, authors AS u" +
        " WHERE a.ts >= ? AND a.author_id = u.id",

	SQL_FETCH_SUBJECTS =
   	    "SELECT DISTINCT(subject) AS s FROM ARTICLES ORDER BY s",
   	    
   	SQL_ADD_ARTICLE =
   	    "INSERT INTO articles (title, url, ts, author_id, subject, content, wn_id) VALUES (?, ?, ?, ?, ?, ?, ?)",

    SQL_CREATE_AUTHOR_TABLE =
        "CREATE TABLE authors (\n" +
   	    "  _id   INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
   	    "  name  TEXT(100) NOT NULL,\n" +
   	    "  email TEXT(100),\n" +
   	    "  url   TEXT(1024)\n" +
   	    ")",
   	    
   	SQL_CREATE_ARTICLE_TABLE =
   	    "CREATE TABLE articles (\n" +
        "  _id       INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
        "  title     TEXT(1024) NOT NULL,\n" +
        "  url       TEXT(1024) NOT NULL,\n" +
        "  ts        INTEGER    NOT NULL, -- timestamp from the \"modified\" field in the RSS\n" +
        "  subject   TEXT(100) NOT NULL,  -- not null for sure?  Could be a foreign key but then adding a new subject would require an update to that table.\n" +
        "  content   TEXT NOT NULL,\n" +
        "  wn_id     TEXT(100),            -- This is the \"id\" field from the RSS. I don't know the semantic. Is it always present?\n" +
        "  author_id INTEGER REFERENCES authors (_id) NOT NULL\n" +
        ")",
        
   	SQL_CREATE_INDEX_1 = "CREATE UNIQUE INDEX IF NOT EXISTS articles_by_title   ON articles (title)",
   	SQL_CREATE_INDEX_2 = "CREATE        INDEX IF NOT EXISTS articles_by_time    ON articles (ts DESC)",
   	SQL_CREATE_INDEX_3 = "CREATE UNIQUE INDEX IF NOT EXISTS articles_by_wn_id   ON articles (wn_id)",
   	SQL_CREATE_INDEX_4 = "CREATE        INDEX IF NOT EXISTS articles_by_subject ON articles (subject)",
   	
   	SQL_DROP_AUTHORS  = "DROP TABLE IF EXISTS authors",
   	SQL_DROP_ARTICLES = "DROP TABLE IF EXISTS articles",
   	
   	SQL_DROP_INDEX_1  = "DROP INDEX IF EXISTS articles_by_title",
   	SQL_DROP_INDEX_2  = "DROP INDEX IF EXISTS articles_by_time",
   	SQL_DROP_INDEX_3  = "DROP INDEX IF EXISTS articles_by_wn_id",
   	SQL_DROP_INDEX_4  = "DROP INDEX IF EXISTS articles_by_subject",
   	
   	SQL_DUMP_AUTHORS  = "SELECT _id, name, email, url FROM authors",
   	SQL_DUMP_ARTICLES = "SELECT _id, title, url, ts, subject, content, wn_id, author_id FROM articles"
;
final static String[]
    SQL_CREATE_STATEMENTS = new String[] {SQL_CREATE_AUTHOR_TABLE,
                                          SQL_CREATE_ARTICLE_TABLE,
                                          SQL_CREATE_INDEX_1,
                                          SQL_CREATE_INDEX_2,
                                          SQL_CREATE_INDEX_3,
                                          SQL_CREATE_INDEX_4
                                         },
    SQL_DROP_STATEMENTS  = new String[]  {SQL_DROP_INDEX_4,
                                          SQL_DROP_INDEX_3,
                                          SQL_DROP_INDEX_2,
                                          SQL_DROP_INDEX_1,
                                          SQL_DROP_ARTICLES,
                                          SQL_DROP_AUTHORS};
static SQLiteDatabase db = null;

int      id, author_id;
String   title, url, subject, content, wn_id, author_name, author_url, author_email;
int      timestamp;

private Article(Cursor cursor) {
 // Create an article from a database row.

 id           = cursor.getInt(   cursor.getColumnIndexOrThrow("a.id"));
 title        = cursor.getString(cursor.getColumnIndexOrThrow("a.title"));
 url          = cursor.getString(cursor.getColumnIndexOrThrow("a.url"));
 timestamp    = cursor.getInt(   cursor.getColumnIndexOrThrow("a.ts"));
 author_id    = cursor.getInt(   cursor.getColumnIndexOrThrow("a.author_id"));
 author_name  = cursor.getString(cursor.getColumnIndexOrThrow("u.name"));
 author_url   = cursor.getString(cursor.getColumnIndexOrThrow("u.url"));
 author_email = cursor.getString(cursor.getColumnIndexOrThrow("u.email"));
 subject      = cursor.getString(cursor.getColumnIndexOrThrow("a.subject"));
 content      = cursor.getString(cursor.getColumnIndexOrThrow("a.content"));
 wn_id        = cursor.getString(cursor.getColumnIndexOrThrow("a.wn_id"));
}

private Article() { }

// FIXME: Database functions should run in a separate thread with a callback. --DS, 3-Apr-2014
static public ArrayList<Article> getArticlesNoOlderThan(int timestamp, String subjectFilter) {
 // Return a list of articles from a database fetch.
 // To support newspaper sections (sports, finance, real estate, etc.), the subjectFilter, if not
 // null, causes only articles with a matching subject to be returned. (RSS "Subject" == "Section")

 begin_txn();

 String             sql        = SQL_FETCH_ARTICLES_NO_OLDER_THAN + ((subjectFilter == null) ? "" : " AND a.subject = ?");
 String []          parameters = (subjectFilter == null) ? new String[] {Integer.toString(timestamp)} :
	 													   new String[] {Integer.toString(timestamp), subjectFilter};
 Cursor             cursor     = db.rawQuery(sql, parameters);
 ArrayList<Article> result     = new ArrayList<Article>(cursor.getCount());

 while (cursor.moveToNext()) {
   result.add(new Article(cursor));
 }

 cursor.close();
 commit();

 return result;
}


// FIXME: Database functions should run in a separate thread with a callback. --DS, 3-Apr-2014
static public ArrayList<String> getSubjects() {
 // Return a list of subjects, to populate the list of sections the user might choose among.

 begin_txn();

 Cursor            cursor      = db.rawQuery(SQL_FETCH_SUBJECTS, null);
 final int         columnIndex = cursor.getColumnIndexOrThrow("s");
 ArrayList<String> result      = new ArrayList<String>(cursor.getCount());

 while (cursor.moveToNext()) {
   result.add(cursor.getString(columnIndex));
 }

 cursor.close();
 commit();

 return result;
}

@Override
public String toString() {
   return "Article id " + id +
          " has title='" + title +
          "', subject='" + subject +
          "', timestamp=" + timestamp +
          ", wn_id='" + wn_id +
          ", author='" + author_name +
          "', email=" + author_email +
          ", a.url=" + author_url +
          ", url=" + url +
          ", content=\n" + content;
}

private void writeToDB() {
   String[] params = new String[] {title, url, "" + timestamp, "" + author_id, subject, content, wn_id};
   
   db.execSQL(Article.SQL_ADD_ARTICLE, params);
}


static public int[] makeFakeAuthors() {  
   int[]    author_ids = new int[3];
   
   for (int i=0; i < author_ids.length; i++) {
       String name  = "fake author #" + i;
       String email = "fake_author_" + i + "@example.com";
       String url   = "http://example.com/fake_author_url/" + i;
       
       String[] params = new String[] {name, email, url};

       db.execSQL("INSERT INTO authors (name, email, url) VALUES (?, ?, ?)", params);

       Cursor cursor = db.rawQuery("SELECT _id FROM authors WHERE name='" + name + "'", null);
       author_ids[i] = cursor.moveToFirst() ? cursor.getInt(cursor.getColumnIndexOrThrow("_id")) : -1;
       cursor.close();
       
//       Log.d("Article.makeFakeAuthors()", "Created author #" + i + ", id=" + author_ids[i] + ", name='" + name + "', email=" + email + ", url=" + url + ".");
   }

   return author_ids;
}

static public void makeFake() {  
   int[] author_ids = makeFakeAuthors();
   
   for (int i=0; i < 20; i++) {
       Article article   = new Article();
       article.title     = "Fake article title " + i;
       article.url       = "http://example.com/fake_content/" + i;
       article.subject   = "Fake article subject " + i;
       article.wn_id     = "Fake article wn_id " + i;
       article.author_id = author_ids[i % author_ids.length];
       article.timestamp = (int) (java.lang.System.currentTimeMillis() / 1000);
       article.content   = "Fake content of four paragraphs. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec dui sem, suscipit quis vehicula vitae, volutpat nec augue. Pellentesque sapien mi, fringilla vel massa imperdiet, aliquet porttitor lorem. Proin mattis pharetra purus, sit amet bibendum sapien dapibus et. Quisque eu commodo quam. Vivamus volutpat augue at lectus commodo, sed aliquam quam venenatis. Donec egestas dignissim est et mollis. Morbi vitae lacus vitae libero congue euismod. Integer nec mi purus. Nullam laoreet nisi eu eros luctus rhoncus.\nPhasellus convallis odio urna, sed auctor nisl aliquam ac. Aliquam quam lacus, malesuada quis porta non, elementum eget neque. Maecenas rhoncus pulvinar vulputate. Etiam interdum sagittis erat quis ornare. Nam luctus nibh id nisi tempor, nec blandit ante volutpat. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Pellentesque tincidunt, lorem in suscipit interdum, mi tellus consectetur elit, in fermentum neque nisi in metus.\nFusce nec ultrices eros, sed sagittis ante. Cras sollicitudin ornare justo, eu rutrum sem. Curabitur id venenatis leo. Curabitur non egestas felis, et cursus risus. Maecenas ultrices euismod neque vitae vulputate. Nulla elementum nisl et felis commodo, eu hendrerit nisi blandit. Suspendisse eu mattis nunc, sit amet blandit lorem. Quisque placerat non velit vitae consectetur. Aliquam eu molestie magna, vitae euismod tellus.\nPraesent sit amet massa hendrerit, cursus sapien eget, accumsan sapien. Cras luctus, urna vel egestas pulvinar, dolor magna tincidunt odio, ut ullamcorper mi turpis id nisl. Suspendisse justo metus, varius ac purus quis, gravida aliquam lorem. Sed dapibus mi at velit mattis, et aliquam justo placerat. Integer lacinia dictum lectus ac fringilla. Nam eget scelerisque tortor, id laoreet sapien.\nNunc egestas massa fermentum, rhoncus lacus sed, feugiat diam. Nam posuere elementum nisl, ac lobortis urna condimentum nec. Donec ut cursus lorem. Morbi tempor magna libero, et blandit augue commodo et.";
       
       article.writeToDB();
       
       String[] params = new String[] {Integer.toString(article.author_id)};
       Cursor cursor = db.rawQuery("SELECT name, email, url FROM authors WHERE _id=?", params);
//       Log.d("Article.makeFake()", "author_id for new article='" + params[0] + "'.");
       cursor.moveToFirst();
       article.author_name  = cursor.getString(0);
       article.author_email = cursor.getString(1);
       article.author_url   = cursor.getString(2);

       cursor.close();
       
//       Log.d("Article.makeFake()", "Article #" + i + " is " + article);
   }       
}

static public void createDB(Context context) {
   if (db == null) {
	   startDB(context);
   }
   
   for (int i=0; i < SQL_CREATE_STATEMENTS.length; i++) {
	   Log.d("Article.createDB()", "About to execute #" + i + ": '" + SQL_CREATE_STATEMENTS[i] + "'.");
       db.execSQL(SQL_CREATE_STATEMENTS[i]);
   }
   
   begin_txn();
   makeFake();
   commit();
}

static public void dropDB(Context context) {
   if (db == null) {
	   startDB(context);
   }
   
   for (int i=0; i < SQL_DROP_STATEMENTS.length; i++) {
	   Log.d("Article.dropDB()", "About to execute # " + i + ": '" + SQL_DROP_STATEMENTS[i] + "'.");
       db.execSQL(SQL_DROP_STATEMENTS[i]);
   }   
}

static public void dumpAuthors(Context context) {
	Log.d("Article.dumpAuthors()", "Entering function.");

	if (db == null) {
		startDB(context);
	}
	
	begin_txn();
	
	Cursor cursor     = db.rawQuery(SQL_DUMP_AUTHORS, null);
	int    rowCounter = 0;
	
	Log.d("Article.dumpAuthors()", "There are " + cursor.getCount() + " authors.");
	
	while (cursor.moveToNext()) {
	     int    id    = cursor.getInt(   cursor.getColumnIndexOrThrow("_id"));
		 String name  = cursor.getString(cursor.getColumnIndexOrThrow("name"));
		 String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
		 String url   = cursor.getString(cursor.getColumnIndexOrThrow("url"));

		 Log.d("Article.dumpAuthors()", "Row #"   + ++rowCounter +
				 						": _id="   + id +
				 						", name='" + name +
				 						", email=" + email +
				 						"', url="  + url +
				 						"'.");
	}

	rollback();
	
	Log.d("Article.dumpAuthors()", "Leaving function.");
}

static public void dumpArticles(Context context) {
	if (db == null) {
		startDB(context);
	}
	
	begin_txn();
	
	Cursor cursor     = db.rawQuery(SQL_DUMP_ARTICLES, null);
	int    rowCounter = 0;
	
	while (cursor.moveToNext()) {
	     int    id         = cursor.getInt(   cursor.getColumnIndexOrThrow("_id"));
		 String title      = cursor.getString(cursor.getColumnIndexOrThrow("title"));
		 String url        = cursor.getString(cursor.getColumnIndexOrThrow("url"));
		 int    timestamp  = cursor.getInt(   cursor.getColumnIndexOrThrow("ts"));
		 String subject    = cursor.getString(cursor.getColumnIndexOrThrow("subject"));
		 String content    = cursor.getString(cursor.getColumnIndexOrThrow("content"));
		 String wn_id      = cursor.getString(cursor.getColumnIndexOrThrow("wn_id"));
		 int    author_id  = cursor.getInt(   cursor.getColumnIndexOrThrow("author_id"));

		 Log.d("Article.dumpArticles()", "Row #"        + ++rowCounter +
				 						 ": _id="       + id +
				 						 ", title='"    + title +
				 						 "', url="      + url +
				 						 ", ts="        + timestamp +
				 						 ", wn_id="     + wn_id +
				 						 ", author_id=" + author_id +
				 						 ", content='"  + content +
				 						 "'.");
	}

	rollback();
}

private static void begin_txn() { db.execSQL("BEGIN TRANSACTION"); }
private static void commit()    { db.execSQL("COMMIT"); }
private static void rollback()  { db.execSQL("ROLLBACK"); }

static public void startDB(Context context) {
	Log.d("Article.startDB()", "Entering with context=" + context);
	if (db == null) {
		MyDatabaseHelper dbh = new MyDatabaseHelper(context, DB_NAME, DB_VERSION, null);
		db = dbh.getWritableDatabase();
		Log.d("Article.startDB()", "Path to database: '" + db.getPath() + "'.");
	}
	
	Log.d("Article.startDB()", "Leaving function.");
}
};


/*

<entry>
  <title>Essay Winners Underscore Westport&#8217;s Lack of Diversity</title>
  <link rel="alternate" type="text/html" href="http://www.westportnow.com/index.php?/v2_5/48011/" /> 
  <id>tag:westportnow.com,2014:/5.48011</id>
  <issued>2014-04-01T02:04:37+00:00</issued>
  <modified>2014-04-01T02:38:38+00:00</modified>
  <summary></summary>
  <created>2014-04-01T02:04:37+00:00</created>
	<author>
	  <name>Gordon Joseloff</name>
	  <email>editor@westportnow.com</email>
	  		</author>
  <dc:subject>News, Arts and Leisure, Education</dc:subject>
  <content type="text/html" mode="escaped" xml:lang="en-US"><![CDATA[<p><b>By James Lomuscio</b></p>

<p>TEAM Westport&#8217;s three winning essayists, all Staples High School students, took their hometown to task tonight for offering everything but de facto diversity. </p><div class="photo" style="width: 260px; float: right; margin: 0 0 5px 10px;"><a href="http://cdn.westportnow.com/ee/images/uploads/IMG_1148.jpg" onclick="window.open('http://cdn.westportnow.com/ee/images/uploads/IMG_1148.jpg','popup','width=1615,height=1069,scrollbars=no,resizable=yes,toolbar=no,directories=no,location=no,menubar=no,status=no,left=0,top=0'); return false"><img src="http://cdn.westportnow.com/ee/images/uploads/IMG_1148_thumb.jpg" border="0" alt="WestportNow.com Image" name="image" width="260" height="171" /></a><br />TEAM Westport essay winners (l-r) Eliza Llewelynn (second place), Megan Root (first place), and Kyle Baer (third place) are flanked by TEAM Westport Chair Harold Bailey (l) and First Selectman Jim Marpe. (CLICK TO ENLARGE) <i>Dave Matlow for WestportNow.com</i><br /></div>

<p>&#8220;I think Staples&#8217; classes present a lot of the right questions, but the dearth of diversity means there are perspectives I&#8217;ve never heard,&#8221; writes Megan Root, a junior who won the $1,000 first prize for &#8220;Diversity: the Maestro of Innovation.&#8221;</p>

<p>Her thesis says that the lack of cultural exposure in a community that is 93 percent white does not &#8220;back the egalitarian ideals taught in history class and encouraged by our community.&#8221;</p>

<p>&#8220;Instead, our relative lack of interaction means many cultures remain unfamiliar to us, and humans tend to fear the unknown,&#8221; she said.
</p>]]></content>
</entry>  

*/