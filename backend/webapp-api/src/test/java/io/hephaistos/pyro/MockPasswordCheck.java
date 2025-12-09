package io.hephaistos.pyro;

import io.hephaistos.pyro.service.BreachedPasswordService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MockPasswordCheck {

    @MockitoBean
    private BreachedPasswordService breachedPasswordService;

    protected void mockPasswordBreachCheckWithResponse(boolean response) {
        when(breachedPasswordService.isPasswordBreached(any())).thenReturn(response);
    }
}
