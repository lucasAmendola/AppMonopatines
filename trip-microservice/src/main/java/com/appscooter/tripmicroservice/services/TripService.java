package com.appscooter.tripmicroservice.services;

import com.appscooter.tripmicroservice.domain.GeneralPrice;
import com.appscooter.tripmicroservice.domain.PauseTrip;
import com.appscooter.tripmicroservice.domain.Trip;
import com.appscooter.tripmicroservice.repositories.GeneralPriceRepository;
import com.appscooter.tripmicroservice.repositories.TariffRepository;
import com.appscooter.tripmicroservice.repositories.TripRepository;
import com.appscooter.tripmicroservice.repositories.interfaces.ScootersByTripsAndYearInterface;
import com.appscooter.tripmicroservice.services.dtos.generalprice.request.GeneralPriceRequestDTO;
import com.appscooter.tripmicroservice.services.dtos.generalprice.response.GeneralPriceResponseDTO;
import com.appscooter.tripmicroservice.services.dtos.tariff.response.ReportProfitsDTO;
import com.appscooter.tripmicroservice.services.dtos.trip.requests.FinishTripRequestDTO;
import com.appscooter.tripmicroservice.services.dtos.trip.requests.TripRequestDTO;
import com.appscooter.tripmicroservice.services.dtos.trip.requests.TripsAndYearRequestDTO;
import com.appscooter.tripmicroservice.services.dtos.trip.responses.ReportScootersDTO;
import com.appscooter.tripmicroservice.services.dtos.trip.responses.ScooterByTripsYearResponseDTO;
import com.appscooter.tripmicroservice.services.dtos.trip.responses.TripResponseDTO;
import com.appscooter.tripmicroservice.services.exception.ConflictExistException;
import com.appscooter.tripmicroservice.services.exception.NotFoundException;
import com.appscooter.tripmicroservice.services.exception.PauseActiveException;
import com.appscooter.tripmicroservice.services.timer.TimerPause;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("TripService")
public class TripService {

    private TripRepository tripRepository;
    private TariffRepository tariffRepository;
    private GeneralPriceRepository priceRepository;

    public TripService(TripRepository t, TariffRepository tariffRepository,
                       GeneralPriceRepository pr) {
        this.tripRepository=t;
        this.tariffRepository=tariffRepository;
        this.priceRepository=pr;
    }

    @Transactional(readOnly = true)
    public TripResponseDTO findTripById(long id) {
        return tripRepository.findById(id)
                .map(TripResponseDTO::new)
                .orElseThrow(() -> new NotFoundException("Trip", "Id", id));
    }

