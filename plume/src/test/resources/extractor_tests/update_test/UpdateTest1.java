package extractor_tests.update_test;

public class UpdateTest1 {

    private int i = 3;

    public static void main(String[] args) {
        int a = 3;
        int b = 2;
        int c = a + b;
        int d = UpdateTest2.f();
    }

    public static int foo(int x) {
        return x + 1;
    }

}
