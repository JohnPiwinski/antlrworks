package org.antlr.works.components;

import edu.usfca.xj.appkit.document.XJDocument;
import edu.usfca.xj.appkit.frame.XJFrame;
import edu.usfca.xj.appkit.menu.XJMainMenuBar;
import edu.usfca.xj.appkit.menu.XJMenu;
import edu.usfca.xj.appkit.menu.XJMenuItem;
import edu.usfca.xj.appkit.undo.XJUndo;
import edu.usfca.xj.foundation.notification.XJNotificationCenter;
import edu.usfca.xj.foundation.notification.XJNotificationObserver;
import org.antlr.works.debugger.Debugger;
import org.antlr.works.prefs.AWPrefsDialog;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/*

[The "BSD licence"]
Copyright (c) 2005 Jean Bovet
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

public abstract class ComponentEditor implements XJNotificationObserver {

    protected ComponentContainer container;
    protected JPanel mainPanel;
    protected Box statusBar;

    public ComponentEditor(ComponentContainer container) {
        this.container = container;

        mainPanel = new JPanel(new BorderLayout());

        statusBar = new ComponentStatusBar();
        statusBar.setPreferredSize(new Dimension(0, 30));

        XJNotificationCenter.defaultCenter().addObserver(this, AWPrefsDialog.NOTIF_PREFS_APPLIED);
        XJNotificationCenter.defaultCenter().addObserver(this, Debugger.NOTIF_DEBUG_STARTED);
        XJNotificationCenter.defaultCenter().addObserver(this, Debugger.NOTIF_DEBUG_STOPPED);
    }

    public void refreshMainMenuBar() {
        if(getXJFrame().getMainMenuBar() != null)
            getXJFrame().getMainMenuBar().refreshState();
    }

    public JComponent getToolbar() {
        return null;
    }

    public JComponent getStatusBar() {
        return statusBar;
    }

    public XJDocument getDocument() {
        return container.getDocument();
    }

    public XJFrame getXJFrame() {
        return container.getXJFrame();
    }

    public JFrame getJFrame() {
        return getXJFrame().getJFrame();
    }

    public Container getJavaContainer() {
        return getXJFrame().getJavaContainer();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public XJUndo getCurrentUndo() {
        return getXJFrame().getCurrentUndo();
    }

    public void notificationFire(Object source, String name) {
        if(name.equals(AWPrefsDialog.NOTIF_PREFS_APPLIED)) {
            notificationPrefsChanged();
        } else if(name.equals(Debugger.NOTIF_DEBUG_STARTED)) {
            notificationDebuggerStarted();
        } else if(name.equals(Debugger.NOTIF_DEBUG_STOPPED)) {
            notificationDebuggerStopped();
        }
    }

    protected static JComponent createSeparator() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        Dimension d = s.getMaximumSize();
        d.width = 2;
        s.setMaximumSize(d);
        return s;
    }
                 
    /** For subclass only
     *
     */

    public abstract void create();

    public abstract void loadText(String text);

    public abstract String getText();

    public void close() {
        XJNotificationCenter.defaultCenter().removeObserver(this);
    }

    public abstract void componentDocumentContentChanged();

    public boolean componentDocumentWillSave() {
        return true;
    }

    public void setPersistentData(Map data) {

    }

    public Map getPersistentData() {
        return null;
    }

    public void componentDidAwake() {
    }

    public void customizeFileMenu(XJMenu menu) {
    }

    public void customizeEditMenu(XJMenu menu) {
    }

    public void customizeWindowMenu(XJMenu menu) {
    }

    public void customizeHelpMenu(XJMenu menu) {
    }

    public void customizeMenuBar(XJMainMenuBar menubar) {
    }

    public void menuItemState(XJMenuItem item) {
    }

    public void componentActivated() {
    }

    public void componentIsSelected() {
    }

    public void notificationPrefsChanged() {
    }

    public void notificationDebuggerStarted() {
    }

    public void notificationDebuggerStopped() {
    }

}