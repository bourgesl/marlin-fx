package test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.scene.paint.Color;

public class TextTest extends Application {

    @Override
    public void start(final Stage stage) {
        final StringBuilder sb = new StringBuilder("Text: ");
        final String addText = "01234567890123456789012345678901234567890123456789";

        final Text textNode = new Text(10, 50, sb.toString());
        textNode.setStroke(Color.TRANSPARENT);

        final Label label = new Label("Length:" + sb.length());

        final Button button = new Button("Add text");
        button.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                final String len = "Length: " + sb.length();
                System.out.println(len);
                label.setText(len);

                textNode.setText(sb.append(addText).toString());
            }
        });

        final VBox vbox = new VBox(10);
        vbox.getChildren().add(button);
        vbox.getChildren().add(textNode);
        vbox.getChildren().add(label);
        vbox.setAlignment(Pos.CENTER);

        stage.setScene(new Scene(vbox));
        stage.setWidth(400);
        stage.show();
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
