package com.peerdrop.desktop.view;

import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.view.controller.CentralHubController;
import com.peerdrop.desktop.view.controller.GatewayController;
import com.peerdrop.desktop.viewmodel.CentralHubViewModel;
import com.peerdrop.desktop.viewmodel.GatewayViewModel;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewManager {
    private final Stage stage;

    private final AppContext appContext;

    private GatewayViewModel gatewayViewModel;
    private CentralHubViewModel centralHubViewModel;

    public ViewManager(
            Stage stage, AppContext appContext
    ) {
        this.stage = stage;
        this.appContext = appContext;
    }

    public void showGatewayView() {
        loadView(
                "/com/peerdrop.desktop.view/GatewayView.fxml",
                "Gateway - PeerDrop",
                _ -> new GatewayController(getWelcomeViewModel()));
    }

    public void showCentralHubView() {
        loadView(
                "/com/peerdrop.desktop.view/CentralHubView.fxml",
                "Central Hub",
                _ -> new CentralHubController(getMainViewModel()));
    }

    public void reset() {
        gatewayViewModel = null;
        centralHubViewModel = null;
    }

    private void loadView (
            String fxmlPath,
            String title,
            javafx.util.Callback<Class<?>, Object> controllerFactory
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // Manual Dependency Injection via Controller Factory
            loader.setControllerFactory(controllerFactory);

            Parent root = loader.load();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load view: " + fxmlPath);
        }
    }

    private GatewayViewModel getWelcomeViewModel() {
        if (gatewayViewModel == null) {
            gatewayViewModel =
                    new GatewayViewModel(this, appContext);
        }
        return gatewayViewModel;
    }

    private CentralHubViewModel getMainViewModel() {
        if (centralHubViewModel == null) {
            centralHubViewModel =
                    new CentralHubViewModel(
                            this, appContext);
        }
        return centralHubViewModel;
    }
}
