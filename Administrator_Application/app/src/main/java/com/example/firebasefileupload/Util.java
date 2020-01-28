package com.example.firebasefileupload;

import java.net.MalformedURLException;
import java.net.URL;

class Util {
    private Util(){
    }
    public static boolean isValidUrl(String url){
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}

