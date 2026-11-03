package fu.de190100.reservationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.de190100.reservationservice.entity.ReservationStatus;

import java.math.BigDecimal;
import java.util.Date;

public class ReservationDetailDTO {

    private Long reservationId;
    private String guestName;
    private String guestEmail;
    private String guestPhone;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date checkInDate;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date checkOutDate;

    private Integer numberOfGuests;
    private BigDecimal totalAmount;
    private ReservationStatus status;

    private RoomDTO room;

    public ReservationDetailDTO() {
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

    public RoomDTO getRoom() {
        return room;
    }

    public void setRoom(RoomDTO room) {
        this.room = room;
    }
}