import database.DBConnection;
import javafx.application.Application;
import javafx.stage.Stage;
import ui.LoginScreen;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
    
        DBConnection.initSchema();

      
        new LoginScreen().show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
