package ro.oks.quickjob.common.error;

import java.net.URI;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ro.oks.quickjob.common.i18n.MessageService;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageService messageService;

    public GlobalExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        String detail = messageService.get(ex.getMessage(), ex.getArgs());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), detail);
        problem.setType(URI.create("https://quickjob.app/errors/" + ex.getCode()));
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://quickjob.app/errors/VALIDATION_FAILED"));
        problem.setProperty("code", ErrorCode.VALIDATION_FAILED);
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = messageService.get("error.conflict");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setType(URI.create("https://quickjob.app/errors/CONFLICT"));
        problem.setProperty("code", ErrorCode.CONFLICT);
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        String detail = messageService.get("error.concurrent_modification");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setType(URI.create("https://quickjob.app/errors/CONCURRENT_MODIFICATION"));
        problem.setProperty("code", ErrorCode.CONCURRENT_MODIFICATION);
        return problem;
    }
}
