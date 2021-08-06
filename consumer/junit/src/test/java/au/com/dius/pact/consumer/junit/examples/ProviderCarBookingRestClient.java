package au.com.dius.pact.consumer.junit.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;

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

    public ClassicHttpResponse placeOrder(String baseUrl, String personId, String carId, String date)
        throws IOException {
        String personStr = Request.get(baseUrl + "/persons/" + personId)
            .execute().returnContent().asString();
        ObjectMapper mapper = new ObjectMapper();
        Person person = mapper.readValue(personStr, Person.class);

        String carDetails = Request.get(baseUrl + "/cars/" + carId)
            .execute().returnContent().asString();
        Car car = mapper.readValue(carDetails, Car.class);

        String body = "{\n" +
            "\"person\": " + mapper.writeValueAsString(person) + ",\n" +
            "\"cars\": " + mapper.writeValueAsString(car) + "\n" +
            "}\n";
        return (ClassicHttpResponse) Request.post(baseUrl + "/orders/").bodyString(body, ContentType.APPLICATION_JSON)
            .execute().returnResponse();
    }
}
