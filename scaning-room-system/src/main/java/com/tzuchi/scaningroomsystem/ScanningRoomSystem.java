package com.tzuchi.scaningroomsystem;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class ScanningRoomSystem extends Application {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, VBox> queueDisplays = new HashMap<>();
    private final Map<String, Label> latestNumberLabels = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tzu Chi Scanning Room System");

        // Main horizontal layout
        HBox mainLayout = new HBox(10);
        mainLayout.setPadding(new Insets(10));
        mainLayout.setStyle("-fx-background-color: white;");

        // Left section for queues
        VBox queuesSection = new VBox(10);
        queuesSection.setPrefWidth(800);
        queuesSection.setMaxHeight(Double.MAX_VALUE);

        // Queue displays container
        HBox queueDisplaysContainer = new HBox(5);
        queueDisplaysContainer.setAlignment(Pos.TOP_CENTER);

        // Add queue displays
        String[] columns = {"2", "5", "8", "6"};
        for (String column : columns) {
            VBox queueDisplay = createQueueDisplay(column);
            queueDisplays.put(column, queueDisplay);
            queueDisplaysContainer.getChildren().add(queueDisplay);
            HBox.setHgrow(queueDisplay, Priority.ALWAYS);
        }

        queuesSection.getChildren().add(queueDisplaysContainer);

        // Right section for video
        VBox videoSection = new VBox();
        videoSection.setPrefWidth(800);
        videoSection.setStyle("""
        -fx-border-color: #2d5d7b;
        -fx-border-width: 2;
        -fx-background-color: #f0f0f0;
        """);

        Label videoPlaceholder = new Label("影像顯示 / Video Display");
        videoPlaceholder.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        videoPlaceholder.setAlignment(Pos.CENTER);
        videoPlaceholder.setMaxWidth(Double.MAX_VALUE);
        videoPlaceholder.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(videoPlaceholder, Priority.ALWAYS);
        videoSection.getChildren().add(videoPlaceholder);

        mainLayout.getChildren().addAll(queuesSection, videoSection);
        HBox.setHgrow(videoSection, Priority.ALWAYS);

        Scene scene = new Scene(mainLayout, 1600, 900);

        // Add keyboard event handling
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case NUMPAD2 -> callNumber("2");
                case NUMPAD5 -> callNumber("5");
                case NUMPAD8 -> callNumber("8");
                case NUMPAD6 -> callNumber("6");
                case NUMPAD1 -> returnNumber("1");
                case NUMPAD4 -> returnNumber("4");
                case NUMPAD7 -> returnNumber("7");
                case NUMPAD3 -> returnNumber("3");
                default -> {}
            }
        });

        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();

        startPeriodicUpdates();
    }

    private VBox createTopSection() {
        VBox topSection = new VBox(10);
        topSection.setAlignment(Pos.CENTER);

        HBox callButtonsRow = new HBox(20);
        callButtonsRow.setAlignment(Pos.CENTER);

        String[] callButtons = {"2", "5", "8", "6"};
        for (String number : callButtons) {
            Button button = createStyledButton("Call " + number);
            button.setOnAction(e -> callNumber(number));
            callButtonsRow.getChildren().add(button);
        }

        topSection.getChildren().add(callButtonsRow);
        return topSection;
    }

    private HBox createMiddleSection() {
        HBox middleSection = new HBox(20);
        middleSection.setAlignment(Pos.CENTER);

        String[] columns = {"2", "5", "8", "6"};
        for (String column : columns) {
            VBox queueDisplay = createQueueDisplay(column);
            queueDisplays.put(column, queueDisplay);
            middleSection.getChildren().add(queueDisplay);
        }

        return middleSection;
    }

    private VBox createBottomSection() {
        VBox bottomSection = new VBox(10);
        bottomSection.setAlignment(Pos.CENTER);

        HBox returnButtonsRow = new HBox(20);
        returnButtonsRow.setAlignment(Pos.CENTER);

        String[] returnButtons = {"1", "4", "7", "3"};
        for (String number : returnButtons) {
            Button button = createStyledButton("Return " + number);
            button.setOnAction(e -> returnNumber(number));
            returnButtonsRow.getChildren().add(button);
        }

        bottomSection.getChildren().add(returnButtonsRow);
        return bottomSection;
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle("""
            -fx-background-color: #2d5d7b;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-padding: 10 20;
            -fx-min-width: 120px;
            """);
        return button;
    }

    private VBox createQueueDisplay(String column) {
        VBox display = new VBox(0);
        display.setAlignment(Pos.TOP_CENTER);
        display.setPadding(new Insets(0));
        display.setPrefWidth(190);
        display.setMaxHeight(Double.MAX_VALUE);
        display.setStyle("""
        -fx-border-color: #2d5d7b;
        -fx-border-width: 1;
        -fx-background-color: white;
        """);

        Label headerLabel = new Label("Column " + column);
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setPadding(new Insets(10));
        headerLabel.setStyle("-fx-background-color: #e8e8e8;");

        VBox queueList = new VBox(0);
        queueList.setAlignment(Pos.TOP_CENTER);
        queueList.setStyle("-fx-background-color: white;");

        // Pre-create empty line labels
        for (int i = 0; i < 300; i++) {
            Label lineLabel = new Label("");
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            lineLabel.setPrefHeight(40);

            // Highlight first 4 boxes
            String backgroundColor = i < 4 ? "#e6f3ff" : "white";
            lineLabel.setStyle(String.format("""
            -fx-border-color: #cccccc;
            -fx-border-width: 0 0 1 0;
            -fx-padding: 10;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-alignment: center;
            -fx-background-color: %s;
            """, backgroundColor));

            queueList.getChildren().add(lineLabel);
        }

        ScrollPane scrollPane = new ScrollPane(queueList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("""
        -fx-background: white;
        -fx-background-color: white;
        -fx-border-width: 0;
        """);

        Label latestLabel = new Label("Latest: -");
        latestLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        latestLabel.setMaxWidth(Double.MAX_VALUE);
        latestLabel.setAlignment(Pos.CENTER);
        latestLabel.setPadding(new Insets(10));
        latestLabel.setStyle("-fx-background-color: #e8e8e8;");
        latestNumberLabels.put(column, latestLabel);

        display.getChildren().addAll(headerLabel, scrollPane, latestLabel);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return display;
    }
    private void callNumber(String column) {
        String endpoint = switch (column) {
            case "2" -> BASE_URL + "/call/highest";
            case "5" -> BASE_URL + "/call/nextE";
            case "6" -> BASE_URL + "/call/nextB";
            case "8" -> BASE_URL + "/call/nextD";
            default -> "";
        };

        if (!endpoint.isEmpty()) {
            System.out.println("Calling next number for column " + column + " at endpoint: " + endpoint);

            sendHttpRequest(endpoint, "POST", response -> {
                System.out.println("Call response received: " + response);

                // Assuming response is patientId, update latest number
                Platform.runLater(() -> {
                    // First update the latest called number
                    updateLatestNumber(column, response);

                    // Then immediately fetch updated queue
                    String queueEndpoint = BASE_URL + "/row" + column;
                    System.out.println("Fetching updated queue from: " + queueEndpoint);

                    sendHttpRequest(queueEndpoint, "GET", queueResponse -> {
                        System.out.println("Queue response received: " + queueResponse);
                        Platform.runLater(() -> {
                            // Update the display with new queue data
                            updateQueueDisplay(column, queueResponse);

                            // Debug print current queue state
                            JsonNode root;
                            try {
                                root = OBJECT_MAPPER.readTree(queueResponse);
                                JsonNode patients = root.get("patients");
                                if (patients != null && patients.isArray()) {
                                    System.out.println("Current queue for column " + column + ":");
                                    for (JsonNode patient : patients) {
                                        System.out.println("Patient: " + patient.toString());
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error parsing queue response: " + e.getMessage());
                            }
                        });
                    });
                });
            });
        }
    }

    private void returnNumber(String button) {
        System.out.println("Return button pressed: " + button);
    }

    private void startPeriodicUpdates() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Starting periodic update..."); // Debug print
                updateAllQueues();
            }
        }, 0, 5000);
    }

    private void updateAllQueues() {
        String[] columns = {"2", "5", "8", "6"};
        for (String column : columns) {
            String endpoint = BASE_URL + "/row" + column;
            System.out.println("Updating queue for column " + column); // Debug print
            sendHttpRequest(endpoint, "GET", response -> {
                Platform.runLater(() -> updateQueueDisplay(column, response));
            });
        }
    }

    private void sendHttpRequest(String endpoint, String method, java.util.function.Consumer<String> responseHandler) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10));

            if (method.equals("POST")) {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            } else {
                requestBuilder.GET();
            }

            // Add debug headers
            requestBuilder.header("Accept", "application/json");

            HttpRequest request = requestBuilder.build();
            System.out.println("Sending " + method + " request to: " + endpoint);

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println(method + " response from " + endpoint);
                        System.out.println("Status code: " + response.statusCode());
                        System.out.println("Response body: " + response.body());

                        if (response.statusCode() == 200) {
                            responseHandler.accept(response.body());
                        } else {
                            Platform.runLater(() -> showError("API Error",
                                    "Request failed with status: " + response.statusCode() +
                                            "\nEndpoint: " + endpoint +
                                            "\nResponse: " + response.body()));
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("Request failed for " + endpoint);
                        e.printStackTrace();
                        Platform.runLater(() -> showError("Connection Error",
                                "Failed to connect to: " + endpoint + "\nError: " + e.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("Error creating request for " + endpoint);
            e.printStackTrace();
        }
    }

    private void updateLatestNumber(String column, String patientId) {
        try {
            Label label = latestNumberLabels.get(column);
            if (label != null && patientId != null) {
                // Remove any quotes and whitespace
                String displayId = patientId.trim().replaceAll("^\"|\"$", "");
                if (!displayId.isEmpty()) {
                    label.setText("Latest: " + displayId);
                    System.out.println("Updated latest number for column " + column + " to: " + displayId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating latest number for column " + column + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateQueueDisplay(String column, String responseBody) {
        try {
            System.out.println("Updating display for column " + column + " with response: " + responseBody);

            VBox queueDisplay = queueDisplays.get(column);
            if (queueDisplay == null) return;

            ScrollPane scrollPane = (ScrollPane) queueDisplay.getChildren().get(1);
            VBox queueList = (VBox) scrollPane.getContent();

            // Clear existing text but maintain highlight colors
            for (int i = 0; i < queueList.getChildren().size(); i++) {
                if (queueList.getChildren().get(i) instanceof Label) {
                    Label label = (Label) queueList.getChildren().get(i);
                    String backgroundColor = i < 4 ? "#e6f3ff" : "white";
                    label.setText("");
                    label.setStyle(String.format("""
                    -fx-border-color: #cccccc;
                    -fx-border-width: 0 0 1 0;
                    -fx-padding: 10;
                    -fx-font-size: 16px;
                    -fx-font-weight: bold;
                    -fx-alignment: center;
                    -fx-background-color: %s;
                    """, backgroundColor));
                }
            }

            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode patients = root.get("patients");

            if (patients != null && patients.isArray()) {
                int index = 0;
                for (JsonNode patient : patients) {
                    if (index >= queueList.getChildren().size()) break;

                    String patientId = patient.has("patientId") ? patient.get("patientId").asText() : "";
                    boolean inQueue = patient.has("inQueue") ? patient.get("inQueue").asBoolean(true) : true;

                    if (!patientId.isEmpty() && inQueue) {
                        Label label = (Label) queueList.getChildren().get(index);
                        String backgroundColor = index < 4 ? "#e6f3ff" : "white";
                        label.setText(patientId);
                        index++;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error updating display for column " + column);
            System.err.println("Response body: " + responseBody);
            e.printStackTrace();
        }
    }
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}