package com.example.rqchallenge;

import com.example.rqchallenge.employees.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
class RqChallengeApplicationTests {

    private final Employee inputTestEmployee = new Employee(
            // MAX_VALUE here to check that the API is not setting the ID to the input value,
            // but rather server-assigning a new ID
            Integer.MAX_VALUE, "Dr. Heath Botello", 88500, 42, ""
            // (generally this would be a server-assigned filename)
    );
    private final List<Employee> testData = new ArrayList<>();
    @MockBean
    private IEmployeeService employeeService;
    private IEmployeeController employeeController;

    private Employee getOutputTestEmployee(Employee input) {
        return new Employee(9999, input.getName(), input.getSalary(), input.getAge(), "9999.png");
    }

    private void addTestEmployee(int id,
                                 String name,
                                 int salary,
                                 int age,
                                 String profileImage) {
        testData.add(new Employee(id, name, salary, age, profileImage));
    }

    private void generateTestData() {
        addTestEmployee(50, "Barry Hargrove", 76000, 50, "emp-50.png");
        addTestEmployee(100, "Matthei Clovis", 42000, 40, "emp-100.png");
        addTestEmployee(200, "Hayden Erkan Eriksen", 20000, 32, "");
        addTestEmployee(300, "Sherry Porcia Pál", 76000, 45, "");
        addTestEmployee(400, "Rina Myrddin Brioschi", 290000, 22, "");
        addTestEmployee(500, "Alice Lengyel", 87345, 75, "employee_images/alice.jpg");
        addTestEmployee(600, "Athenais Quijote", 140000, 28, "");
        addTestEmployee(700, "Aparna Jones", 90900, 19, "");
        addTestEmployee(800, "Erasmo Gerarda Gilroy", 80000, 51, "");
        addTestEmployee(900, "Leudoberct Draper", 37000, 35, "");
        addTestEmployee(1000, "Reina Manfredonia", 15000, 20, "flowers.jpg");
        addTestEmployee(1100, "Florina Emiliano Breiner", 95000, 31, "");
        addTestEmployee(1200, "Christobel Lorainne", 192000, 20, "");
        addTestEmployee(1300, "Hayden Carol Padmore", 54200, 20, "");
        addTestEmployee(1400, "Aviram Lopes", 45600, 20, "");
        addTestEmployee(1500, "Christos Hedda Readdie", 54000, 20, "");
    }

    @BeforeEach
    public void init() throws ServiceException, IOException {
        generateTestData();
        // mock service
        when(employeeService.getAllEmployees()).thenReturn(testData);
        when(employeeService.getEmployeeById(500)).thenReturn(Optional.of(testData.get(5)));
        when(employeeService.createEmployee(any())).thenAnswer(x -> {
            Employee submitted = (Employee) x.getArguments()[0];
            return getOutputTestEmployee(submitted);
        });
        when(employeeService.deleteEmployeeById(anyInt())).thenAnswer(x -> {
            int submittedId = (int) x.getArguments()[0];
            return testData.stream()
                           .filter(e -> e.getId() == submittedId)
                           .findFirst()
                           .orElseThrow()
                           .getName();
        });
        employeeController = new EmployeeController(employeeService);
    }

    @Test
    void testGetAllEmployees() throws IOException {
        ResponseEntity<List<Employee>> response = employeeController.getAllEmployees();
        assertHttpOk(response);
        List<Employee> employeeList = response.getBody();
        assertNotNull(employeeList);
        assertEquals(employeeList.size(), testData.size(), "Total size of results was incorrect.");
        IntStream.range(0, employeeList.size())
                 .forEach(index -> {
                     assertEmployeeMatches(testData.get(index), employeeList.get(index));
                 });
    }

    private void assertHttpOk(ResponseEntity<?> response) {
        assertEquals(response.getStatusCodeValue(), HttpStatus.OK.value(), "Test should return OK HTTP status");
    }

    @Test
    void testGetEmployeeById() {
        ResponseEntity<Employee> response = employeeController.getEmployeeById(testData.get(5)
                                                                                       .getId() + "");
        assertHttpOk(response);
        assertEmployeeMatches(testData.get(5), response.getBody());
    }

