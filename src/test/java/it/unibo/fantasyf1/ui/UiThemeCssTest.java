package it.unibo.fantasyf1.ui;

import javafx.css.CssParser;
import javafx.css.Stylesheet;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiThemeCssTest {

    @Test
    void sharedStylesheetIsPackagedAndValid() throws Exception {
        final URL resource = UiThemeCssTest.class.getResource(
            "/it/unibo/fantasyf1/ui/fantasy-f1.css"
        );
        assertNotNull(resource);

        CssParser.errorsProperty().clear();
        final Stylesheet stylesheet = new CssParser().parse(resource);

        assertNotNull(stylesheet);
        assertTrue(
            CssParser.errorsProperty().isEmpty(),
            () -> "Errori nel tema JavaFX: " + CssParser.errorsProperty()
        );
    }
}
