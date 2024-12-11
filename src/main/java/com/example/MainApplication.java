package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.image.Image;
public class MainApplication extends Application {
    private Parent logVisRoot;
    private Parent analyticsRoot;
    private Parent filterRoot;
    private Parent chatbotRoot;
    private LogVisController logVisController;
    private LogAnalyticsController analyticsController;
    private FilterController filterController;
    private ChatbotController chatbotController;
    private StackPane mainContainer;
    
    private static MainApplication instance;
    
    public MainApplication() {
        instance = this;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainContainer = new StackPane();
        
        FXMLLoader logVisLoader = new FXMLLoader(getClass().getResource("/logVis.fxml"));
        logVisRoot = logVisLoader.load();
        logVisController = logVisLoader.getController();
        
        FXMLLoader analyticsLoader = new FXMLLoader(getClass().getResource("/logAnalytics.fxml"));
        analyticsRoot = analyticsLoader.load();
        analyticsController = analyticsLoader.getController();

        FXMLLoader filterLoader = new FXMLLoader(getClass().getResource("/filter.fxml"));
        filterRoot = filterLoader.load();
        filterController = filterLoader.getController();

        FXMLLoader chatbotLoader = new FXMLLoader(getClass().getResource("/chatbot.fxml"));
        chatbotRoot = chatbotLoader.load();
        chatbotController = chatbotLoader.getController();

        logVisController.setAnalyticsController(analyticsController);
        
        setupViewSwitching();
        
        mainContainer.getChildren().add(analyticsRoot);
        
        Scene scene = new Scene(mainContainer, 1280, 670);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setTitle("LogFlow Analytics App");
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResource("/icon.png").toExternalForm()));    
        primaryStage.setOnCloseRequest(event -> {
            logVisController.shutdown();
            Platform.exit();
            System.exit(0);
        });
        
        primaryStage.show();
        
        logVisController.startLogStreaming();
    }

    private void setupViewSwitching() {
        Label dashboardLabel = logVisController.getDashboardLabel();
        Label logsLabel = logVisController.getLogsLabel();
        Label filterLabel = logVisController.getFilterLabel();
        Label chatbotLabel = logVisController.getChatbotLabel();
        
        dashboardLabel.setOnMouseClicked(event -> {
            try {
                setRoot("logAnalytics");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            dashboardLabel.getStyleClass().add("nav-item-active");
            logsLabel.getStyleClass().remove("nav-item-active");
            filterLabel.getStyleClass().remove("nav-item-active");
            chatbotLabel.getStyleClass().remove("nav-item-active");
        });
        
        logsLabel.setOnMouseClicked(event -> {
            try {
                setRoot("logVis");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            logsLabel.getStyleClass().add("nav-item-active");
            dashboardLabel.getStyleClass().remove("nav-item-active");
            filterLabel.getStyleClass().remove("nav-item-active");
            chatbotLabel.getStyleClass().remove("nav-item-active");
        });

        filterLabel.setOnMouseClicked(event -> {
            try {
                setRoot("filter");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            filterLabel.getStyleClass().add("nav-item-active");
            dashboardLabel.getStyleClass().remove("nav-item-active");
            logsLabel.getStyleClass().remove("nav-item-active");
            chatbotLabel.getStyleClass().remove("nav-item-active");
        });
        chatbotLabel.setOnMouseClicked(event -> {
            try {
                setRoot("chatbot");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            chatbotLabel.getStyleClass().add("nav-item-active");
            dashboardLabel.getStyleClass().remove("nav-item-active");
            filterLabel.getStyleClass().remove("nav-item-active");
            logsLabel.getStyleClass().remove("nav-item-active");


        });
    }

    public static void setRoot(String viewName) throws Exception {
        if (instance == null) {
            throw new IllegalStateException("MainApplication not initialized");
        }
        
        instance.mainContainer.getChildren().clear();
        
        switch (viewName) {
            case "logVis":
                instance.mainContainer.getChildren().add(instance.logVisRoot);
                break;
            case "logAnalytics":
                instance.mainContainer.getChildren().add(instance.analyticsRoot);
                break;
            case "filter":
                instance.mainContainer.getChildren().add(instance.filterRoot);
                break;
            case "chatbot":
                instance.mainContainer.getChildren().add(instance.chatbotRoot);
                break;
            default:
                throw new IllegalArgumentException("Unknown view: " + viewName);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}