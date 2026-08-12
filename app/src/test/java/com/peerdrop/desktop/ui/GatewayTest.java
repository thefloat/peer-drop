/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.ui;

import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.view.ViewManager;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

public class GatewayTest extends ApplicationTest {

    @Mock private AppContext appContext;

    @Override
    public void start(Stage primaryStage) {
        appContext = new AppContext();
        ViewManager viewManager = new ViewManager(
                primaryStage, appContext);
        viewManager.showGatewayView();

        primaryStage.toFront();
    }

    @Test
    public void connect_button_clicks() {
        clickOn("#joinButton");

        verifyThat("#joinButton", hasText("Connect"));
    }

    @Test
    public void connect_with_valid_handle_loads_central_hub() {
        clickOn("#usernameField").write("home-pc");

        clickOn("#joinButton");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(isVisible("#peerListView"));
    }

    private boolean isVisible(String elementId) {
        return lookup(elementId).tryQuery().isPresent();
    }
}
