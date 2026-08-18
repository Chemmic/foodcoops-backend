package de.dhbw.foodcoop.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;

@SpringBootApplication
@EnableScheduling
public class FoodcoopWarehouseApplication {

    private final DeadlineService deadlineService;

    public FoodcoopWarehouseApplication(DeadlineService deadlineService) {
        this.deadlineService = deadlineService;
    }

    public static void main(String[] args) {
        SpringApplication.run(FoodcoopWarehouseApplication.class, args);
    }

    @Scheduled(fixedDelay = 1000 * 60 * 3, initialDelay = 1000 * 2)
    public void updateDeadlineIfNecessary() {
        deadlineService.updateDeadline();
    }
}