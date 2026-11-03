package fu.de190100.reservationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.de190100.reservationservice.entity.ReservationStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Date;

public class ReservationDTO {

    private Long reservationId;

    @NotBlank(message = "guestName is required")
    @Size(max = 100, message = "guestName must not exceed 100 characters")
    private String guestName;

    @NotBlank(message = "guestEmail is required")
    @Email(message = "guestEmail is invalid")
    @Size(max = 100, message = "guestEmail must not exceed 100 characters")
    private String guestEmail;

    @NotBlank(message = "guestPhone is required")
    @Size(max = 20, message = "guestPhone must not exceed 20 characters")
    private String guestPhone;

    @NotNull(message = "checkInDate is required")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date checkInDate;

    @NotNull(message = "checkOutDate is required")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date checkOutDate;

    @NotNull(message = "numberOfGuests is required")
    @Min(value = 1, message = "numberOfGuests must be >= 1")
    @Max(value = 10, message = "numberOfGuests must be <= 10")
    private Integer numberOfGuests;

    private BigDecimal totalAmount;

    private ReservationStatus status;

    @NotNull(message = "roomId is required")
    private Long roomId;

    public ReservationDTO() {
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public Date getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(Date checkInDate) {
        this.checkInDate = checkInDate;
    }

    public Date getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(Date checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(Integer numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}