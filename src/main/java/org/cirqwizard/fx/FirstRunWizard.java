package org.cirqwizard.fx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.cirqwizard.fx.controls.RealNumberTextField;
import org.cirqwizard.fx.util.ExceptionAlert;
import org.cirqwizard.logging.LoggerFactory;
import org.cirqwizard.serial.SerialInterfaceFactory;
import org.cirqwizard.settings.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class FirstRunWizard extends ScreenController implements Initializable
{
    @FXML private AnchorPane firstStepPane;
    @FXML private VBox secondStepVBox;
    @FXML private VBox thirdStepVBox;
    @FXML private VBox isolatingMinerDescriptionVBox;
    @FXML private VBox drillDescriptionVBox;
    @FXML private VBox dispenseDescriptionVBox;
    @FXML private VBox serialPortVBox;
    @FXML private VBox homingStep;
    @FXML private Button onDrillButton;
    @FXML private Button onDispenseButton;
    @FXML private Button onFinishButton;
    @FXML private Button onThirdStepButton;
    @FXML private Button onHomingButton;
    @FXML private ComboBox serialPortComboBox;

    @FXML private RealNumberTextField yAxisDifferenceField;
    @FXML private RealNumberTextField referencePinXField;
    @FXML private RealNumberTextField referencePinYField;

    @FXML private RealNumberTextField xTextField;
    @FXML private RealNumberTextField yTextField;
    @FXML private RealNumberTextField zTextField;
    @FXML private Button homeButton;

    private VBox currentStep;
    private Button currentButton;
    private boolean binded = false;

    @Override
    protected String getFxmlName()
    {
        return "FirstRunWizard.fxml";
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        ManualMovementController.KeyboardHandler keyboardHandler = new ManualMovementController.KeyboardHandler();
        xTextField.addEventFilter(KeyEvent.KEY_PRESSED, keyboardHandler);
        yTextField.addEventFilter(KeyEvent.KEY_PRESSED, keyboardHandler);
        zTextField.addEventFilter(KeyEvent.KEY_PRESSED, keyboardHandler);


        serialPortComboBox.setOnAction(e -> {
            onHomingButton.setDisable(thirdButtonDisabled());
        });


        yAxisDifferenceField.setOnKeyReleased(e -> {
            saveMachineSettings();
            onHomingButton.setDisable(thirdButtonDisabled());
        });

        referencePinXField.setOnKeyReleased(e -> {
            saveMachineSettings();
            onHomingButton.setDisable(thirdButtonDisabled());
        });

        referencePinYField.setOnKeyReleased(e -> {
            saveMachineSettings();
            onHomingButton.setDisable(thirdButtonDisabled());
        });
    }

    public void toSecondStep()
    {
        firstStepPane.setVisible(false);
        secondStepVBox.setVisible(true);

        List<String> interfaces = SerialInterfaceFactory.getSerialInterfaces(getMainApplication().getSerialInterface());
        UserPreference<String> port = SettingsFactory.getApplicationSettings().getSerialPort();
        serialPortComboBox.setItems(FXCollections.observableArrayList(interfaces));
        serialPortComboBox.setValue(port.getValue());

        yAxisDifferenceField.setIntegerValue(getYAxisDifference());
        referencePinXField.setIntegerValue(getReferencePinXField());
        referencePinYField.setIntegerValue(getReferencePinYField());

        onHomingButton.setDisable(thirdButtonDisabled());
    }

    public void toThirdStep()
    {
        saveMachineSettings();

        homingStep.setVisible(false);
        thirdStepVBox.setVisible(true);

        thirdStepChangeDescription(isolatingMinerDescriptionVBox, onDrillButton);
    }

    public void toHoming()
    {
        secondStepVBox.setVisible(false);
        homingStep.setVisible(true);
    }

    private void thirdStepChangeDescription(VBox step, Button button)
    {
        if (currentStep != null)
            currentStep.setVisible(false);

        currentStep = step;
        currentStep.setVisible(true);

        if (currentButton != null)
            currentButton.setVisible(false);

        currentButton = button;
        currentButton.setVisible(true);
    }

    public void onFinish()
    {
        double zOffset = Double.parseDouble(zTextField.getRealNumberText());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation");
        alert.setHeaderText(String.format("Use %.2f as dispensing z offset?", zOffset));
        alert.showAndWait().filter(response -> response == ButtonType.YES).ifPresent(response ->
        {
            DispensingSettings dispensingSettings = SettingsFactory.getDispensingSettings();

            UserPreference userPreference = dispensingSettings.getZOffset();
            userPreference.setValue(zTextField.getIntegerValue());
            dispensingSettings.setZOffset(userPreference);

            dispensingSettings.save();

            getMainApplication().showMainApplication();
        });
    }

    public void onDrill()
    {
        double zOffset = Double.parseDouble(zTextField.getRealNumberText());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation");
        alert.setHeaderText(String.format("Use %.2f as isolation milling z offset?", zOffset));
        alert.showAndWait().filter(response -> response == ButtonType.YES).ifPresent(response ->
        {
            ToolLibrary toolLibrary = new ToolLibrary();
            ToolSettings toolSettings = ToolLibrary.getDefaultTool();
            toolSettings.setZOffset(zTextField.getIntegerValue());
            toolLibrary.setToolSettings(new ToolSettings[]{toolSettings});
            toolLibrary.save();

            thirdStepChangeDescription(drillDescriptionVBox, onDispenseButton);
        });
    }

    public void onDispense()
    {
        double zOffset = Double.parseDouble(zTextField.getRealNumberText());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation");
        alert.setHeaderText(String.format("Use %.2f as drilling and contour milling z offset?", zOffset));
        alert.showAndWait().filter(response -> response == ButtonType.YES).ifPresent(response ->
        {
            DrillingSettings drillingSettings = SettingsFactory.getDrillingSettings();
            UserPreference drillingZOffset = drillingSettings.getZOffset();
            drillingZOffset.setValue(zTextField.getIntegerValue());

            drillingSettings.setZOffset(drillingZOffset);
            drillingSettings.save();

            ContourMillingSettings contourMillingSettings = SettingsFactory.getContourMillingSettings();
            UserPreference contourMillingZOffset = contourMillingSettings.getZOffset();
            contourMillingZOffset.setValue(zTextField.getIntegerValue());

            contourMillingSettings.setZOffset(contourMillingZOffset);
            contourMillingSettings.save();

            thirdStepChangeDescription(dispenseDescriptionVBox, onFinishButton);
        });
    }

    public void home()
    {
        try
        {
            if (getMainApplication().getCNCController() == null)
            {
                Object serialPort = serialPortComboBox.getValue();

                getMainApplication().connectSerialPort(serialPort.toString());
                TimeUnit.SECONDS.sleep(1);

                for (int i = 0; i < 5 && getMainApplication().getCNCController() == null; ++i)
                {
                    TimeUnit.SECONDS.sleep(1);
                }

                if (getMainApplication().getCNCController() == null)
                {
                    throw new Exception("Establishing connection with cirqoid has failed.");
                }
            }

            getMainApplication().getCNCController().home(yAxisDifferenceField.getIntegerValue());
            onThirdStepButton.setDisable(false);
        }
        catch (Exception ex)
        {
            LoggerFactory.logException("Communication with controller failed: ", ex);
            ExceptionAlert alert = new ExceptionAlert("Oops! That's embarrassing!", "Communication error",
                    "Something went wrong while communicating with the controller. " +
                            "The most sensible thing to do now would be to close the program and start over again. Sorry about that.", ex);
            alert.showAndWait();
        }
    }

    private void saveMachineSettings()
    {
        Object serialPort = serialPortComboBox.getValue();
        if (serialPort != null)
        {
            ApplicationSettings applicationSettings = SettingsFactory.getApplicationSettings();
            UserPreference serialPortPreference = applicationSettings.getSerialPort();
            serialPortPreference.setValue(serialPort.toString());
            applicationSettings.setSerialPort(serialPortPreference);
            applicationSettings.save();
        }

        // saving settings
        MachineSettings machineSettings = SettingsFactory.getMachineSettings();

        UserPreference yAxisDifference = machineSettings.getYAxisDifference();
        yAxisDifference.setValue(yAxisDifferenceField.getIntegerValue());

        UserPreference referencePinX = machineSettings.getReferencePinX();
        referencePinX.setValue(referencePinXField.getIntegerValue());

        UserPreference referencePinY = machineSettings.getReferencePinY();
        referencePinY.setValue(referencePinYField.getIntegerValue());

        machineSettings.setYAxisDifference(yAxisDifference);
        machineSettings.setReferencePinX(referencePinX);
        machineSettings.setReferencePinY(referencePinY);
        machineSettings.save();
    }

    private Integer getYAxisDifference()
    {
        // saving settings
        MachineSettings machineSettings = SettingsFactory.getMachineSettings();

        UserPreference yAxisDifference = machineSettings.getYAxisDifference();
        return (Integer) yAxisDifference.getValue();
    }

    private Integer getReferencePinXField()
    {
        // saving settings
        MachineSettings machineSettings = SettingsFactory.getMachineSettings();

        UserPreference referencePinX = machineSettings.getReferencePinX();
        return (Integer) referencePinX.getValue();
    }

    private Integer getReferencePinYField()
    {
        // saving settings
        MachineSettings machineSettings = SettingsFactory.getMachineSettings();

        UserPreference referencePinY = machineSettings.getReferencePinY();
        return (Integer) referencePinY.getValue();
    }

    private boolean thirdButtonDisabled()
    {
        return referencePinXField.getIntegerValue() == null || referencePinXField.getIntegerValue() == 0 ||
               referencePinYField.getIntegerValue() == null || referencePinYField.getIntegerValue() == 0 ||
               yAxisDifferenceField.getIntegerValue() == null || yAxisDifferenceField.getIntegerValue() == 0 ||
               serialPortComboBox.getValue() == null;
    }

    public void goToUpdatedCoordinates()
    {
        getMainApplication().getCNCController().moveTo(xTextField.getIntegerValue(), yTextField.getIntegerValue(),
                zTextField.getIntegerValue());
    }
}
