package com.example.sidd.mondo;


import android.provider.BaseColumns;

public class Table {
    public Table(String tabname){


    }

    public static abstract class TableInfo implements BaseColumns
    {

        public static final String routename = "routename";
        public static final String lat = "lat";
        public static final String lon = "lon";
        public static final String database_name = "user_info";
        public static String table_name = "myroute";
    }
}

