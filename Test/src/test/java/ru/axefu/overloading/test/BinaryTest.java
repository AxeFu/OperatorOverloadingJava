package ru.axefu.overloading.test;

import org.junit.Test;
import ru.axefu.overloading.test.pack.OtherClass;
import ru.axefu.overloading.test.pack.Vector3;
import com.testlib.Num;

import static ru.axefu.overloading.test.StaticImport.*;

/**
 * Тесты для {@link ru.axefu.overloading.annotation.Operator}
 *
 * @author Artem Moshkin
 * @since 15.10.2024
 */
public class BinaryTest {

    private Vector3 a = new Vector3(12, 15, 34);
    private final Vector3 b = new Vector3(7, 7, 7);

    /**
     * Тест перегрузки бинарных операторов
     */
    @Test
    public final void binaryTest() {
        Vector3 a = new Vector3(1, 2, 3);
        Vector3 b = new Vector3(4, 5, 6);
        a = a + b;
        System.out.println(a + b);
        System.out.println(a - b);
        System.out.println(a * 5);
        System.out.println(a + a * b + a);
        System.out.println(a / 3);
    }

    /**
     * Тест перегрузки унарных операторов
     */
    @Test
    public void variableTest() {
        Vector3 a = new Vector3(1, 2, 3);
        System.out.println(a + b);
        System.out.println(b + a);
    }

    @Test
    public void selectTest() {
        Vector3 a = new Vector3(1, 2, 3);
        System.out.println(a + this.a);
        System.out.println(this.a + a);
        System.out.println(f() + this.a);
    }

    @Test
    public void staticTest() {
        System.out.println(OtherClass.a + this.a);
        System.out.println(OtherClass.f() + a);
    }

    @Test
    public void staticImportTest() {
        System.out.println(c + a);
    }

    @Test
    public void methodReturnValueTest() {
        System.out.println(f() + a);
    }

    @Test
    public void externalLibrary() {
        Num a = new Num(5);
        Num b = new Num(10);
        System.out.println(this.a + a + b);
        System.out.println(a + b);
    }

    @Test
    public void assignOpTest() {
        a += a + b;
        System.out.println(a += a + b);
        System.out.println(a -= b);
        System.out.println(a *= b);
        System.out.println(a /= 5);
        create();
    }

    private Vector3 f() {
        return new Vector3(1, 2, 3);
    }

    public void create() {
        Tes tes = new Tes();
        tes.hello();
    }

    public class Tes {

        public void hello() {
            System.out.println(a + b);
            System.out.println(a += b);
        }

    }

}
