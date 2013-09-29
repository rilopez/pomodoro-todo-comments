package com.ril;


import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiTodoSearchHelper;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

public class PomodoroCommentVSFListener implements ProjectComponent {

    private final Project myProject;

    public PomodoroCommentVSFListener (Project project) {
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
            System.out.println("contentsChanged " + event.getFileName());
            updateTodoCount(myProject);
        /*
            PsiTodoSearchHelper todoHelper = PsiTodoSearchHelper.SERVICE.getInstance(myProject);
            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(event.getFile());

            PsiElementFactory psiFactory = JavaPsiFacade.getInstance(myProject).getElementFactory();

            TodoItem[] todoItems = todoHelper.findTodoItems(psiFile);
            int i = 0;
            for (TodoItem todoItem : todoItems) {
                System.out.println(todoItem.getTextRange());
                PsiElement psiTodoComment = psiFile.findElementAt(todoItem.getTextRange().getStartOffset());
                PsiComment modifiedComment = psiFactory.createCommentFromText(psiTodoComment.getText() + " - " + i, null);
                psiTodoComment.replace(modifiedComment);
                i++;
            }
*/

        }

        public void fileCreated(VirtualFileEvent event) {
            updateTodoCount(myProject);
        }

        private void updateTodoCount(Project myProject) {

            int count = 0;
            PsiTodoSearchHelper todoHelper = PsiTodoSearchHelper.SERVICE.getInstance(myProject);

            PsiFile[] filesWithTodoItems = todoHelper.findFilesWithTodoItems();
            for (PsiFile filesWithTodoItem : filesWithTodoItems) {
                count += todoHelper.getTodoItemsCount(filesWithTodoItem);
            }

            int todoCount = count;
            showMessage("TODO count:" + todoCount, myProject);

        }

        public void fileDeleted(VirtualFileEvent event) {
            updateTodoCount(myProject);
        }
    }
}

