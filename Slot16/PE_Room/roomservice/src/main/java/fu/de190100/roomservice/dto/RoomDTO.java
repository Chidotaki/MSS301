package fu.de190100.roomservice.dto;

import fu.de190100.roomservice.entity.RoomStatus;
import fu.de190100.roomservice.entity.RoomType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class RoomDTO {

    private Long roomId;

    @NotBlank(message = "roomNumber is required")
    @Size(max = 20, message = "roomNumber must not exceed 20 characters")
    private String roomNumber;

    @NotNull(message = "roomType is required")
    private RoomType roomType;

    @NotNull(message = "pricePerNight is required")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "pricePerNight must be > 0"
    )
    private BigDecimal pricePerNight;

    @NotNull(message = "capacity is required")
    @Min(value = 1, message = "capacity must be >= 1")
    @Max(value = 10, message = "capacity must be <= 10")
    private Integer capacity;

    @NotNull(message = "floor is required")
    @Min(value = 1, message = "floor must be >= 1")
    private Integer floor;

    private RoomStatus status;

    public RoomDTO() {
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}