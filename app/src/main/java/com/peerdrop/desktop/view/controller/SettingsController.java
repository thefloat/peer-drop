package com.peerdrop.desktop.view.controller;

import com.peerdrop.desktop.viewmodel.SettingsViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;

import java.net.NetworkInterface;

public class SettingsController {

    @FXML private ComboBox<NetworkInterface> interfaceComboBox;
    @FXML private Label lblSystemName;
    @FXML private Label lblMacAddress;
    @FXML private Label lblMtu;
    @FXML private Label lblStatusFlags;
    @FXML private Label lblIpAddresses;
    @FXML private Button btnSave;

    private final SettingsViewModel viewModel;

    public SettingsController(SettingsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        setupDataBindings();
        setupInterfaceComboBoxCellFactory();
    }

    private void setupDataBindings() {
        // Bind items & selected value
        interfaceComboBox.setItems(viewModel.getAvailableInterfaces());
        interfaceComboBox.valueProperty().bindBidirectional(viewModel.selectedInterfaceProperty());

        // Bind details
        lblSystemName.textProperty().bind(viewModel.interfaceIdProperty());
        lblMacAddress.textProperty().bind(viewModel.macAddressProperty());
        lblMtu.textProperty().bind(viewModel.mtuValueProperty());
        lblStatusFlags.textProperty().bind(viewModel.statusFlagsProperty());
        lblIpAddresses.textProperty().bind(viewModel.ipAddressesProperty());

        // Bind Apply button disabling logic to ViewModel dirty property
        btnSave.disableProperty().bind(viewModel.isDirtyProperty().not());
    }

    private void setupInterfaceComboBoxCellFactory() {
        // Custom cell renderer for ComboBox to display human-readable names
        interfaceComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(NetworkInterface item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDisplayName());
            }
        });
        interfaceComboBox.setButtonCell(interfaceComboBox.getCellFactory().call(null));
    }

    @FXML
    private void handleSave() {
        viewModel.applyAndSave();
        closeModal();
    }

    @FXML
    private void handleClose() {
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) interfaceComboBox.getScene().getWindow();
        stage.close();
    }
}
