import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Test {

    public static void main(String[] args) {
        //一位数组中随机插入20个字母，区分大小写，计算每个字母的重复数量
        int A = 65;
        int a = 97;
        Map<String, Integer> map = new HashMap<>();
        Random random = new Random();
        String currentStr = null;
        for(int i=0; i<20; i++) {
            if (random.nextInt(2) == 1) {
                 currentStr = String.valueOf((char)(a + random.nextInt(26)));
            } else {
                currentStr = String.valueOf((char)(A + random.nextInt(26)));
            }
            if (map.get(currentStr) != null) {
                map.put(currentStr, map.get(currentStr) + 1);
            } else {
                map.put(currentStr, 1);
            }
        }
        System.out.println(map.toString());
    }

}
