package com.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "db_downtime_store")
public class DbDowntimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Automatically generate IDs
    private int id;

    private String downtimeTimestamp;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDowntimeTimestamp() {
        return downtimeTimestamp;
    }

    public void setDowntimeTimestamp(String downtimeTimestamp) {
        this.downtimeTimestamp = downtimeTimestamp;
    }
}