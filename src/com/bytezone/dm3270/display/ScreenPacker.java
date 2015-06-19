package com.bytezone.dm3270.display;

import java.util.List;

import com.bytezone.dm3270.attributes.Attribute;
import com.bytezone.dm3270.attributes.Attribute.AttributeType;
import com.bytezone.dm3270.attributes.StartFieldAttribute;
import com.bytezone.dm3270.commands.AIDCommand;
import com.bytezone.dm3270.orders.BufferAddress;
import com.bytezone.dm3270.orders.Order;
import com.bytezone.dm3270.structuredfields.SetReplyMode;

public class ScreenPacker
{
  private final Screen screen;
  private final byte[] buffer = new byte[4096];

  private byte replyMode;
  private byte[] replyTypes;

  public ScreenPacker (Screen screen)
  {
    this.screen = screen;
  }

  public AIDCommand readModifiedFields (byte currentAID, int cursorLocation,
      FieldManager fieldManager, boolean readModifiedAll)
  {
    // pack the AID
    int ptr = 0;
    buffer[ptr++] = currentAID;               // whatever key was pressed

    // PA keys and the CLR key only return the AID byte
    if (!readModifiedAll)
      if (currentAID == AIDCommand.AID_PA1 || currentAID == AIDCommand.AID_PA2
          || currentAID == AIDCommand.AID_PA3 || currentAID == AIDCommand.AID_CLEAR)
        return new AIDCommand (screen, buffer, 0, ptr);

    // pack the cursor address
    BufferAddress ba = new BufferAddress (cursorLocation);
    ptr = ba.packAddress (buffer, ptr);

    Field tsoCommandField = fieldManager.getTSOCommandField ();

    // pack all modified fields
    for (Field field : fieldManager.getUnprotectedFields ())
      if (field.isModified ())
      {
        ptr = packField (field, buffer, ptr);
        if (field == tsoCommandField)
          System.out.println ("User command : " + field.getText ().trim ());
      }

    return new AIDCommand (screen, buffer, 0, ptr);
  }

  public AIDCommand readBuffer (ScreenPosition[] screenPositions, int cursorLocation,
      byte currentAID, byte replyMode, byte[] replyTypes)
  {
    this.replyMode = replyMode;
    this.replyTypes = replyTypes;

    // pack the AID
    int ptr = 0;
    buffer[ptr++] = currentAID;

    // pack the cursor address
    BufferAddress ba = new BufferAddress (cursorLocation);
    ptr = ba.packAddress (buffer, ptr);

    // pack every screen location
    for (ScreenPosition sp : screenPositions)
      if (sp.isStartField ())
        ptr = packStartPosition (sp, buffer, ptr);
      else
        ptr = packDataPosition (sp, buffer, ptr);       // don't suppress nulls

    return new AIDCommand (screen, buffer, 0, ptr);
  }

  private int packStartPosition (ScreenPosition sp, byte[] buffer, int ptr)
  {
    assert sp.isStartField ();

    StartFieldAttribute sfa = sp.getStartFieldAttribute ();

    if (replyMode == SetReplyMode.RM_FIELD)
    {
      buffer[ptr++] = Order.START_FIELD;
      buffer[ptr++] = sfa.getAttributeValue ();
    }
    else
    {
      buffer[ptr++] = Order.START_FIELD_EXTENDED;

      List<Attribute> attributes = sp.getAttributes ();
      buffer[ptr++] = (byte) (attributes.size () + 1);    // +1 for StartFieldAttribute

      ptr = sfa.pack (buffer, ptr);                       // pack the SFA first
      for (Attribute attribute : attributes)
        ptr = attribute.pack (buffer, ptr);               // then pack the rest
    }
    return ptr;
  }

  private int packDataPosition (ScreenPosition sp, byte[] buffer, int ptr)
  {
    if (replyMode == SetReplyMode.RM_CHARACTER)
      for (Attribute attribute : sp.getAttributes ())
        if (attribute.getAttributeType () == AttributeType.RESET)
        {
          buffer[ptr++] = Order.SET_ATTRIBUTE;
          ptr = attribute.pack (buffer, ptr);
        }
        else
          for (byte b : replyTypes)
            if (attribute.matches (b))
            {
              buffer[ptr++] = Order.SET_ATTRIBUTE;
              ptr = attribute.pack (buffer, ptr);
              break;
            }

    if (sp.isGraphicsChar () && replyMode != SetReplyMode.RM_FIELD)
      buffer[ptr++] = Order.GRAPHICS_ESCAPE;

    buffer[ptr++] = sp.getByte ();

    return ptr;
  }

  private int packField (Field field, byte[] buffer, int ptr)
  {
    assert field.isModified ();

    for (ScreenPosition sp : field)
      if (sp.isStartField ())
      {
        buffer[ptr++] = Order.SET_BUFFER_ADDRESS;
        BufferAddress ba = new BufferAddress (field.getFirstLocation ());
        ptr = ba.packAddress (buffer, ptr);
      }
      else if (!sp.isNull ())
        ptr = packDataPosition (sp, buffer, ptr);       // suppress nulls

    return ptr;
  }
}