package uber;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Reservation_table")
public class Reservation {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long rsvId;
    private Long taxiId;
    private String status; // VALUE = reqReserve, reserved, reqCancel, cancelled
    private Long payId; // 결제 ID : 결재 완료시 Update, 결제 취소하는 경우 사용

    @PostPersist
    public void onPostPersist(){

        ////////////////////////////////
        // RESERVATION에 INSERT 된 경우 
        ////////////////////////////////

        ////////////////////////////////////
        // 예약 요청(reqReserve) 들어온 경우
        ////////////////////////////////////

        // 해당 Taxi가 Available한 상태인지 체크
        boolean result = ReservationApplication.applicationContext.getBean(uber.external.TaxiService.class)
                        .chkAndReqReserve(this.getTaxiId());
        System.out.println("######## Check Result : " + result);

        if(result) { 

            // 예약 가능한 상태인 경우(Available)

            //////////////////////////////
            // PAYMENT 결제 진행 (POST방식)
            //////////////////////////////
            uber.external.Payment payment = new uber.external.Payment();
            payment.setRsvId(this.getRsvId());
            payment.setTaxiId(this.getTaxiId());
            payment.setStatus("paid");
            ReservationApplication.applicationContext.getBean(uber.external.PaymentService.class)
                .approvePayment(payment);

            /////////////////////////////////////
            // 이벤트 발행 --> ReservationCreated
            /////////////////////////////////////
            ReservationCreated reservationCreated = new ReservationCreated();
            BeanUtils.copyProperties(this, reservationCreated);
            reservationCreated.publishAfterCommit();
        }
    }

    @PostUpdate
    public void onPostUpdate(){

        ////////////////////////////////
        // RESERVATION에 UPDATE 된 경우
        ////////////////////////////////
        if(this.getStatus().equals("reqCancel")) {

            ///////////////////////
            // 취소 요청 들어온 경우
            ///////////////////////

            // 이벤트 발생 --> reservationCancelRequested
            ReservationCancelRequested reservationCancelRequested = new ReservationCancelRequested();
            BeanUtils.copyProperties(this, reservationCancelRequested);
            reservationCancelRequested.publishAfterCommit();

        }

        

        if(this.getStatus().equals("reserved")) {

            ////////////////////
            // 예약 확정된 경우
            ////////////////////

            // 이벤트 발생 --> ReservationConfirmed
            ReservationConfirmed reservationConfirmed = new ReservationConfirmed();
            BeanUtils.copyProperties(this, reservationConfirmed);
            reservationConfirmed.publishAfterCommit();
        }

        if(this.getStatus().equals("cancelled")) {
            ////////////////////////
            // 예약 취소 확정된 경우
            ////////////////////////

            // 이벤트 발생 --> ReservationCancelled
            ReservationCancelled reservationCancelled = new ReservationCancelled();
            BeanUtils.copyProperties(this, reservationCancelled);
            reservationCancelled.publishAfterCommit();
        }

    }


    public Long getRsvId() {
        return rsvId;
    }

    public void setRsvId(Long rsvId) {
        this.rsvId = rsvId;
    }
    public Long getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(Long taxiId) {
        this.taxiId = taxiId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }

}
