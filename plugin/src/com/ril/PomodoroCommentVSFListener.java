package com.ril;


import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.PsiTodoSearchHelper;
import com.intellij.psi.search.TodoItem;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PomodoroCommentVSFListener implements ProjectComponent {

    private static TodoItem activeTodoItem;
    private final Project myProject;

    public PomodoroCommentVSFListener(Project project) {
        myProject = project;
    }

    private static void showMessage(String message, Project myProject) {
        WindowManager windowManager = WindowManager.getInstance();
        StatusBar statusBar = windowManager.getStatusBar(myProject);


        JBPopupFactory instance = JBPopupFactory.getInstance();

        instance.createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
                .setCloseButtonEnabled(true)
                .setDialogMode(true)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                        Balloon.Position.atRight);
    }

    private static TodoItem findActivePomodoroTodoItem(TodoItem[] todoItems) {
        Pattern pattern = Pattern.compile("\\btodo \\*p[0-9]+\\b.*", Pattern.CASE_INSENSITIVE);
        TodoItem foundActivePomodoro = null;

        for (TodoItem todoItem : todoItems) {
            String todoText = getTodoItemText(todoItem);
            //looks for strings like this TODO *P0: first task
            Matcher todoMatcher = pattern.matcher(todoText);
            if (todoMatcher.find()) {
                foundActivePomodoro = todoItem;
                break;
            }
        }
        return foundActivePomodoro;
    }

    private static String getTodoItemText(TodoItem todoItem) {
        TextRange textRange = todoItem.getTextRange();
        PsiElement psiElement = todoItem.getFile().findElementAt(textRange.getStartOffset());
        return psiElement.getText();
    }

    private static void setActivePomodoro(TodoItem foundActivePomodoro, Project project) {
        activeTodoItem = foundActivePomodoro;
        showMessage(String.format("active pomodoro detected: <strong>%s</strong>", getTodoItemText(activeTodoItem)), project);
    }

    private static void findActivePomodoroInProject(Project myProject) {
        PsiTodoSearchHelper todoHelper = PsiTodoSearchHelper.SERVICE.getInstance(myProject);

        PsiFile[] filesWithTodoItems = todoHelper.findFilesWithTodoItems();

        TodoItem pomodoroTodoItem = null;
        for (PsiFile fileWithTodoItem : filesWithTodoItems) {
            pomodoroTodoItem = findActivePomodoroTodoItem(todoHelper.findTodoItems(fileWithTodoItem));
            if (pomodoroTodoItem != null) {
                break;
            }
        }
        if (pomodoroTodoItem != null) {
            setActivePomodoro(pomodoroTodoItem,myProject);
        }
    }

    public void projectOpened() {
        ProjectRootManager projectRootManager = ProjectRootManager.getInstance(myProject);
        VirtualFile[] sourceRoots = projectRootManager.getContentSourceRoots();


        // Add the Virtual File listener
        MyVfsListener vfsListener = new MyVfsListener(myProject);
        VirtualFileManager.getInstance().addVirtualFileListener(vfsListener, myProject);



    }

    public void projectClosed() {
    }

    public void initComponent() {
        // empty
    }

    public void disposeComponent() {
        // empty
    }

    @NotNull
    public String getComponentName() {
        return "com.ril.PomodoroCommentVSFListener";
    }

    // -------------------------------------------------------------------------
    // MyVfsListener
    // -------------------------------------------------------------------------

    private static class MyVfsListener extends VirtualFileAdapter {
        private Project myProject;

        public MyVfsListener(Project project) {
            this.myProject = project;
        }

        @Override
        public void contentsChanged(VirtualFileEvent event) {
            PsiTodoSearchHelper todoHelper = PsiTodoSearchHelper.SERVICE.getInstance(myProject);
            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(event.getFile());

            TodoItem foundActivePomodoro = findActivePomodoroTodoItem(todoHelper.findTodoItems(psiFile));
            if (foundActivePomodoro != null) {
                setActivePomodoro(foundActivePomodoro,myProject);
            }
        }

        public void fileCreated(VirtualFileEvent event) {
            findActivePomodoroInProject(myProject);
        }

        public void fileDeleted(VirtualFileEvent event) {
            findActivePomodoroInProject(myProject);
        }
    }
}

