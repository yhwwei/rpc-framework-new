package rpc.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yhw
 * @version 1.0
 **/
public class ReflectionUtil {

    public static<T> T newInstance(Class<T> clazz){
        try {
           return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public static Method[] getPublicMethods(Class clazz){
        /*
        * getDeclaredMethods是获取类自己声明的  不会包括父类继承下来的
        * 但是getMethods会获取 包括从父类继承的 所有public 权限的方法
        * 所以要进行筛选
        * */
        List<Method> publicMethods = new ArrayList<>();
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            if(Modifier.isPublic(declaredMethod.getModifiers())){
                publicMethods.add(declaredMethod);
            }
        }
        return publicMethods.toArray(new Method[0]);
    }

    public static Object invoke(Object obj,Method method,Object...parameters){
        try {
            return method.invoke(obj,parameters);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
