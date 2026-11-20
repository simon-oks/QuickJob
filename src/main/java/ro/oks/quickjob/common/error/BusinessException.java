package ro.oks.quickjob.common.error;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Object[] args;

    public BusinessException(String code, String messageKey, HttpStatus status, Object... args) {
        super(messageKey);
        this.code = code;
        this.status = status;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object[] getArgs() {
        return args;
    }
}
