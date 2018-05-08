/*
This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 3 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.cirqwizard.fx.services;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.cirqoid.cnc.controller.commands.Command;
import org.cirqoid.cnc.controller.serial.SerialException;
import org.cirqwizard.fx.MainApplication;
import org.cirqwizard.fx.util.ExceptionAlert;
import org.cirqwizard.logging.LoggerFactory;

import java.util.List;


public class SerialInterfaceCommandsService extends Service
{
    private MainApplication mainApplication;
    private List<Command> commands;
    private Property<String> executionTime = new SimpleStringProperty("");
    private Property<String> responses = new SimpleStringProperty("");
    private boolean readResponses;
    private boolean suppressExceptions;

    public SerialInterfaceCommandsService(MainApplication mainApplication)
    {
        this.mainApplication = mainApplication;
    }

    public void setCommands(List<Command> commands)
    {
        this.commands = commands;
    }

    public Property<String> executionTimeProperty()
    {
        return executionTime;
    }

    public Property<String> responsesProperty()
    {
        return responses;
    }

    @Override
    protected Task createTask()
    {
        return new SerialInterfaceTask();
    }

    private String formatTime(long time)
    {
        return String.format("%02d:%02d:%02d", time / 3600, (time % 3600) / 60, time % 60);
    }

    public class SerialInterfaceTask extends Task
    {
        @Override
        protected Object call() throws Exception
        {
            try
            {
                long executionStartTime = System.currentTimeMillis();
                for (int i = 0; i < commands.size(); i++)
                {
                    if (isCancelled())
                    {
                        if (i > 0)
                        {
                            Command lastCommand = commands.get( i - 1);
                            mainApplication.getCNCController().getInterpreter().setContext(lastCommand.getContext());
                        }

                        throw new InterruptedException();
                    }

                    mainApplication.getCNCController().send(commands.get(i));
                    updateProgress(i, commands.size());
                    final String s = formatTime((System.currentTimeMillis() - executionStartTime) / 1000);
                    Platform.runLater(() -> executionTime.setValue(s));
                }
            }
            catch (SerialException e)
            {
                LoggerFactory.logException("Error communicating with the controller", e);
                ExceptionAlert alert = new ExceptionAlert("Oops! That's embarrassing!", "Communication error",
                        "Something went wrong while communicating with the controller. " +
                                "The most sensible thing to do now would be to close the program and start over again. Sorry about that.", e);
                alert.showAndWait();
            }
            catch (InterruptedException e)
            {
                mainApplication.getCNCController().interruptProgram();
            }
            return null;
        }
    }
}
