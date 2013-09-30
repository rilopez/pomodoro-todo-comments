package com.ril;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PomodoroCommentVSFListenerTest {
    @Test
    public void matchActivePattern() throws Exception {
       assertTrue(PomodoroCommentVSFListener.matchActivePattern("//TODO *P0 active pattern"));
       assertFalse(PomodoroCommentVSFListener.matchActivePattern("//TODO P0 active pattern"));
    }

    @Test
    public void testIsSameText() throws Exception {

        assertTrue(PomodoroCommentVSFListener.isSameTodo("//TODO *P0 active pattern","//TODO P1 active pattern"));
        assertTrue(PomodoroCommentVSFListener.isSameTodo("//TODO P1 active pattern","//TODO P1 active pattern"));
        assertFalse(PomodoroCommentVSFListener.isSameTodo("//TODO *P2 active pattern","//TODO *P2 different pattern"));



    }
}
