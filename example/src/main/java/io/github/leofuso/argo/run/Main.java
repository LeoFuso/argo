package io.github.leofuso.argo.run;

import io.github.leofuso.obs.demo.events.Department;
import io.github.leofuso.obs.demo.events.Details;

public class Main {

    public static void main(String[] args) {
        final Details details = Details.newBuilder()
            .setAnnotation("Some annotation value")
            .setDepartment(Department.ALLOWANCE)
            .build();
    }
}
