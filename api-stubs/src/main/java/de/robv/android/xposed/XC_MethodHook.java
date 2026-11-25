package de.robv.android.xposed;
import java.lang.reflect.Member;
public abstract class XC_MethodHook {
 protected void beforeHookedMethod(MethodHookParam p) throws Throwable {}
 protected void afterHookedMethod(MethodHookParam p) throws Throwable {}
 public static class MethodHookParam { public Member method; public Object thisObject; public Object[] args; public void setResult(Object o){throw new AssertionError();} public Object getResult(){throw new AssertionError();} }
 public class Unhook {}
}