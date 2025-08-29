package com.example.healthrxchallenge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class HealthrxChallengeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthrxChallengeApplication.class, args);
    }
}

/**
 * This component runs on application startup. It handles the entire challenge logic.
 */
@Component
class ChallengeRunner implements CommandLineRunner {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // Jackson's object mapper for JSON handling

    // Constructor injection for RestTemplateBuilder and ObjectMapper
    public ChallengeRunner(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Application started. Executing the HealthRx challenge...");

        try {
            // Step 1 & 2: Generate Webhook and get the access token
            WebhookDetails webhookDetails = generateWebhook();
            if (webhookDetails == null) {
                System.err.println("Failed to generate webhook. Aborting.");
                return;
            }

            System.out.println("Successfully generated webhook URL: " + webhookDetails.getWebhookUrl());
            System.out.println("Received Access Token.");

            // Step 3: Solve the SQL problem.
            // Your registration number 22bce8946 ends in 46 (even), so we solve Question 2.
            String finalQuery = getSolutionForQuestion2();
            System.out.println("\nFinal SQL Query to be submitted:\n" + finalQuery);


            // Step 4: Submit the solution to the webhook URL
            submitSolution(webhookDetails, finalQuery);

        } catch (Exception e) {
            System.err.println("An error occurred during the challenge execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends the initial POST request to generate the webhook and retrieve the access token.
     * @return WebhookDetails containing the URL and token, or null on failure.
     */
    private WebhookDetails generateWebhook() {
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "Hari vamsi Nagubilli"); // <-- IMPORTANT: Change to your name
        requestBody.put("regNo", "22bce8946"); // Your registration number
        requestBody.put("email", "hari381937vamsi@gmail.com"); // <-- IMPORTANT: Change to your email

        // Create the HTTP entity
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        System.out.println("Sending request to generate webhook...");
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse the JSON response to extract webhook and accessToken
                JsonNode root = objectMapper.readTree(response.getBody());
                String webhookUrl = root.path("webhook").asText();
                String accessToken = root.path("accessToken").asText();
                return new WebhookDetails(webhookUrl, accessToken);
            } else {
                System.err.println("Failed to generate webhook. Status: " + response.getStatusCode());
                System.err.println("Response Body: " + response.getBody());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error while generating webhook: " + e.getMessage());
            return null;
        }
    }

    /**
     * Provides the final SQL query for Question 2.
     * Problem: Calculate the number of employees younger than each employee in the same department.
     * @return A string containing the final SQL query.
     */
    private String getSolutionForQuestion2() {
        return "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, " +
                "(SELECT COUNT(*) FROM EMPLOYEE e2 WHERE e2.DEPARTMENT = e1.DEPARTMENT AND e2.DOB > e1.DOB) AS YOUNGER_EMPLOYEES_COUNT " +
                "FROM EMPLOYEE e1 " +
                "JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID " +
                "ORDER BY e1.EMP_ID DESC;";
    }

    /**
     * Submits the final SQL query to the provided webhook URL.
     * @param details The webhook details (URL and token).
     * @param finalQuery The SQL query to submit.
     */
    private void submitSolution(WebhookDetails details, String finalQuery) {
        String url = details.getWebhookUrl();

        // Set headers, including the JWT for Authorization
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", details.getAccessToken()); // Sets "Authorization: Bearer <token>"

        // Create the request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("finalQuery", finalQuery);

        // Create the HTTP entity
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        System.out.println("\nSubmitting final solution to: " + url);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully submitted the solution!");
                System.out.println("Response: " + response.getBody());
            } else {
                System.err.println("Failed to submit solution. Status: " + response.getStatusCode());
                System.err.println("Response Body: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error while submitting solution: " + e.getMessage());
        }
    }

    /**
     * A simple helper class to store the webhook URL and access token.
     */
    private static class WebhookDetails {
        private final String webhookUrl;
        private final String accessToken;

        public WebhookDetails(String webhookUrl, String accessToken) {
            this.webhookUrl = webhookUrl;
            this.accessToken = accessToken;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public String getAccessToken() {
            return accessToken;
        }
    }
}
