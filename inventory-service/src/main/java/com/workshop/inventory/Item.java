package com.workshop.inventory;

public class Item {

    private final long id;
    private final String name;
    private final int stock;

    public Item(long id, String name, int stock) {
        this.id = id;
        this.name = name;
        this.stock = stock;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public int getStock() { return stock; }
}
