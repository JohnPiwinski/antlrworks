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

package org.antlr.works.editor.helper;

import edu.usfca.xj.appkit.swing.XJTable;
import org.antlr.works.editor.EditorTab;
import org.antlr.works.editor.EditorWindow;
import org.antlr.works.interfaces.Console;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.io.StringWriter;
import java.io.PrintWriter;

public class EditorConsole implements EditorTab, Console {

    protected EditorWindow editor;

    protected JScrollPane groupScrollPane;
    protected XJTable groupTable;
    protected AbstractTableModel groupTableModel;

    protected JPanel panel;
    protected JTextArea textArea;
    protected JSplitPane splitPane;

    protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    protected Action currentAction = null;
    protected List actions = new ArrayList();

    protected static EditorConsole current = null;

    public static synchronized void setCurrent(EditorConsole console) {
        current = console;
    }

    public static synchronized EditorConsole getCurrent() {
        return current;
    }

    public EditorConsole(EditorWindow editor) {
        this.editor = editor;

        panel = new JPanel(new BorderLayout());

        splitPane = new JSplitPane();
        splitPane.setBorder(null);
        splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createGroupTable());
        splitPane.setRightComponent(createTextArea());
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        Box box = Box.createHorizontalBox();

        JButton clear = new JButton("Clear All");
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });
        box.add(clear);
        box.add(Box.createHorizontalGlue());

        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(box, BorderLayout.SOUTH);
    }

    public void makeCurrent() {
        EditorConsole.setCurrent(this);
    }

    public Container getContainer() {
        return panel;
    }

    public Container createGroupTable() {
        groupTable = new XJTable();
        groupTable.setBorder(null);
        groupTable.setPreferredScrollableViewportSize(new Dimension(200, 0));
        groupTable.setShowGrid(false);
        groupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        groupTable.setModel(groupTableModel = new AbstractTableModel() {
            public int getColumnCount() {
                return 1;
            }

            public int getRowCount() {
                return actions.size();
            }

            public boolean isCellEditable(int row, int col) {
                return false;
            }

            public String getColumnName(int column) {
                return "Actions";
            }

            public Object getValueAt(int row, int col) {
                return getActionNameAtIndex(row);
            }

            public void setValueAt(Object value, int row, int col) {
            }
        });

        groupTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if(e.getValueIsAdjusting())
                    return;

                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                if(!lsm.isSelectionEmpty()) {
                    updateTextArea();
                }
            }
        });

        groupScrollPane = new JScrollPane(groupTable);
        groupScrollPane.setBorder(null);
        groupScrollPane.setPreferredSize(new Dimension(200, 0));
        groupScrollPane.setWheelScrollingEnabled(true);

        return groupScrollPane;
    }

    public Container createTextArea() {
        textArea = new JTextArea();
        JScrollPane textAreaScrollPane = new JScrollPane(textArea);
        textAreaScrollPane.setWheelScrollingEnabled(true);
        return textAreaScrollPane;
    }

    public void updateTextArea() {
        textArea.setText(getActionTextAtIndex(groupTable.getSelectionModel().getMinSelectionIndex()));
        textArea.setCaretPosition(0);
    }

    public String getActionNameAtIndex(int index) {
        Action a = (Action)actions.get(index);
        return a.name;
    }

    public String getActionTextAtIndex(int index) {
        Action a = (Action)actions.get(Math.min(Math.max(0, index), actions.size()-1));
        return a.text.toString();
    }

    public void clear() {
        actions.clear();
        textArea.setText("");
        groupTableModel.fireTableDataChanged();
    }

    public void openGroup(String name) {
        if(currentAction != null)
            closeGroup();

        newAction(name);

        if(groupTable.getSelectionModel().getMinSelectionIndex() == -1)
            groupTable.getSelectionModel().setSelectionInterval(0, 0);
    }

    public void closeGroup() {
        if(currentAction != null) {
            currentAction = null;
        }
    }

    public synchronized void println(String s) {
        String t = "["+dateFormat.format(new Date())+"] "+s;
        if(currentAction == null) {
            newAction("Idle");
        }
        currentAction.appendString(t+"\n");
        System.out.println(s);
    }

    public synchronized void print(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        println(sw.toString());
    }

    protected synchronized void newAction(String name) {
        currentAction = new Action(name);
        actions.add(0, currentAction);
        groupTableModel.fireTableDataChanged();
    }

    public String getTabName() {
        return "Console";
    }

    public Component getTabComponent() {
        return getContainer();
    }

    public class Action {

        public String name = null;
        public StringBuffer text = new StringBuffer();

        public Action(String name) {
            this.name = name;
        }

        public synchronized void appendString(String t) {
            text.append(t);
            updateTextArea();
        }
    }
}