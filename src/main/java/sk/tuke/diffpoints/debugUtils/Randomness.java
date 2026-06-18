package sk.tuke.diffpoints.debugUtils;

import java.util.Random;

public class Randomness {

//    public static void main(String[] args) {
//        // Example usage
//        for (int i = 0; i < 10; i++) {
//            int x =  randomInRange(1, 10);
//            System.out.println("One in 5: " + x);
//            System.out.println("Random in range 1-10: " + randomInRange(1, 10));
//        }
//    }
//

    private static final Random random = new Random();

    public static boolean oneInX(int x) {
        if (x <= 0) throw new IllegalArgumentException("x must be positive");
        return random.nextInt(x) == 0;
    }


    public static int randomInRange(int min, int max) {
        return random.nextInt(max - min) + min;
    }

}
