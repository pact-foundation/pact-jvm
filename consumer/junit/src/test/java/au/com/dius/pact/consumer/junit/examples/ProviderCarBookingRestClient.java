package au.com.dius.pact.consumer.junit.examples;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class ProviderCarBookingRestClient {

    public class Person {
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

    public class Car {
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
        Gson gson = new Gson();
        String personStr = Request.Get(baseUrl + "/persons/" + personId)
            .execute().returnContent().asString();
        Person person = gson.fromJson(personStr, Person.class);

        String carDetails = Request.Get(baseUrl + "/cars/" + carId)
            .execute().returnContent().asString();
        Car car = gson.fromJson(carDetails, Car.class);

        String body = "{\n" +
            "\"person\": " + gson.toJson(person) + ",\n" +
            "\"cars\": " + gson.toJson(car) + "\n" +
            "}\n";
        return Request.Post(baseUrl + "/orders/").bodyString(body, ContentType.APPLICATION_JSON)
            .execute().returnResponse();
    }
}