    @Transactional(readOnly = true)
    public List<TripResponseDTO> findAllTrip() {
        List<Trip> trips = tripRepository.findAll();
        return trips.stream().map(s1-> new TripResponseDTO(s1)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TripResponseDTO> findAllTripByScooter(String licenseScooter) {
            List<Trip> trips = this.tripRepository.findAllByLicenseScooter(licenseScooter);
            return trips.stream().map(t-> new TripResponseDTO(t))
                    .collect(Collectors.toList());
    }

    @Transactional
    public ResponseEntity saveTrip(TripRequestDTO request) {
        if(!this.tripRepository.existsById(request.getId())){

            //chequear que existe la scooter ingresada
            // solicitar a scooterusemicro (/api/scooters/{licensePlate})
            //obtener obj o usar request de scooter avisame benja
            //si me da null largar exception, si me trae entidad prosigue metodo

            Timestamp currentDate = Timestamp.valueOf(LocalDateTime.now());
            GeneralPrice currentPrices = this.priceRepository.findByCurrent(true);
            if(currentPrices != null) {
                GeneralPrice lastPrices = this.priceRepository.findByLastDateValidity(currentPrices.getDateValidity());
                if(lastPrices == null || currentDate.compareTo(lastPrices.getDateValidity()) < 0) {
                    this.tripRepository.save(new Trip(request, currentPrices.getPriceService()));
                    return new ResponseEntity(request.getId(), HttpStatus.CREATED);
                }
                else {
                    lastPrices.setCurrent(true);
                    currentPrices.setCurrent(false);
                    this.tripRepository.save(new Trip(request, lastPrices.getPriceService()));
                    return new ResponseEntity(request.getId(), HttpStatus.CREATED);
                }
            }
            throw new NotFoundException("GeneralPrices", "current", "true");
        }

        throw new ConflictExistException("Trip", "ID", request.getId());
    }

    @Transactional
    public ResponseEntity updateTrip(TripRequestDTO request, Long id) {
        Optional<Trip> tripExisting = this.tripRepository.findById(id);
        if(!tripExisting.isEmpty()){
            if(this.tripRepository.existsById(request.getId())) {
                if(request.getId() == id) {
                    tripExisting.get().setKms(request.getKms());
                    tripExisting.get().setInitTime(request.getInitTime());
                    tripExisting.get().setEndTime(request.getEndTime());
                    tripExisting.get().setEnded(request.getEnded());
                    return new ResponseEntity(id, HttpStatus.ACCEPTED);
                }
                else {
                    throw new ConflictExistException("Trip", "Id", request.getId());
                }
            }
            throw new NotFoundException("Trip", "Id", request.getId());
        }
        throw new NotFoundException("Trip", "Id", id);
    }

    @Transactional
    public ResponseEntity finishTrip(FinishTripRequestDTO request, Long id) {
        Optional<Trip> tripExisting = this.tripRepository.findById(id);
        if(!tripExisting.isEmpty()){
            String licensePlate = tripExisting.get().getLicenseScooter();


            //NO HAGAS ESTE
            //FALTA METODO EN SCOOTERUSEMICRO
            /*antes de finalizar el viaje debo chequear que mi scooter asociada
             * esta en una parada valida
             * solicitar a scooter-use-microservice (/api/scooters/{licensePlate}
             *  )*/

            tripExisting.get().setEndTime(Timestamp.valueOf(LocalDateTime.now()));
            tripExisting.get().setKms(request.getKms());
            tripExisting.get().setEnded(request.getEnded());
            return new ResponseEntity(id, HttpStatus.ACCEPTED);

        }
        throw new NotFoundException("Trip", "Id", id);
    }

    @Transactional
    public ResponseEntity deleteTrip(Long id) {
        if(this.tripRepository.existsById(id)){
            this.tripRepository.deleteById(id);
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
        else {
            throw new NotFoundException("Trip", "Id", id);
        }
    }

    @Transactional
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void initPause(Long id) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if(!trip.isEmpty()) {
            PauseTrip pauseTrip = trip.get().getPause();
            if(pauseTrip == null) {
                trip.get().setTimer(new TimerPause(trip.get().getId(), this.tripRepository, this.tariffRepository, this.priceRepository));
                trip.get().getTimer().initPause();
            }
            else {
                throw new ConflictExistException("PauseTrip", "Id", pauseTrip.getId());
            }
        }
        else {
            throw new NotFoundException("Trip", "Id", id);
        }
    }

    @Transactional
    public ResponseEntity endPause(Long id) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if(!trip.isEmpty()) {
            PauseTrip pauseTrip = trip.get().getPause();
            if(pauseTrip != null) {
                if(pauseTrip.getEndPause() == null) {
                    Time endPause = Time.valueOf(LocalTime.now());
                    Time initPause = pauseTrip.getInitPause();
                    Long diff = endPause.getTime() - initPause.getTime();
                    pauseTrip.setTimePause(diff/1000);
                    pauseTrip.setEndPause(endPause);
                    this.tripRepository.save(trip.get());
                    return new ResponseEntity(HttpStatus.NO_CONTENT);
                }
                else {
                    throw new PauseActiveException("PauseTrip", "id", pauseTrip.getId());
                }
            }
            else {
                throw new NotFoundException("PauseTrip", "Id", pauseTrip.getId());
            }
        }
        else {
            throw new NotFoundException("Trip", "Id", id);
        }

    }

    @Transactional(readOnly = true)
    public List<ReportProfitsDTO> findProfitsByMonthsInYear(Long year) {
        return this.tariffRepository.findProfitsByMonthsInYear(year)
                .stream().map(p->new ReportProfitsDTO(p.getXmonth(), p.getXyear(), p.getTotalProfits()))
                .collect(Collectors.toList());
    }

    public ResponseEntity saveNewPrices(GeneralPriceRequestDTO request) {
        if(this.priceRepository.findByDateValidity(request.getDateValidity()) == null) {
            this.priceRepository.save(new GeneralPrice(request));
            return new ResponseEntity("date validity: "+request.getDateValidity(), HttpStatus.CREATED);
        }

        throw new ConflictExistException("GeneralPrice", "dateValidity", request.getDateValidity());
    }

    @Transactional(readOnly = true)
    public List<GeneralPriceResponseDTO> findHistoryPrices() {
        List<GeneralPrice> prices = priceRepository.findAll();
        return prices.stream().map(s1-> new GeneralPriceResponseDTO(s1)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScooterByTripsYearResponseDTO> findAllScooterByTripsAndYear(TripsAndYearRequestDTO request) {
        List<ScootersByTripsAndYearInterface> scooters =
                this.tripRepository.findAllScooterByTripsAndYear(request.getMinCountTrips(), request.getYear());
        return scooters.stream()
                .map(s1-> new ScooterByTripsYearResponseDTO(s1.getLicenseScooter(), s1.getCountTrips(), s1.getYear())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportScootersDTO> findUseScootersByKms() {
        return this.tripRepository.findAllByKms()
                .stream()
                .map(r-> new ReportScootersDTO(r.getLicenseScooter(),r.getCountTrips(),r.getKms())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportScootersDTO> findUseScootersByTimeCcPauses() {
        return this.tripRepository.findAllByTimeCcPauses()
                .stream()
                .map(r-> new ReportScootersDTO(r.getLicenseScooter(),r.getCountTrips(),r.getKms())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportScootersDTO> findUseScootersByTimeOutPauses() {
        return this.tripRepository.findAllByTimeWithoutPauses()
                .stream()
                .map(r-> new ReportScootersDTO(r.getLicenseScooter(),r.getCountTrips(),r.getKms())).collect(Collectors.toList());
    }


}
