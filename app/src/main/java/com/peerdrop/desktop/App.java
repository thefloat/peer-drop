package com.peerdrop.desktop;

import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.view.ViewManager;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    ViewManager viewManager;
    AppContext appContext;

    @Override
    public void start(Stage primaryStage) {
        appContext = new AppContext();
        viewManager =
                new ViewManager(
                        primaryStage, appContext);
        viewManager.showGatewayView();
    }
    
    @Override
    public void stop() {
        viewManager.reset();
        appContext.reset();
    }
}

