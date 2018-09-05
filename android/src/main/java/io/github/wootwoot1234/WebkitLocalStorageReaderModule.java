package io.github.wootwoot1234;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableNativeMap;
import com.github.hf.leveldb.Iterator;
import com.github.hf.leveldb.LevelDB;

import java.io.File;
import java.nio.charset.Charset;


public class WebkitLocalStorageReaderModule extends ReactContextBaseJavaModule {

    public WebkitLocalStorageReaderModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "WebkitLocalStorageReader";
    }

    public WritableMap getLevel() {
        String dataDir = getReactApplicationContext().getApplicationInfo().dataDir;
        WritableMap kv = new WritableNativeMap();

        try {
            LevelDB levelDB = LevelDB.open(dataDir + "/app_webview/Local Storage/leveldb");

            Iterator iterator = levelDB.iterator();
            int prefixCount = 10;

            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                byte[] key   = iterator.key();
                byte[] value = iterator.value();

                if (key.length > prefixCount) {
                    byte[] subkey = new byte[key.length - prefixCount];
                    byte[] subval = new byte[value.length -1];

                    for (int i = prefixCount; i < key.length; i++) {
                        subkey[i - prefixCount] = key[i];
                    }

                    for (int i = 1; i < value.length; i++) {
                        subval[i - 1] = value[i];
                    }

                    String sKey = new String(subkey);
                    String sVal = new String(subval);
                    kv.putString(sKey, sVal);
                }
            }

            iterator.close(); // closing is a must!


        } catch (Exception e) {
            e.printStackTrace();
        }

        return kv;
    }

    @ReactMethod
    public void get(Promise promise) {
        WritableMap kv = getLevel();
        boolean hasItems = kv.keySetIterator().hasNextKey();

        if (hasItems) {
            promise.resolve(kv);
            return;
        }

        String dataDir = getReactApplicationContext().getApplicationInfo().dataDir;

        File localstorage = new File(dataDir + "/app_webview/Local Storage/file__0.localstorage");
        File leveldb = new File(dataDir + "/app_webview/Local Storage/leveldb");


        if (!localstorage.exists()) {
            promise.resolve(kv);
            return;
        }

        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            File dbfile = getReactApplicationContext().getDatabasePath(localstorage.getPath());
            dbfile.setWritable(true);
            db = SQLiteDatabase.openDatabase(dbfile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);

            String sql = "SELECT key,value FROM ItemTable";
            cursor = db.rawQuery(sql, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String key = cursor.getString(0);
                byte[] itemByteArray = cursor.getBlob(1);
                String value = new String(itemByteArray, Charset.forName("UTF-16LE"));

                kv.putString(key, value);
                cursor.moveToNext();
            }
            promise.resolve(kv);
        } catch (Exception e) {
            e.printStackTrace();
            promise.resolve(kv);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }
}
