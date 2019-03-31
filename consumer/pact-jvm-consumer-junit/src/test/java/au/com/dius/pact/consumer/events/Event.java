package au.com.dius.pact.consumer.events;

import java.time.LocalDateTime;

public class Event {
  private String title;
  private LocalDateTime date;
  private String location;

  //for jackson
  private Event(){}

  public Event(String title, LocalDateTime date, String location) {
    this.title = title;
    this.date = date;
    this.location = location;
  }

  public String getTitle() {
    return title;
  }

  public LocalDateTime getDate() {
    return date;
  }

  public String getLocation() {
    return location;
  }

  @Override
  public String toString() {
    return "Event{" +
      "title='" + title + '\'' +
      ", date=" + date +
      ", location='" + location + '\'' +
      '}';
  }
}
