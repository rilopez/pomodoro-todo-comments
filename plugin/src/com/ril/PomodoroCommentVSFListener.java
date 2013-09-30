package com.ril;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
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
    private static final int REST_LENGTH = 5;
    private static final int POMODORO_LENGTH = 30;
    private static int remainingSeconds = POMODORO_LENGTH;
    private static TodoItem activeTodoItem;
    private static Pattern activePattern = Pattern.compile("\\btodo \\*p[0-9]+(.+)", Pattern.CASE_INSENSITIVE);
    private static Pattern inactivePattern = Pattern.compile("\\btodo p[0-9]+(.+)", Pattern.CASE_INSENSITIVE);
    private final Project myProject;
    private static PomodoroStatus status = PomodoroStatus.IDLE;
    private TimerEventsSourceAppComponent.Listener listener;

    public PomodoroCommentVSFListener(Project project) {
        myProject = project;
    }

    private static String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d", min) + ":" + String.format("%02d", sec);
    }

    private static void showMessage(String message, Project myProject, MessageType messageType) {
        WindowManager windowManager = WindowManager.getInstance();
        StatusBar statusBar = windowManager.getStatusBar(myProject);

        JBPopupFactory instance = JBPopupFactory.getInstance();

        instance.createHtmlTextBalloonBuilder(message, messageType, null)
                .setCloseButtonEnabled(true)
                .setFadeoutTime(5000)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                        Balloon.Position.atRight);
    }

    static boolean matchActivePattern(String todoItemText) {
        Matcher todoMatcher = activePattern.matcher(todoItemText);
        return todoMatcher.find();
    }

    public void projectOpened() {
        TimerEventsSourceAppComponent timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSourceAppComponent.class);
        listener = new TimerEventsSourceAppComponent.Listener() {
            @Override
            public void onTimerEvent() {
                if (!status.equals(PomodoroStatus.IDLE)) {
                    remainingSeconds--;
                    if (remainingSeconds <= 0) {
                        switch (status) {
                            case RESTING:
                                remainingSeconds = POMODORO_LENGTH;
                                status = PomodoroStatus.IDLE;
                                break;
                            case RUNNING:
                                remainingSeconds = REST_LENGTH;
                                status = PomodoroStatus.RESTING;
                                break;
                        }
                    }
                    System.out.println(status + " " + formatTime(remainingSeconds));
                }
            }
        };
        timerEventsSource.addListener(listener);


        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
            @Override
            public void contentsChanged(VirtualFileEvent event) {
                PsiTodoSearchHelper todoHelper = PsiTodoSearchHelper.SERVICE.getInstance(myProject);
                PsiFile psiFile = PsiManager.getInstance(myProject).findFile(event.getFile());

                TodoItem[] todoItems = getTodoItems(todoHelper, psiFile);
                String activePomodoroText = extractTodoTextOnly(getTodoItemText(activeTodoItem));
                for (TodoItem todoItem : todoItems) {
                    String todoText = getTodoItemText(todoItem);
                    String pomodoroText = extractTodoTextOnly(todoText);
                    if (matchActivePattern(todoText)) {
                        if (!pomodoroText.equals(activePomodoroText)) {
                            setActivePomodoro(todoItem, myProject);
                        }
                        break;
                    } else {
                        if (matchInactivePattern(todoText) && pomodoroText.equals(activePomodoroText)) {
                            setInactivePomodoro(todoText, myProject);
                        }
                    }

                }
            }
        }, myProject);
    }

    static boolean matchInactivePattern(String todoItemText) {
        Matcher todoMatcher = inactivePattern.matcher(todoItemText);
        return todoMatcher.find();
    }

    private static String getTodoItemText(TodoItem todoItem) {
        if (todoItem == null) return null;
        TextRange textRange = todoItem.getTextRange();
        PsiElement psiElement = todoItem.getFile().findElementAt(textRange.getStartOffset());
        return psiElement.getText();
    }

    static boolean isSameTodo(String todoItemA, String todoItemB) {
        if (todoItemA.equals(todoItemB)) {
            return true;
        }

        String todoTextOnlyA = extractTodoTextOnly(todoItemA);
        String todoTextOnlyB = extractTodoTextOnly(todoItemB);

        return todoTextOnlyA.equals(todoTextOnlyB);
    }

    private static String extractTodoTextOnly(String todoItemA) {
        if (todoItemA == null) return null;
        Matcher matcherA;

        if (todoItemA.contains("*")) {
            matcherA = activePattern.matcher(todoItemA);
        } else {
            matcherA = inactivePattern.matcher(todoItemA);
        }
        if (matcherA.find()) {
            return matcherA.group(1);
        } else {
            return todoItemA;
        }
    }

    private static void setActivePomodoro(TodoItem foundActivePomodoro, Project project) {
        activeTodoItem = foundActivePomodoro;
        status = PomodoroStatus.RUNNING;
        showMessage(String.format("active pomodoro : <strong>%s</strong>", getTodoItemText(activeTodoItem)), project, MessageType.INFO);
    }

    private void setInactivePomodoro(String pomodoroText, Project myProject) {
        activeTodoItem = null;
        status = PomodoroStatus.IDLE;
        showMessage(String.format("stop pomodoro : <strong>%s</strong>. there are no active pomodoros", pomodoroText), myProject, MessageType.ERROR);
    }

    private TodoItem[] getTodoItems(PsiTodoSearchHelper todoHelper, PsiFile psiFile) {
        return todoHelper.findTodoItems(psiFile);
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

}

