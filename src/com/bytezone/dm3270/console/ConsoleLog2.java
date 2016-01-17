package com.bytezone.dm3270.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javafx.scene.control.TextArea;

/*
 * https://www-01.ibm.com/support/knowledgecenter/SSLTBW_2.2.0/com.ibm.zos.v2r2.ieag100/
 * toc.htm
 * The system uses five special screen characters to indicate the status of certain
 * screen messages. These special characters appear in position three, four, or five
 * of the lines in the message area:
 * A vertical line (|) in position three indicates that required action has been
 * taken for the message and the system has deleted the message.
 * A horizontal bar (-) in position three indicates that the message is for
 * information only and requires no action from you.
 * An asterisk (*) in position four indicates that the message is a system message
 * that requires action from you.
 * An at sign (@) in position four indicates that the message is a problem program
 * message that requires action from you.
 * A plus sign (+) in position five indicates that the message is a problem program
 * message that requires no action from you.
 */
public class ConsoleLog2
{
  private static final Pattern messagePattern =
      Pattern.compile ("^...[-| ][* ]\\d\\d(\\.\\d\\d){2} .*");
  private final List<ConsoleMessage> messages = new ArrayList<> ();
  private final TextArea text = new TextArea ();

  public void addLines (List<String> lines)
  {
    List<ConsoleMessage> tempMessages = new ArrayList<> ();
    int max = lines.size ();
    System.out.println ("------------------------------------------------------------");
    System.out.printf ("%d lines to check%n", max);

    for (int i = max - 1; i >= 0; i--)
    {
      String line = lines.get (i);
      if (messagePattern.matcher (line).matches ())
      {
        ConsoleMessage message = new ConsoleMessage ();
        for (int j = i; j < max; j++)
          message.add (lines.get (j));
        max = i;
        tempMessages.add (message);
      }
      else
        System.out.printf ("  rejected: %s%n", line);
    }

    System.out.println ("------------------------------------------------------------");
    for (ConsoleMessage message : tempMessages)
      System.out.println (message);
    System.out.println ("------------------------------------------------------------");
    System.out.printf ("%d lines left over%n", max);
    if (max > 0)
    {
      ConsoleMessage lastMessage = null;

      if (tempMessages.size () > 0)
      {
        ConsoleMessage firstMessage = tempMessages.get (tempMessages.size () - 1);
        int index = getIndex (firstMessage);
        if (index >= 0)
        {
          lastMessage = messages.get (index - 1);
          System.out.println ("found, so check previous");
        }
        else
        {
          lastMessage = messages.get (messages.size () - 1);
          System.out.println ("not found, so check last");
        }
      }
      else
      {
        lastMessage = messages.get (messages.size () - 1);
        System.out.println ("screen has no messages");
      }

      if (lastMessage != null)
        for (int j = 0; j < max; j++)
        {
          String line = lines.get (j);
          // check if this line is contained in the previous message
          if (!lastMessage.contains (line))
          {
            lastMessage.add (lines.get (j));
            text.appendText ("\n" + lines.get (j));
            System.out.println (lines.get (j));
          }
        }
    }

    System.out.println ("------------------------------------------------------------");
    System.out.printf ("%d messages to check%n", tempMessages.size ());
    Collections.reverse (tempMessages);
    for (ConsoleMessage message : tempMessages)
      add (message);
  }

  private void add (ConsoleMessage message)
  {
    System.out.println ("checking:");
    System.out.println (message);
    int last = Math.max (0, messages.size () - 20);
    for (int i = messages.size () - 1; i >= last; i--)
      if (messages.get (i).matches (message))
      {
        System.out.println ("  --> already there");
        return;
      }

    if (messages.size () > 0)
      text.appendText ("\n");
    text.appendText (message.toString ());
    messages.add (message);
    System.out.println ("  --> adding");
  }

  private int getIndex (ConsoleMessage message)
  {
    int last = Math.max (0, messages.size () - 20);
    for (int i = messages.size () - 1; i >= last; i--)
      if (messages.get (i).matches (message))
        return i;
    return -1;
  }

  TextArea getTextArea ()
  {
    return text;
  }

  private void writeMessages ()
  {
    for (ConsoleMessage message : messages)
      System.out.println (message);
  }
}