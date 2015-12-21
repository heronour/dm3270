package com.bytezone.dm3270.assistant;

import com.bytezone.dm3270.application.ConsolePane;
import com.bytezone.dm3270.commands.AIDCommand;
import com.bytezone.dm3270.display.Field;
import com.bytezone.dm3270.display.ScreenChangeListener;
import com.bytezone.dm3270.display.ScreenWatcher;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

public class TSOCommand implements ScreenChangeListener
{
  private final HBox hbox = new HBox (10);

  private final Label lblCommand = new Label ("TSO Command");
  final TextField txtCommand = new TextField ();
  final Button btnExecute = new Button ("Execute");

  private ConsolePane consolePane;
  private ScreenWatcher screenDetails;

  public TSOCommand ()
  {
    txtCommand.setEditable (true);
    txtCommand.setPrefWidth (500);
    txtCommand.setFont (Font.font ("Monospaced", 12));
    txtCommand.setFocusTraversable (false);

    btnExecute.setOnAction (e -> execute ());

    hbox.getChildren ().addAll (lblCommand, txtCommand, btnExecute);
    hbox.setAlignment (Pos.CENTER_LEFT);
  }

  public HBox getBox ()
  {
    return hbox;
  }

  public void setConsolePane (ConsolePane consolePane)
  {
    this.consolePane = consolePane;
  }

  private void execute ()
  {
    if (screenDetails == null || consolePane == null)
      return;

    Field tsoCommandField = screenDetails.getTSOCommandField ();
    if (tsoCommandField == null)
    {
      System.out.println ("Null TSO command field");
      return;
    }
    String command = txtCommand.getText ();

    if (command.length () > tsoCommandField.getDisplayLength ())
    {
      System.out.println ("Command is too long for the input field");
      System.out.printf ("Field: %d, command: %d%n", tsoCommandField.getDisplayLength (),
                         command.length ());
      return;
    }

    if (!command.isEmpty ())
    {
      tsoCommandField.setText (command);
      consolePane.sendAID (AIDCommand.AID_ENTER, "ENTR");
    }
  }

  @Override
  public void screenChanged (ScreenWatcher screenDetails)
  {
    this.screenDetails = screenDetails;
  }
}