package io.hephaistos.flagforge;

import io.hephaistos.flagforge.service.BreachedPasswordService;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Import(PostgresTestContainerConfiguration.class)
public class MockPasswordCheck {

    @MockitoBean
    private BreachedPasswordService breachedPasswordService;

    protected void mockPasswordBreachCheckWithResponse(boolean response) {
        when(breachedPasswordService.isPasswordBreached(any())).thenReturn(response);
    }
}