    private void assertEmployeeMatches(Employee expected,
                                       Employee employee) {
        String message = "Employee object did not contain %s";
        assertNotNull(employee, format(message, "a non-null Employee object."));
        assertEquals(expected.getId(), employee.getId(), format(message, "the correct Employee ID"));
        assertEquals(expected.getName(), employee.getName(), format(message, "the correct Employee Name"));
        assertEquals(expected.getSalary(), employee.getSalary(), format(message, "the correct Salary"));
        assertEquals(expected.getAge(), employee.getAge(), format(message, "the correct Age"));
        assertEquals(expected.getProfileImage(),
                     employee.getProfileImage(),
                     format(message, "the correct Profile Image"));
    }


    @Test
    void testGetEmployeesByNameSearch() {
        ResponseEntity<List<Employee>> response = employeeController.getEmployeesByNameSearch("Hayden");
        assertHttpOk(response);
        String message = "getEmployeesByNameSearch should return %s";
        List<Employee> employees = response.getBody();
        assertNotNull(employees, format(message, "a non-null list."));
        assertEquals(2, employees.size(), format(message, "the correct number of results."));
        assertEmployeeMatches(testData.get(2), employees.get(0));
        assertEmployeeMatches(testData.get(13), employees.get(1));
    }

    @Test
    void testGetHighestSalaryOfEmployees() {
        ResponseEntity<Integer> response = employeeController.getHighestSalaryOfEmployees();
        assertHttpOk(response);
        Integer salary = response.getBody();
        assertNotNull(salary);
        assertEquals(290000, salary);
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames() {

        ResponseEntity<List<String>> response = employeeController.getTopTenHighestEarningEmployeeNames();
        assertHttpOk(response);
        String message = "getTopTenHighestEarningEmployeeNames should return %s";
        List<String> topTen = response.getBody();
        assertNotNull(topTen, format(message, "a non-null list."));
        assertEquals(10, topTen.size(), "the correct number of employees.");
        String listMessage = format(message, "the correct employee names in the correct order");

        // @formatter:off
        String[] expected = new String[]{
                "Rina Myrddin Brioschi",
                "Christobel Lorainne",
                "Athenais Quijote",
                "Florina Emiliano Breiner",
                "Aparna Jones",
                "Alice Lengyel",
                "Erasmo Gerarda Gilroy",
                "Barry Hargrove",
                "Sherry Porcia Pál",
                "Hayden Carol Padmore"
        };
        // @formatter:on

        int i = 0;
        for (String s : topTen) {
            assertEquals(expected[i], s, listMessage);
            i++;
        }

    }

    @Test
    void testCreateEmployee() {

        Employee input = inputTestEmployee;
        Map<String, Object> map = new HashMap<>();
        map.put("name", input.getName());
        map.put("salary", input.getSalary());
        map.put("age", input.getAge());
        map.put("profileImage", input.getProfileImage());

        ResponseEntity<Employee> response = employeeController.createEmployee(map);
        assertHttpOk(response);
        Employee created = response.getBody();
        String message = "Method createEmployee should return %s";
        assertNotNull(created, format(message, "a non-null Employee object"));
        assertTrue(created.getId() > 0, format(message, "an Employee object with a server-assigned ID > 0"));
        assertNotEquals(input.getId(),
                        created.getId(),
                        format(message, "a server-assigned ID and not just use the submitted input ID value."));
        String fieldMessage = "Method createEmployee returned a %s value that did not match the input";
        assertEquals(input.getName(), created.getName(), format(fieldMessage, "name"));
        assertEquals(input.getAge(), created.getAge(), format(fieldMessage, "age"));
        assertEquals(input.getSalary(), created.getSalary(), format(fieldMessage, "salary"));

    }

    @Test
    void testDeleteEmployeeById() {
        Employee toDelete = testData.get(14);
        ResponseEntity<String> response = employeeController.deleteEmployeeById(toDelete.getId() + "");
        assertHttpOk(response);
        String deletedName = response.getBody();
        assertEquals(toDelete.getName(), deletedName);
    }

}
