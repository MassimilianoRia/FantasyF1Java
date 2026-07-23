package it.unibo.fantasyf1;

import it.unibo.fantasyf1.service.ApplicationServices;
import it.unibo.fantasyf1.ui.user.FxTaskRunner;
import it.unibo.fantasyf1.ui.user.UserApplicationView;

import javafx.application.Application;
import javafx.stage.Stage;

public final class FantasyF1Application extends Application {

    private FxTaskRunner taskRunner;
    private UserApplicationView applicationView;

    @Override
    public void start(final Stage stage) {
        taskRunner = new FxTaskRunner();
        applicationView = new UserApplicationView(
            stage,
            ApplicationServices.production(),
            taskRunner
        );
        applicationView.show();
    }

    @Override
    public void stop() {
        if (applicationView != null) {
            applicationView.close();
        }
        if (taskRunner != null) {
            taskRunner.close();
        }
    }
}
