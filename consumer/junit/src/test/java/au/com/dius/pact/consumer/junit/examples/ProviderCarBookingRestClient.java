package au.com.dius.pact.consumer.junit.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class ProviderCarBookingRestClient {

    public static class Person {
        private String id;
        private String firstName;
        private String lastName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    public static class Car {
        private String id;
        private String brand;
        private String model;
        private Integer year;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getBrand() {
            return brand;
        }

        public void setBrand(String brand) {
            this.brand = brand;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }
    }

    public HttpResponse placeOrder(String baseUrl, String personId, String carId, String date)
        throws IOException {
        String personStr = Request.Get(baseUrl + "/persons/" + personId)
            .execute().returnContent().asString();
        ObjectMapper mapper = new ObjectMapper();
        Person person = mapper.readValue(personStr, Person.class);

        String carDetails = Request.Get(baseUrl + "/cars/" + carId)
            .execute().returnContent().asString();
        Car car = mapper.readValue(carDetails, Car.class);

        String body = "{\n" +
            "\"person\": " + mapper.writeValueAsString(person) + ",\n" +
            "\"cars\": " + mapper.writeValueAsString(car) + "\n" +
            "}\n";
        return Request.Post(baseUrl + "/orders/").bodyString(body, ContentType.APPLICATION_JSON)
            .execute().returnResponse();
    }
}
