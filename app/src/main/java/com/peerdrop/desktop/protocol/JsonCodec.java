/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.protocol;

import com.google.gson.Gson;

public class JsonCodec {

    private static final Gson gson = new Gson();

    public static String encode(Object pojo) {
        return gson.toJson(pojo);
    }

    public static <T> T decode(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}