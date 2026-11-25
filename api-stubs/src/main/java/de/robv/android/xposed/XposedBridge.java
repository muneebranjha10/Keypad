package de.robv.android.xposed;
import java.lang.reflect.Member;
import java.util.Set;
public final class XposedBridge {
 public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> c, XC_MethodHook h){throw new AssertionError();}
 public static XC_MethodHook.Unhook hookMethod(Member m, XC_MethodHook h){throw new AssertionError();}
 public static Object invokeOriginalMethod(Member m,Object o,Object[] a) throws Throwable {throw new AssertionError();}
 public static void log(String s){throw new AssertionError();}
}