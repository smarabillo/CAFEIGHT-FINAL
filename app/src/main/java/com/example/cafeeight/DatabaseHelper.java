package com.example.cafeeight;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CafeEightDB";
    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_USERS = "Users";
    private static final String USER_ID = "id";
    private static final String USER_EMAIL = "email";
    private static final String USER_PASSWORD = "password";

    private static final String TABLE_ORDERS = "Orders";
    private static final String ORDER_ID = "order_id";
    private static final String ORDER_TOTAL_AMOUNT = "total_amount";
    private static final String ORDER_TOTAL_ITEMS = "total_items";
    private static final String ORDER_DATE = "order_date";

    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm a";

    private OnOrderInsertedListener orderInsertedListener;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createUsersTable(db);
        createOrdersTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        onCreate(db);
    }

    private void createUsersTable(SQLiteDatabase db) {
        String createUsersTableQuery = "CREATE TABLE " + TABLE_USERS + "(" +
                USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                USER_EMAIL + " TEXT, " +
                USER_PASSWORD + " TEXT)";
        db.execSQL(createUsersTableQuery);
    }

    private void createOrdersTable(SQLiteDatabase db) {
        String createOrdersTableQuery = "CREATE TABLE " + TABLE_ORDERS + "(" +
                ORDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ORDER_TOTAL_AMOUNT + " REAL, " +
                ORDER_TOTAL_ITEMS + " INTEGER, " +
                ORDER_DATE + " TEXT)";
        db.execSQL(createOrdersTableQuery);
    }

    public static long insertOrder(Context context, double totalAmount, int totalItems) {
        SQLiteDatabase db = null;
        long orderId = -1;

        try {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(ORDER_TOTAL_AMOUNT, totalAmount);
            values.put(ORDER_TOTAL_ITEMS, totalItems);
            values.put(ORDER_DATE, getCurrentDate());
            orderId = db.insert(TABLE_ORDERS, null, values);

            if (context instanceof OnOrderInsertedListener) {
                ((OnOrderInsertedListener) context).onOrderInserted();
            }

            if (dbHelper.orderInsertedListener != null) {
                dbHelper.orderInsertedListener.onOrderInserted();
            }

        } catch (SQLiteException e) {
            Log.e("DatabaseHelper", "Error inserting order: " + e.getMessage());
        } finally {
            closeDatabase(db);
        }

        return orderId;
    }

    private static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return dateFormat.format(new Date());
    }

    public interface OnOrderInsertedListener {
        void onOrderInserted();
    }

    // Get the latest order from the database

    public List<Order> getTodaySalesData() {
        List<Order> todayOrders = new ArrayList<>();
        SQLiteDatabase db = null;

        try {
            db = getReadableDatabase();

            // Fetch data for the current day
            String query = "SELECT " + ORDER_DATE + ", SUM(" + ORDER_TOTAL_AMOUNT + ") AS total_amount " +
                    "FROM " + TABLE_ORDERS +
                    " WHERE DATE(" + ORDER_DATE + ") = DATE('now')" +
                    " GROUP BY " + ORDER_DATE;
            Cursor cursor = db.rawQuery(query, null);

            while (cursor.moveToNext()) {
                int totalAmountIndex = cursor.getColumnIndex("total_amount");
                int orderDateIndex = cursor.getColumnIndex(ORDER_DATE);

                if (totalAmountIndex != -1 && orderDateIndex != -1) {
                    double totalAmount = cursor.getDouble(totalAmountIndex);
                    String orderDate = cursor.getString(orderDateIndex);

                    Order order = new Order(0, totalAmount, 0, orderDate);
                    todayOrders.add(order);
                }
            }

            cursor.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting today's sales data: " + e.getMessage());
        } finally {
            closeDatabase(db);
        }

        return todayOrders;
    }

    public List<Order> getUpdatedData() {
        List<Order> updatedData = new ArrayList<>();
        SQLiteDatabase db = null;

        try {
            db = getReadableDatabase();

            // Fetch updated data for each day
            String query = "SELECT " + ORDER_DATE + ", SUM(" + ORDER_TOTAL_AMOUNT + ") AS total_amount " +
                    "FROM " + TABLE_ORDERS +
                    " GROUP BY " + ORDER_DATE;
            Cursor cursor = db.rawQuery(query, null);

            while (cursor.moveToNext()) {
                int totalAmountIndex = cursor.getColumnIndex("total_amount");
                int orderDateIndex = cursor.getColumnIndex(ORDER_DATE);

                if (totalAmountIndex != -1 && orderDateIndex != -1) {
                    double totalAmount = cursor.getDouble(totalAmountIndex);
                    String orderDate = cursor.getString(orderDateIndex);

                    Order order = new Order(0, totalAmount, 0, orderDate);
                    updatedData.add(order);
                }
            }

            cursor.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting updated data: " + e.getMessage());
        } finally {
            closeDatabase(db);
        }

        return updatedData;
    }

    // Insert user data into the Users table

    public void insertData(String email, String password) {
        SQLiteDatabase db = null;

        try {
            db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(USER_EMAIL, email);
            contentValues.put(USER_PASSWORD, password);
            db.insertOrThrow(TABLE_USERS, null, contentValues);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error inserting user data: " + e.getMessage());
        } finally {
            closeDatabase(db);
        }
    }

    // Check if an email exists in the Users table
    public boolean checkEmail(String email) {
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + USER_EMAIL + " = ?", new String[]{email});
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }

    // Check if an email and password match in the Users table
    public boolean checkEmailPassword(String email, String password) {
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + USER_EMAIL + " = ? AND " + USER_PASSWORD + " = ?", new String[]{email, password});
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }

    // Close the database if open
    private static void closeDatabase(SQLiteDatabase db) {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    public List<Order> getAllConfirmedOrders() {
        SQLiteDatabase db = null;
        List<Order> orderList = new ArrayList<>();

        try {
            db = getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_ORDERS + " ORDER BY " + ORDER_ID + " DESC";
            Cursor cursor = db.rawQuery(query, null);

            while (cursor.moveToNext()) {
                int orderIdIndex = cursor.getColumnIndex(ORDER_ID);
                int totalAmountIndex = cursor.getColumnIndex(ORDER_TOTAL_AMOUNT);
                int totalItemsIndex = cursor.getColumnIndex(ORDER_TOTAL_ITEMS);
                int orderDateIndex = cursor.getColumnIndex(ORDER_DATE);

                if (orderIdIndex != -1 && totalAmountIndex != -1 && totalItemsIndex != -1 && orderDateIndex != -1) {
                    int orderId = cursor.getInt(orderIdIndex);
                    double totalAmount = cursor.getDouble(totalAmountIndex);
                    int totalItems = cursor.getInt(totalItemsIndex);
                    String orderDate = cursor.getString(orderDateIndex);

                    Order order = new Order(orderId, totalAmount, totalItems, orderDate);
                    orderList.add(order);
                }
            }

            cursor.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting confirmed orders: " + e.getMessage());
        } finally {
            closeDatabase(db);
        }

        return orderList;
    }

}