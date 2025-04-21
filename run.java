import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;


public class run {

    public static boolean checkCapacity(int maxCapacity, List<Map<String, String>> guests) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        PriorityQueue<LocalDate> checkOutDates = new PriorityQueue<>();
        guests.sort((lhs, rhs) -> {
            return LocalDate.parse(lhs.get("check-in"),dtf)
                .compareTo(LocalDate.parse(rhs.get("check-in"), dtf));
        });
        for (var guest : guests) {
            var checkIn = LocalDate.parse(guest.get("check-in"));
            var checkOut = LocalDate.parse(guest.get("check-out"));
            while(!checkOutDates.isEmpty() && checkIn.compareTo(checkOutDates.peek()) >= 0)
                checkOutDates.poll();
            if (checkOutDates.size() + 1 >= maxCapacity) return false;
            checkOutDates.add(checkOut);
        }
        return true; // или false
    }


    // Вспомогательный метод для парсинга JSON строки в Map
    private static Map<String, String> parseJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        // Удаляем фигурные скобки
        json = json.substring(1, json.length() - 1);


        // Разбиваем на пары ключ-значение
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            String key = keyValue[0].trim().replace("\"", "");
            String value = keyValue[1].trim().replace("\"", "");
            map.put(key, value);
        }

        return map;
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);


        // Первая строка - вместимость гостиницы
        int maxCapacity = Integer.parseInt(scanner.nextLine());


        // Вторая строка - количество записей о гостях
        int n = Integer.parseInt(scanner.nextLine());


        List<Map<String, String>> guests = new ArrayList<>();


        // Читаем n строк, json-данные о посещении
        for (int i = 0; i < n; i++) {
            String jsonGuest = scanner.nextLine();
            // Простой парсер JSON строки в Map
            Map<String, String> guest = parseJsonToMap(jsonGuest);
            guests.add(guest);
        }


        // Вызов функции
        boolean result = checkCapacity(maxCapacity, guests);


        // Вывод результата
        System.out.println(result ? "True" : "False");


        scanner.close();
    }
}