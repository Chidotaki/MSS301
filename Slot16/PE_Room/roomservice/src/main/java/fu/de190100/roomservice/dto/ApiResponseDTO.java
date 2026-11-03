package fu.de190100.roomservice.dto;

import java.time.Instant;

public class ApiResponseDTO {

    private int status;
    private Object data;
    private String timestamp;
    private String message;

    public ApiResponseDTO() {
    }

    public ApiResponseDTO(
            int status,
            String message,
            Object data
    ) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toString();
    }

    public static ApiResponseDTO of(
            int status,
            String message,
            Object data
    ) {
        return new ApiResponseDTO(
                status,
                message,
                data
        );
    }

    public static ApiResponseDTO ok(
            String message,
            Object data
    ) {
        return new ApiResponseDTO(
                1,
                message,
                data
        );
    }

    public static ApiResponseDTO error(
            int status,
            String message
    ) {
        return new ApiResponseDTO(
                status,
                message,
                null
        );
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}