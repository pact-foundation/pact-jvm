package au.com.dius.pact.provider.spring;

import org.joda.time.DateTime;

import java.util.UUID;

public class Book {
    private UUID id;
    private String author;
    private boolean bestSeller;
    private DateTime createdOn;

    public Book() {}

    public Book(UUID id, String author, boolean bestSeller, DateTime createdOn) {
        this.id = id;
        this.author = author;
        this.bestSeller = bestSeller;
        this.createdOn = createdOn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isBestSeller() {
        return bestSeller;
    }

    public void setBestSeller(boolean bestSeller) {
        this.bestSeller = bestSeller;
    }

    public DateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(DateTime createdOn) {
        this.createdOn = createdOn;
    }

}
