package com.peerdrop.desktop.view.controller;

import com.peerdrop.desktop.viewmodel.GatewayViewModel;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class GatewayController {
    private final GatewayViewModel viewModel;

    @FXML private TextField usernameField;
    @FXML private Label statusLabel;
    @FXML private Button joinButton;

    public GatewayController(GatewayViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        // 1. Two-way data binding: UI changes update the model, and vice versa
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());

        // 2. One-way binding: UI reflects model status updates automatically
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // Disable input elements automatically while a connection is processing
        joinButton.disableProperty().bind(viewModel.isConnectingProperty());
        usernameField.disableProperty().bind(viewModel.isConnectingProperty());
    }

    @FXML
    public void handleLogin() {
        viewModel.join();
    }
}
