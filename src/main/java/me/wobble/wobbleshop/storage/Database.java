package me.wobble.wobbleshop.storage;

import java.sql.Connection;

public interface Database {

    void initialize();

    Connection getConnection();

    void close();
}
