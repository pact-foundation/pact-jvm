package au.com.dius.pact.consumer.junit.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "project")
@XmlAccessorType(XmlAccessType.FIELD)
public class Project {
  @XmlAttribute(name = "id")
  private int id;
  @XmlAttribute(name = "type")
  private String type;
  @XmlAttribute(name = "name")
  private String name;
  @XmlAttribute(name = "due")
  private String due;

  @XmlElement(name = "tasks")
  private Tasks tasks;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDue() {
    return due;
  }

  public void setDue(String due) {
    this.due = due;
  }

  public Tasks getTasks() {
    return tasks;
  }

  public void setTasks(Tasks tasks) {
    this.tasks = tasks;
  }
}
