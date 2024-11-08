package tn.esprit.tpfoyer17.services;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.tpfoyer17.entities.Bloc;
import tn.esprit.tpfoyer17.entities.Chambre;
import tn.esprit.tpfoyer17.entities.Etudiant;
import tn.esprit.tpfoyer17.entities.Reservation;
import tn.esprit.tpfoyer17.entities.enumerations.TypeChambre;
import tn.esprit.tpfoyer17.repositories.BlocRepository;
import tn.esprit.tpfoyer17.repositories.ChambreRepository;
import tn.esprit.tpfoyer17.repositories.EtudiantRepository;
import tn.esprit.tpfoyer17.repositories.ReservationRepository;

import java.time.Year;
import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationService implements IReservationService {
    ReservationRepository reservationRepository;
    EtudiantRepository etudiantRepository;
    ChambreRepository chambreRepository;
    BlocRepository blocRepository;


    @Override
    public List<Reservation> getAllReservations() {
        return (List<Reservation>) reservationRepository.findAll();
    }

    @Override
    public Reservation getReservationById(String idReservation) {
        return reservationRepository.findById(idReservation).orElse(null);
    }

    @Override
    public void deleteReservation(String idReservation) {
        reservationRepository.deleteById(idReservation);

    }

    @Override
    public Reservation updateReservation(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

    @Override
    public Reservation ajouterReservation(long idBloc, long cinEtudiant) {

        if (reservationRepository.findForReservation(idBloc) != null) {
            Reservation reservation = reservationRepository.findForReservation(idBloc);
            reservation.getEtudiants().add(etudiantRepository.findByCinEtudiant(cinEtudiant));
            Chambre chambre = chambreRepository.findByReservationsIdReservation(reservation.getIdReservation());
            if (chambre.getTypeChambre() == TypeChambre.TRIPLE) {
                if (reservation.getEtudiants().size() == 3) {
                    reservation.setEstValide(false);
                }
            } else if (chambre.getTypeChambre() == TypeChambre.DOUBLE) {
                reservation.setEstValide(false);
            }
            return reservationRepository.save(reservation);
        } else {
            List<Etudiant> etudiants = new ArrayList<>();
            etudiants.add(etudiantRepository.findByCinEtudiant(cinEtudiant));
            Reservation reservation = Reservation.builder().anneeUniversitaire(new Date()).etudiants(new HashSet<>(etudiants)).build();
            Chambre chambre = chambreRepository.getForReservation(idBloc);
            Bloc bloc = blocRepository.findById(idBloc).orElseThrow();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            String idReservation = chambre.getIdChambre() + bloc.getNomBloc() + calendar.get(Calendar.YEAR);
            reservation.setIdReservation(idReservation);
            reservation.setEstValide(!chambre.getTypeChambre().equals(TypeChambre.SIMPLE));

            return reservationRepository.save(reservation);
        }


    }

    @Override
    public Reservation annulerReservation(long cinEtudiant) {
        Reservation reservation = reservationRepository.findByEtudiantsCinEtudiantAndAnneeUniversitaire(cinEtudiant, Year.now().getValue());
        Etudiant etudiant = etudiantRepository.findByCinEtudiant(cinEtudiant);
        reservation.getEtudiants().remove(etudiant);
        reservation.setEstValide(true);
        return reservationRepository.save(reservation);
    }

    @Override
    public List<Reservation> getReservationParAnneeUniversitaireEtNomUniversite(Date anneeUniversite, String nomUniversite) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(anneeUniversite);
        return reservationRepository.findByAnneeUniversitaire_YearAndNomUnuiversite(calendar.get(Calendar.YEAR), nomUniversite);

    }
}
