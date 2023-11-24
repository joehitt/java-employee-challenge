package com.example.rqchallenge;

import com.example.rqchallenge.employees.Employee;
import com.example.rqchallenge.employees.EmployeeController;
import com.example.rqchallenge.employees.IEmployeeController;
import com.example.rqchallenge.employees.IEmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
class RqChallengeApplicationTests {

    private final Employee inputTestEmployee = new Employee(Integer.MAX_VALUE, "Dr. Heath Botello", 88500, 42, "");
    private final List<Employee> testData = new ArrayList<>();
    @MockBean
    private IEmployeeService employeeService;
    private IEmployeeController employeeController;

    private Mono<Employee> getOutputTestEmployee(Map<String, Object> input) {
        return Mono.just(new Employee(9999,
                                      (String) input.get("name"),
                                      (Integer) input.get("salary"),
                                      (Integer) input.get("age"),
                                      "9999.png"));
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
    @SuppressWarnings("unchecked")
    public void init() {
        generateTestData();
        // mock service
        when(employeeService.getAllEmployees()).thenReturn(Flux.fromIterable(testData));
        when(employeeService.getEmployeeById(500)).thenReturn(Mono.just(Optional.of(testData.get(5))));
        when(employeeService.createEmployee(any())).thenAnswer(x -> {
            Map<String, Object> submitted = (Map<String, Object>) x.getArguments()[0];
            return getOutputTestEmployee(submitted);
        });
        when(employeeService.deleteEmployeeById(anyInt())).thenAnswer(x -> {
            int submittedId = (int) x.getArguments()[0];
            String deletedName = testData.stream()
                                         .filter(e -> e.getId() == submittedId)
                                         .findFirst()
                                         .orElseThrow()
                                         .getName();
            return Mono.just(deletedName);
        });
        employeeController = new EmployeeController(employeeService);
    }

    @Test
    void testGetAllEmployees() {
        List<Employee> employeeList = employeeController.getAllEmployees()
                                                        .collectList()
                                                        .block();
        assertNotNull(employeeList);
        assertEquals(employeeList.size(), testData.size(), "Total size of results was incorrect.");
        IntStream.range(0, employeeList.size())
                 .forEach(index -> {
                     assertEmployeeMatches(testData.get(index), employeeList.get(index));
                 });
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
    void testGetEmployeeById() {
        Mono<Employee> employeeMono = employeeController.getEmployeeById(testData.get(5)
                                                                                 .getId() + "");
        Employee employee = employeeMono.block();
        assertEmployeeMatches(testData.get(5), employee);
    }

    @Test
    void testGetEmployeesByNameSearch() {
        Flux<Employee> employeeFlux = employeeController.getEmployeesByNameSearch("Hayden");
        String message = "getEmployeesByNameSearch should return %s";
        List<Employee> employees = employeeFlux.collectList()
                                               .block();
        assertNotNull(employees, format(message, "a non-null list."));
        assertEquals(2, employees.size(), format(message, "the correct number of results."));
        assertEmployeeMatches(testData.get(2), employees.get(0));
        assertEmployeeMatches(testData.get(13), employees.get(1));
    }


    @Test
    void testGetHighestSalaryOfEmployees() {
        Mono<Integer> salaryMono = employeeController.getHighestSalaryOfEmployees();
        Integer salary = salaryMono.block();
        assertNotNull(salary);
        assertEquals(290000, salary);
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames() {

        Flux<String> employeeNamesFlux = employeeController.getTopTenHighestEarningEmployeeNames();
        String message = "getTopTenHighestEarningEmployeeNames should return %s";
        List<String> topTen = employeeNamesFlux.collectList()
                                               .block();
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

        Mono<Employee> createdMono = employeeController.createEmployee(map);

        Employee created = createdMono.block();
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
        Mono<String> deletedMono = employeeController.deleteEmployeeById(toDelete.getId() + "");
        String deletedName = deletedMono.block();
        assertEquals(toDelete.getName(), deletedName);
    }

}
