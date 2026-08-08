/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.view;

import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.view.controller.CentralHubController;
import com.peerdrop.desktop.view.controller.GatewayController;
import com.peerdrop.desktop.view.controller.SettingsController;
import com.peerdrop.desktop.viewmodel.CentralHubViewModel;
import com.peerdrop.desktop.viewmodel.GatewayViewModel;

import com.peerdrop.desktop.viewmodel.SettingsViewModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class ViewManager {
    private final Stage stage;
    private final AppContext appContext;

    private GatewayViewModel gatewayViewModel;
    private CentralHubViewModel centralHubViewModel;
    private SettingsViewModel settingsViewModel;

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
                _ -> new GatewayController(getGatewayViewModel()));
    }

    public void showCentralHubView() {
        loadView(
                "/com/peerdrop.desktop.view/CentralHubView.fxml",
                "Central Hub",
                _ -> new CentralHubController(getCentralHubViewModel()));
    }

    public void openSettingsModal() {
        loadModal(
                "/com/peerdrop.desktop.view/SettingsView.fxml",
                _ -> new SettingsController(getSettingsViewModel()));
    }

    public void reset() {
        gatewayViewModel = null;
        centralHubViewModel = null;
        settingsViewModel = null;
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

    public void loadModal(
            String fxmlPath,
            javafx.util.Callback<Class<?>, Object> controllerFactory
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // Manual Dependency Injection via Controller Factory
            loader.setControllerFactory(controllerFactory);

            Parent root = loader.load();

            // Create Stage Modal window
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.WINDOW_MODAL);
            modalStage.initOwner(stage);
            modalStage.initStyle(StageStyle.UNDECORATED); // Modern borderless window

            modalStage.setScene(new Scene(root));
            modalStage.centerOnScreen();
            modalStage.showAndWait();

        } catch (IOException e) {
            throw new RuntimeException("Unable to load view: " + fxmlPath);
        }
    }

    private GatewayViewModel getGatewayViewModel() {
        if (gatewayViewModel == null) {
            gatewayViewModel =
                    new GatewayViewModel(this, appContext);
        }
        return gatewayViewModel;
    }

    private CentralHubViewModel getCentralHubViewModel() {
        if (centralHubViewModel == null) {
            centralHubViewModel =
                    new CentralHubViewModel(
                            this, appContext);
        }
        return centralHubViewModel;
    }

    private SettingsViewModel getSettingsViewModel() {
        if (settingsViewModel == null) {
            settingsViewModel =
                    new SettingsViewModel(appContext);
        }
        return settingsViewModel;
    }
}
