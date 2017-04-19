package com.ericsson.ema.tim.reflection;

//import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectionTest {
    private final static int LOOPNUM = 10000000;
    private static List<Tuple> list = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        testReflection();
//        testCgLib();
        testNonReflection();
        //testGetDeclaredMethod();
    }

    private static void init() {
        list.add(new Tuple(1, "eqinson", "male", 36));
        list.add(new Tuple(2, "eqinson2", "male", 36));
        list.add(new Tuple(3, "eqinson3", "male", 36));
        list.add(new Tuple(4, "eqinson4", "male", 36));
    }

    private static void testNonReflection() {
        long start = System.currentTimeMillis();
        init();
        for (int i = 0; i < LOOPNUM; i++)
            list.stream().filter(t -> t.getId() == 1).collect(Collectors.toList());
        //result.forEach(System.out::println);
        System.out.println(System.currentTimeMillis() - start);
    }

    private static void testReflection() throws NoSuchMethodException {
        long start = System.currentTimeMillis();
        init();
        Method getid = Tuple.class.getDeclaredMethod("getId");
        getid.setAccessible(true);

        for (int i = 0; i < LOOPNUM; i++) {
            list.stream().filter(t -> getTupleId(t, getid) == 1).collect(Collectors.toList());
        }
        //result.forEach(System.out::println);
        System.out.println(System.currentTimeMillis() - start);
    }

//    private static void testCgLib() throws NoSuchMethodException {
//        long start = System.currentTimeMillis();
//        init();
//        FastClass fastClass = FastClass.create(Tuple.class);
//        Method getMethod = Tuple.class.getMethod("getId");
//        getMethod.setAccessible(true);
//        FastMethod fastGetMethod = fastClass.getMethod(getMethod);
//        for (int i = 0; i < LOOPNUM; i++) {
//            list.stream().filter(t -> getTupleIdWithCgLib(t, fastGetMethod) == 1).collect(Collectors.toList
//                    ());
//        }
//        //result.forEach(System.out::println);
//        System.out.println(System.currentTimeMillis() - start);
//    }

    private static int getTupleId(Tuple tuple, Method getid) {
        int result = -1;
        try {
            result = (Integer) getid.invoke(tuple);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

//    private static int getTupleIdWithCgLib(Tuple tuple, FastMethod fastMethod) {
//        int result = -1;
//        try {
//            result = (Integer) fastMethod.invoke(tuple, new Object[]{});
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return -1;
//    }

    private static void testGetDeclaredMethod() throws NoSuchFieldException {
        long start = System.currentTimeMillis();
        for (int i = 0; i < LOOPNUM; i++)
            Tuple.class.getDeclaredField("name");
        System.out.println(System.currentTimeMillis() - start);
    }

}

class Tuple {
    private int id;
    private String name;
    private String gender;
    private int age;

    public Tuple(int id, String name, String gender, int age) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.age = age;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", age=" + age +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
