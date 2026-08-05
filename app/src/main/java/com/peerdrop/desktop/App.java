package com.peerdrop.desktop;

import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.FileShareService;
import com.peerdrop.desktop.service.MessageService;
import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.state.PeerRegistry;
import com.peerdrop.desktop.view.ViewManager;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    ViewManager viewManager;

    @Override
    public void start(Stage primaryStage) {
        var appContext = new AppContext();
        viewManager =
                new ViewManager(
                        primaryStage);
        viewManager.showGatewayView(); // Show initial scene
    }
    
    @Override
    public void stop() {
        viewManager.reset();
    }
}

