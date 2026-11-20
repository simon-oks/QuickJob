package ro.oks.quickjob.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;

class ApplicationTest {

    @Test
    void application_starts_as_pending() {
        Application app = createApplication();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void accept_pending_application() {
        Application app = createApplication();
        app.accept();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void reject_pending_application() {
        Application app = createApplication();
        app.reject();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void cancel_pending_application() {
        Application app = createApplication();
        app.cancel();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
    }

    @Test
    void cannot_accept_non_pending_application() {
        Application app = createApplication();
        app.accept();
        assertThatThrownBy(app::accept)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(ErrorCode.APPLICATION_INVALID_TRANSITION);
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void cannot_reject_accepted_application() {
        Application app = createApplication();
        app.accept();
        assertThatThrownBy(app::reject)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(ErrorCode.APPLICATION_INVALID_TRANSITION);
                });
    }

    @Test
    void cannot_cancel_rejected_application() {
        Application app = createApplication();
        app.reject();
        assertThatThrownBy(app::cancel)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(ErrorCode.APPLICATION_INVALID_TRANSITION);
                });
    }

    private Application createApplication() {
        return new Application(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Je suis disponible");
    }
}
