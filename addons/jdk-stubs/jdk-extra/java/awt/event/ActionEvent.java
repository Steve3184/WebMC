package java.awt.event;
import java.util.EventObject;
public class ActionEvent extends EventObject {
    public static final int SHIFT_MASK = 1;
    public static final int CTRL_MASK = 2;
    public static final int META_MASK = 4;
    public static final int ALT_MASK = 8;
    private final String actionCommand;
    private final int modifiers;
    public ActionEvent(Object source, int id, String command) { super(source); this.actionCommand = command; this.modifiers = 0; }
    public ActionEvent(Object source, int id, String command, int modifiers) { super(source); this.actionCommand = command; this.modifiers = modifiers; }
    public String getActionCommand() { return actionCommand; }
    public int getModifiers() { return modifiers; }
}
