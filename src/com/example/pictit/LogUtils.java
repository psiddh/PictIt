package com.example.pictit;

public abstract interface LogUtils
{
    public static int LOGLEVEL = 0;
    public static boolean WARN = LOGLEVEL > 1;
    public static boolean DEBUG = LOGLEVEL > 0;
}